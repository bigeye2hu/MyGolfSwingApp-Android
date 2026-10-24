package com.mygolfswing.app.data.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.video.V2VideoFileStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
enum class V2AsyncDownloadStatus(val displayName: String) {
    PENDING("等待中"),
    DOWNLOADING("下载中"),
    PAUSED("已暂停"),
    COMPLETED("已完成"),
    FAILED("下载失败"),
}

@Serializable
data class V2AsyncDownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val ownerUserId: String,
    val title: String,
    val originalUrl: String,
    val downloadUrl: String,
    val fileSizeBytes: Long = 0L,
    val durationSeconds: Int? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val status: V2AsyncDownloadStatus = V2AsyncDownloadStatus.PENDING,
    val progress: Double = 0.0,
    val localPath: String? = null,
    val errorMessage: String? = null,
) {
    val fileSizeText: String get() = "${fileSizeBytes.coerceAtLeast(0L) / (1024L * 1024L)}MB"
}

internal fun decodeAsyncDownloadTasks(raw: String, json: Json = asyncDownloadJson): List<V2AsyncDownloadTask> =
    runCatching {
        json.decodeFromString(ListSerializer(V2AsyncDownloadTask.serializer()), raw)
    }.getOrDefault(emptyList())

internal fun encodeAsyncDownloadTasks(
    tasks: List<V2AsyncDownloadTask>,
    json: Json = asyncDownloadJson,
): String = json.encodeToString(ListSerializer(V2AsyncDownloadTask.serializer()), tasks)

internal fun normalizeAsyncDownloadProgress(value: Double): Double =
    value.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0

internal fun restoreRunnableAsyncTasks(tasks: List<V2AsyncDownloadTask>): List<V2AsyncDownloadTask> =
    tasks.map { task ->
        if (task.status == V2AsyncDownloadStatus.DOWNLOADING) {
            task.copy(status = V2AsyncDownloadStatus.PENDING, errorMessage = null)
        } else {
            task.copy(progress = normalizeAsyncDownloadProgress(task.progress))
        }
    }

private val asyncDownloadJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class V2AsyncDownloadQueue(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val initialTasks = restoreRunnableAsyncTasks(
        decodeAsyncDownloadTasks(prefs.getString(KEY_TASKS, "[]").orEmpty()),
    )
    private val _tasks = MutableStateFlow(initialTasks.sortedByDescending(V2AsyncDownloadTask::createdAtEpochMs))
    val tasks: StateFlow<List<V2AsyncDownloadTask>> = _tasks.asStateFlow()

    init {
        if (initialTasks != decodeAsyncDownloadTasks(prefs.getString(KEY_TASKS, "[]").orEmpty())) {
            writeTasks(initialTasks)
        }
    }

    fun addTask(
        ownerUserId: String,
        title: String,
        originalUrl: String,
        downloadUrl: String,
        fileSizeBytes: Long,
        durationSeconds: Int?,
    ): V2AsyncDownloadTask {
        val task = V2AsyncDownloadTask(
            ownerUserId = ownerUserId,
            title = title.ifBlank { "下载视频" },
            originalUrl = originalUrl,
            downloadUrl = downloadUrl,
            fileSizeBytes = fileSizeBytes.coerceAtLeast(0L),
            durationSeconds = durationSeconds?.coerceAtLeast(0),
        )
        mutate { listOf(task) + it }
        if (runCatching { schedule(task.id, ExistingWorkPolicy.KEEP) }.isFailure) {
            markFailed(ownerUserId, task.id)
        }
        return task
    }

    fun recover(ownerUserId: String?) {
        val owner = ownerUserId?.takeIf(String::isNotBlank) ?: return
        tasks.value.filter {
            it.ownerUserId == owner && it.status == V2AsyncDownloadStatus.PENDING
        }.forEach {
            if (runCatching { schedule(it.id, ExistingWorkPolicy.KEEP) }.isFailure) {
                markFailed(it.ownerUserId, it.id)
            }
        }
    }

    fun pauseTask(ownerUserId: String, taskId: String) {
        val changed = updateOwned(ownerUserId, taskId) { task ->
            if (task.status == V2AsyncDownloadStatus.DOWNLOADING || task.status == V2AsyncDownloadStatus.PENDING) {
                task.copy(status = V2AsyncDownloadStatus.PAUSED, errorMessage = null)
            } else task
        }
        if (changed) runCatching { WorkManager.getInstance(appContext).cancelUniqueWork(workName(taskId)) }
    }

    fun resumeTask(ownerUserId: String, taskId: String) {
        val changed = updateOwned(ownerUserId, taskId) { task ->
            if (task.status == V2AsyncDownloadStatus.PAUSED || task.status == V2AsyncDownloadStatus.FAILED) {
                task.copy(status = V2AsyncDownloadStatus.PENDING, progress = 0.0, errorMessage = null)
            } else task
        }
        if (changed && runCatching { schedule(taskId, ExistingWorkPolicy.REPLACE) }.isFailure) {
            markFailed(ownerUserId, taskId)
        }
    }

    fun removeTask(ownerUserId: String, taskId: String) {
        runCatching { WorkManager.getInstance(appContext).cancelUniqueWork(workName(taskId)) }
        mutate { tasks -> tasks.filterNot { it.ownerUserId == ownerUserId && it.id == taskId } }
    }

    fun clearCompleted(ownerUserId: String) {
        mutate { tasks ->
            tasks.filterNot { it.ownerUserId == ownerUserId && it.status == V2AsyncDownloadStatus.COMPLETED }
        }
    }

    fun clearOwner(ownerUserId: String) {
        tasks.value.filter { it.ownerUserId == ownerUserId }.forEach {
            runCatching { WorkManager.getInstance(appContext).cancelUniqueWork(workName(it.id)) }
        }
        mutate { tasks -> tasks.filterNot { it.ownerUserId == ownerUserId } }
    }

    suspend fun runTask(taskId: String): Boolean {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return true
        val currentUserId = ServiceLocator.session.current().userId
        if (currentUserId != task.ownerUserId) return true
        if (task.status == V2AsyncDownloadStatus.PAUSED || task.status == V2AsyncDownloadStatus.COMPLETED) return true
        updateOwned(task.ownerUserId, task.id) {
            it.copy(status = V2AsyncDownloadStatus.DOWNLOADING, errorMessage = null)
        }
        return try {
            val imported = V2VideoFileStore.downloadRemoteVideo(appContext, task.downloadUrl) { progress ->
                if (progress >= 0.0) updateProgress(task.ownerUserId, task.id, progress)
            }
            if (imported == null) {
                markFailed(task.ownerUserId, task.id)
                true
            } else {
                updateOwned(task.ownerUserId, task.id) {
                    it.copy(
                        status = V2AsyncDownloadStatus.COMPLETED,
                        progress = 1.0,
                        fileSizeBytes = imported.sizeBytes,
                        localPath = imported.absolutePath,
                        errorMessage = null,
                    )
                }
                publishVideoToGallery(appContext, task.id, File(imported.absolutePath))
                notifyCompleted(task.title)
                true
            }
        } catch (cancelled: CancellationException) {
            updateOwned(task.ownerUserId, task.id) {
                if (it.status == V2AsyncDownloadStatus.DOWNLOADING) {
                    it.copy(status = V2AsyncDownloadStatus.PAUSED, errorMessage = null)
                } else it
            }
            throw cancelled
        } catch (_: Throwable) {
            markFailed(task.ownerUserId, task.id)
            true
        }
    }

    private fun markFailed(ownerUserId: String, taskId: String) {
        updateOwned(ownerUserId, taskId) {
            it.copy(status = V2AsyncDownloadStatus.FAILED, errorMessage = "下载失败，请重试")
        }
    }

    private fun updateProgress(ownerUserId: String, taskId: String, value: Double) {
        val normalized = normalizeAsyncDownloadProgress(value)
        updateOwned(ownerUserId, taskId) { task ->
            if (task.status != V2AsyncDownloadStatus.DOWNLOADING || normalized - task.progress < 0.01) task
            else task.copy(progress = normalized)
        }
    }

    private fun updateOwned(
        ownerUserId: String,
        taskId: String,
        transform: (V2AsyncDownloadTask) -> V2AsyncDownloadTask,
    ): Boolean {
        var changed = false
        mutate { tasks ->
            tasks.map { task ->
                if (task.ownerUserId == ownerUserId && task.id == taskId) {
                    transform(task).also { changed = changed || it != task }
                } else task
            }
        }
        return changed
    }

    private fun mutate(transform: (List<V2AsyncDownloadTask>) -> List<V2AsyncDownloadTask>) {
        synchronized(lock) {
            val updated = transform(_tasks.value).sortedByDescending(V2AsyncDownloadTask::createdAtEpochMs)
            if (updated == _tasks.value) return
            writeTasks(updated)
            _tasks.value = updated
        }
    }

    private fun writeTasks(tasks: List<V2AsyncDownloadTask>) {
        prefs.edit(commit = true) { putString(KEY_TASKS, encodeAsyncDownloadTasks(tasks)) }
    }

    private fun schedule(taskId: String, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<V2AsyncDownloadWorker>()
            .setInputData(workDataOf(V2AsyncDownloadWorker.KEY_TASK_ID to taskId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(workName(taskId), policy, request)
    }

    private fun notifyCompleted(title: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_COMPLETED, "视频下载完成", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_COMPLETED)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("视频下载完成 ✅")
            .setContentText(if (title.isBlank()) "视频已下载到本地" else "「$title」已下载完成")
            .setAutoCancel(true)
            .setLocalOnly(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID_BASE + (title.hashCode() and 0x0FFF), notification) }
    }

    companion object {
        private const val PREFS_NAME = "v2_async_download_tasks"
        private const val KEY_TASKS = "tasks"
        internal const val CHANNEL_PROGRESS = "async_download_progress"
        private const val CHANNEL_COMPLETED = "async_download_completed"
        internal const val NOTIFICATION_ID_BASE = 28_000
        internal fun workName(taskId: String): String = "async_download_$taskId"
    }
}

internal fun publishVideoToGallery(context: Context, taskId: String, source: File): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !source.exists() || source.length() <= 0L) return false
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "Huibei_${taskId.take(8)}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/挥呗")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
            ?: error("Unable to open gallery output")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        true
    } catch (_: Throwable) {
        resolver.delete(uri, null, null)
        false
    }
}
