package com.mygolfswing.app.data.coach

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mygolfswing.app.data.repo.V2CoachWorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class V2CoachQueuedAttachment(
    val videoPath: String? = null,
    val coverPath: String? = null,
    val existingAssetId: String? = null,
    val uploadId: String,
    val sourceKind: String = "album",
    val title: String = "关联教学视频",
    val durationSeconds: Double? = null,
    val saveToLibrary: Boolean = false,
)

const val V2_MAX_RELATED_MEDIA = 3

internal fun boundedCoachAttachments(
    attachments: List<V2CoachQueuedAttachment>,
): List<V2CoachQueuedAttachment> = attachments.take(V2_MAX_RELATED_MEDIA)

@Serializable
data class V2CoachDeliveryJob(
    val jobId: String,
    val ownerUserId: String,
    val connectionId: String,
    val peerName: String = "",
    val parentItemId: String? = null,
    val replyToItemId: String? = null,
    val replyAtSeconds: Double? = null,
    val videoPath: String? = null,
    val coverPath: String? = null,
    val uploadId: String,
    val requestId: String,
    val title: String,
    val text: String,
    val durationSeconds: Double? = null,
    val requiresCoachReview: Boolean = false,
    val contentMode: String = V2WorkspaceContentMode.PRACTICE.wireValue,
    val sourceKind: String = "video",
    val sourceRecordId: String? = null,
    val deliveryType: String = V2_DELIVERY_WORKSPACE,
    val tags: List<String> = emptyList(),
    val existingAssetId: String? = null,
    val saveToLibrary: Boolean = false,
    val attachments: List<V2CoachQueuedAttachment> = emptyList(),
    val cellularUploadApproved: Boolean = false,
    val pausedByUser: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

const val V2_DELIVERY_WORKSPACE = "workspace"
const val V2_DELIVERY_LIBRARY_IMPORT = "library_import"
const val V2_LARGE_CELLULAR_UPLOAD_BYTES = 100L * 1_024L * 1_024L

internal class V2CellularUploadConfirmationRequired(
    val jobId: String,
    val fileSizeBytes: Long,
) : V2CoachUserFacingException("大视频正在等待 Wi-Fi；如需使用手机流量，请先确认")

fun V2CoachDeliveryJob.requiresCellularUploadConfirmation(
    activeNetworkMetered: Boolean,
): Boolean = activeNetworkMetered &&
    !cellularUploadApproved &&
    localUploadBytes() >= V2_LARGE_CELLULAR_UPLOAD_BYTES

internal fun V2CoachDeliveryJob.localUploadBytes(): Long =
    (if (sourceKind !in setOf("text", "library_asset")) videoPath?.let(::File)?.length() ?: 0L else 0L) +
        boundedCoachAttachments(attachments).sumOf { attachment ->
            if (attachment.existingAssetId.isNullOrBlank()) {
                attachment.videoPath?.let(::File)?.length() ?: 0L
            } else {
                0L
            }
        }

data class V2CoachDeliveryResult(
    val workspaceItem: V2CoachWorkspaceItem? = null,
    val libraryAssetId: String? = null,
) {
    fun requireWorkspaceItem(): V2CoachWorkspaceItem = workspaceItem
        ?: throw V2CoachUserFacingException("发送结果不完整，请刷新确认")
}

data class V2CoachDeliveryCompletion(
    val id: String,
    val ownerUserId: String,
    val connectionId: String?,
    val itemId: String?,
    val peerName: String,
    val title: String,
    val detail: String,
    val opensLibrary: Boolean,
)

internal fun deliveryCompletion(
    job: V2CoachDeliveryJob,
    result: V2CoachDeliveryResult,
    completedAtMillis: Long = System.currentTimeMillis(),
): V2CoachDeliveryCompletion {
    val libraryImport = job.deliveryType == V2_DELIVERY_LIBRARY_IMPORT
    val recipient = if (job.requiresCoachReview) "教练" else "学员"
    val isReply = !job.parentItemId.isNullOrBlank()
    val isVideoPractice = job.requiresCoachReview && job.sourceKind !in setOf("text", "voice_recording")
    val title = when {
        libraryImport -> "素材已保存"
        isReply -> "回复已送达"
        isVideoPractice -> "练习视频已送达"
        else -> "内容已送达"
    }
    val detail = when {
        libraryImport -> "已加入私有素材库"
        isReply -> "${recipient}现在可以在原内容下查看"
        isVideoPractice -> "教练现在可以查看和回复"
        job.requiresCoachReview -> "教练现在可以查看"
        else -> "学员现在可以查看"
    }
    return V2CoachDeliveryCompletion(
        id = "completed-${job.jobId}-$completedAtMillis",
        ownerUserId = job.ownerUserId,
        connectionId = job.connectionId.takeUnless { libraryImport || it.isBlank() },
        itemId = if (isReply) job.parentItemId else result.workspaceItem?.itemId,
        peerName = job.peerName.trim(),
        title = title,
        detail = detail,
        opensLibrary = libraryImport,
    )
}

data class V2CoachDeliveryState(
    val pendingCount: Int = 0,
    val message: String? = null,
    val jobs: List<V2CoachDeliveryJob> = emptyList(),
    val activeJobId: String? = null,
    val activePhase: V2CoachDeliveryPhase? = null,
    val activeProgress: Double = 0.0,
    val completion: V2CoachDeliveryCompletion? = null,
)

enum class V2CoachDeliveryPhase(val displayText: String) {
    PREPARING("正在准备"),
    UPLOADING("正在上传"),
    PUBLISHING("正在发布"),
}

fun V2CoachDeliveryState.jobsForOwner(ownerUserId: String?): List<V2CoachDeliveryJob> =
    ownerUserId?.let { owner -> jobs.filter { it.ownerUserId == owner } }.orEmpty()

fun V2CoachDeliveryState.completionForOwner(ownerUserId: String?): V2CoachDeliveryCompletion? =
    completion?.takeIf { ownerUserId != null && it.ownerUserId == ownerUserId }

class V2CoachDeliveryQueue(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("v2_coach_delivery_queue", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val deliveryMutex = Mutex()
    private val activeDeliveryLock = Any()
    private var activeDeliveryJobId: String? = null
    private var activeDeliveryJob: Job? = null
    private val _state = MutableStateFlow(V2CoachDeliveryState())
    val state: StateFlow<V2CoachDeliveryState> = _state.asStateFlow()

    internal fun cellularConfirmationFor(job: V2CoachDeliveryJob): V2CellularUploadConfirmationRequired? {
        if (!job.requiresCellularUploadConfirmation(appContext.isActiveNetworkMetered())) return null
        return V2CellularUploadConfirmationRequired(job.jobId, job.localUploadBytes())
    }

    suspend fun enqueue(job: V2CoachDeliveryJob) = mutex.withLock {
        job.attachments.drop(V2_MAX_RELATED_MEDIA).forEach { attachment ->
            attachment.videoPath?.let(::File)?.delete()
            attachment.coverPath?.let(::File)?.delete()
        }
        val normalized = job.copy(attachments = boundedCoachAttachments(job.attachments))
        val jobs = readJobs().filterNot { it.jobId == normalized.jobId } + normalized
        writeJobs(jobs)
        _state.value = V2CoachDeliveryState(jobs.size, "内容发送已进入可靠队列", jobs)
        scheduleRecoverySafely(normalized.ownerUserId)
    }

    /** Returns a preflighted draft to its editor without deleting media now owned by that editor. */
    internal suspend fun withdrawPreservingFiles(jobId: String): Boolean = mutex.withLock {
        val jobs = readJobs()
        if (jobs.none { it.jobId == jobId }) return@withLock false
        val remaining = jobs.filterNot { it.jobId == jobId }
        writeJobs(remaining)
        _state.value = V2CoachDeliveryState(
            pendingCount = remaining.size,
            message = "视频已保留在发送确认页",
            jobs = remaining,
        )
        true
    }

    suspend fun deliver(
        jobId: String,
        repository: V2CoachWorkspaceRepository,
    ): Result<V2CoachDeliveryResult> = deliveryMutex.withLock {
        deliverWithActiveJob(jobId, repository)
    }

    private suspend fun deliverWithActiveJob(
        jobId: String,
        repository: V2CoachWorkspaceRepository,
    ): Result<V2CoachDeliveryResult> {
        val coroutineJob = currentCoroutineContext()[Job]
        synchronized(activeDeliveryLock) {
            activeDeliveryJobId = jobId
            activeDeliveryJob = coroutineJob
        }
        return try {
            deliverLocked(jobId, repository)
        } finally {
            synchronized(activeDeliveryLock) {
                if (activeDeliveryJobId == jobId && activeDeliveryJob === coroutineJob) {
                    activeDeliveryJobId = null
                    activeDeliveryJob = null
                }
            }
        }
    }

    suspend fun stop(jobId: String): Boolean {
        val coroutineJob = synchronized(activeDeliveryLock) {
            activeDeliveryJob?.takeIf { activeDeliveryJobId == jobId && it.isActive }
        } ?: return false
        mutex.withLock {
            val jobs = readJobs()
            if (jobs.none { it.jobId == jobId }) return false
            val stopped = jobs.map { job ->
                if (job.jobId == jobId) {
                    job.copy(pausedByUser = true, lastError = "发送已停止，内容仍保留，可以继续发送")
                } else {
                    job
                }
            }
            writeJobs(stopped)
            _state.value = V2CoachDeliveryState(
                pendingCount = stopped.size,
                message = "发送已停止，内容仍保留在队列中",
                jobs = stopped,
            )
        }
        coroutineJob.cancel(V2CoachDeliveryStopped(jobId))
        return true
    }

    suspend fun approveCellularUpload(jobId: String): Boolean = mutex.withLock {
        val jobs = readJobs()
        if (jobs.none { it.jobId == jobId }) return@withLock false
        val updated = jobs.map { job ->
            if (job.jobId == jobId) job.copy(cellularUploadApproved = true, lastError = null) else job
        }
        writeJobs(updated)
        _state.value = V2CoachDeliveryState(
            pendingCount = updated.size,
            message = "已允许使用手机流量发送",
            jobs = updated,
        )
        true
    }

    suspend fun recover(
        ownerUserId: String,
        repository: V2CoachWorkspaceRepository,
    ): Int = deliveryMutex.withLock {
        val candidates = mutex.withLock {
            val now = System.currentTimeMillis()
            val (expired, live) = readJobs().partition {
                now - it.createdAtMillis >= LIFETIME_MILLIS ||
                    !it.hasRecoverablePayload()
            }
            expired.forEach { it.deleteFiles() }
            writeJobs(live)
            live.filter {
                it.ownerUserId == ownerUserId &&
                    !it.pausedByUser &&
                    !it.isFailedQueueJob()
            }
        }
        if (candidates.isEmpty()) {
            val jobs = mutex.withLock { readJobs() }
            _state.value = V2CoachDeliveryState(
                pendingCount = jobs.size,
                jobs = jobs,
                completion = _state.value.completion,
            )
            return@withLock 0
        }
        val queuedJobs = mutex.withLock { readJobs() }
        _state.value = V2CoachDeliveryState(
            queuedJobs.size,
            "正在恢复 ${candidates.size} 条未完成发送",
            queuedJobs,
        )
        var delivered = 0
        for (job in candidates) {
            val result = try {
                deliverWithActiveJob(job.jobId, repository)
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            if (result.isSuccess) {
                delivered += 1
            }
        }
        val remainingJobs = mutex.withLock { readJobs() }
        val remaining = remainingJobs.size
        val latestCompletion = _state.value.completion
        _state.value = V2CoachDeliveryState(
            pendingCount = remaining,
            message = when {
                delivered > 0 && remaining == 0 -> "未完成的内容已恢复发送"
                delivered > 0 -> "已恢复 $delivered 条，仍有 $remaining 条待发送"
                remaining > 0 -> "仍有 $remaining 条内容等待网络恢复"
                else -> null
            },
            jobs = remainingJobs,
            completion = latestCompletion,
        )
        delivered
    }

    suspend fun pendingCount(ownerUserId: String): Int = mutex.withLock {
        readJobs().count { it.ownerUserId == ownerUserId }
    }

    suspend fun automaticPendingCount(ownerUserId: String): Int = mutex.withLock {
        readJobs().count { it.ownerUserId == ownerUserId && !it.pausedByUser }
    }

    suspend fun cancel(jobId: String): Boolean = deliveryMutex.withLock {
        if (_state.value.activeJobId == jobId) return@withLock false
        mutex.withLock {
            val jobs = readJobs()
            val job = jobs.firstOrNull { it.jobId == jobId } ?: return@withLock false
            job.deleteFiles()
            val remaining = jobs.filterNot { it.jobId == jobId }
            writeJobs(remaining)
            _state.value = V2CoachDeliveryState(
                pendingCount = remaining.size,
                message = "已从发送队列移除",
                jobs = remaining,
            )
            true
        }
    }

    fun dismissCompletion(id: String) {
        val current = _state.value
        if (current.completion?.id == id) {
            _state.value = current.copy(
                message = current.message.takeUnless {
                    current.pendingCount == 0 && it == "内容已送达"
                },
                completion = null,
            )
        }
    }

    /**
     * Removes one account's reliable-delivery payloads at the logout privacy boundary.
     * Jobs for another account remain isolated and untouched.
     */
    suspend fun clearOwner(ownerUserId: String) {
        val (activeId, activeJob) = synchronized(activeDeliveryLock) {
            activeDeliveryJobId to activeDeliveryJob
        }
        val ownsActiveJob = activeId != null && mutex.withLock {
            readJobs().any { it.jobId == activeId && it.ownerUserId == ownerUserId }
        }
        if (ownsActiveJob) activeJob?.cancel(CancellationException("Account logged out"))
        WorkManager.getInstance(appContext).cancelUniqueWork("coach_delivery_${ownerUserId.hashCode()}")
        mutex.withLock {
            val jobs = readJobs()
            jobs.filter { it.ownerUserId == ownerUserId }.forEach { it.deleteFiles() }
            val remaining = jobs.filterNot { it.ownerUserId == ownerUserId }
            writeJobs(remaining)
            _state.value = V2CoachDeliveryState(
                pendingCount = remaining.size,
                jobs = remaining,
            )
        }
    }

    suspend fun retryAll(
        ownerUserId: String,
        repository: V2CoachWorkspaceRepository,
    ): Int {
        val ids = mutex.withLock {
            readJobs()
                .filter { it.ownerUserId == ownerUserId && it.isFailedQueueJob() }
                .map(V2CoachDeliveryJob::jobId)
        }
        var delivered = 0
        for (jobId in ids) if (deliver(jobId, repository).isSuccess) delivered += 1
        return delivered
    }

    private fun scheduleRecovery(ownerUserId: String) {
        val request = OneTimeWorkRequestBuilder<V2CoachDeliveryWorker>()
            .setInputData(workDataOf(V2CoachDeliveryWorker.KEY_OWNER_USER_ID to ownerUserId))
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "coach_delivery_${ownerUserId.hashCode()}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private suspend fun deliverJob(
        job: V2CoachDeliveryJob,
        repository: V2CoachWorkspaceRepository,
    ): Result<V2CoachDeliveryResult> {
        if (job.deliveryType == V2_DELIVERY_LIBRARY_IMPORT) {
            val mediaPath = job.videoPath ?: return Result.failure(
                V2CoachUserFacingException("无法找到待上传素材，请重新选择"),
            )
            val coverPath = job.coverPath ?: return Result.failure(
                V2CoachUserFacingException("无法找到素材封面，请重新选择视频"),
            )
            return repository.uploadWorkspaceVideo(
                connectionId = null,
                videoFile = File(mediaPath),
                coverFile = File(coverPath),
                clientUploadId = job.uploadId,
                title = job.title,
                tags = job.tags,
                durationSeconds = job.durationSeconds,
                sourceKind = job.sourceKind,
                savedToLibrary = true,
                onUploadProgress = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.UPLOADING, it) },
                onPublishing = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0) },
            ).map { assetId -> V2CoachDeliveryResult(libraryAssetId = assetId) }
        }
        val resolvedAttachments = resolveAttachments(job, repository).getOrElse {
            return Result.failure(it)
        }
        val attachmentAssetIds = resolvedAttachments.mapNotNull(V2CoachQueuedAttachment::existingAssetId)
        val savedAttachmentIds = resolvedAttachments
            .filter(V2CoachQueuedAttachment::saveToLibrary)
            .mapNotNull(V2CoachQueuedAttachment::existingAssetId)

        if (job.sourceKind == "library_asset") {
            updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0)
            val parentItemId = job.parentItemId ?: return Result.failure(
                V2CoachUserFacingException("素材回复缺少对应交流，请重新选择"),
            )
            val assetId = job.existingAssetId ?: return Result.failure(
                V2CoachUserFacingException("无法找到待发送素材，请重新选择"),
            )
            return repository.createAssetReply(
                connectionId = job.connectionId,
                parentItemId = parentItemId,
                text = job.text,
                assetId = assetId,
                clientRequestId = job.requestId,
                replyToItemId = job.replyToItemId,
                replyAtSeconds = job.replyAtSeconds,
                attachmentAssetIds = attachmentAssetIds,
                saveAssetIdsToLibrary = savedAttachmentIds,
            ).map { V2CoachDeliveryResult(workspaceItem = it) }
        }
        if (job.sourceKind == "text") {
            updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0)
            return if (job.parentItemId == null) {
                repository.createTextPost(
                    connectionId = job.connectionId,
                    text = job.text,
                    requiresCoachReview = job.requiresCoachReview,
                    contentMode = V2WorkspaceContentMode.entries.firstOrNull {
                        it.wireValue == job.contentMode
                    } ?: V2WorkspaceContentMode.PRACTICE,
                    clientRequestId = job.requestId,
                    attachmentAssetIds = attachmentAssetIds,
                    saveAssetIdsToLibrary = savedAttachmentIds,
                ).map { V2CoachDeliveryResult(workspaceItem = it) }
            } else {
                repository.createTextReply(
                    connectionId = job.connectionId,
                    parentItemId = job.parentItemId,
                    text = job.text,
                    replyToItemId = job.replyToItemId,
                    replyAtSeconds = job.replyAtSeconds,
                    clientRequestId = job.requestId,
                    attachmentAssetIds = attachmentAssetIds,
                    saveAssetIdsToLibrary = savedAttachmentIds,
                ).map { V2CoachDeliveryResult(workspaceItem = it) }
            }
        }
        val mediaPath = job.videoPath ?: return Result.failure(
            V2CoachUserFacingException("无法找到待发送媒体，请重新选择"),
        )
        val assetId = if (job.sourceKind == "voice_recording") {
            repository.uploadWorkspaceAudio(
                connectionId = job.connectionId,
                audioFile = File(mediaPath),
                clientUploadId = job.uploadId,
                durationSeconds = job.durationSeconds,
                onUploadProgress = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.UPLOADING, it) },
                onPublishing = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0) },
            )
        } else {
            repository.uploadWorkspaceVideo(
                connectionId = job.connectionId,
                videoFile = File(mediaPath),
                coverFile = job.coverPath?.let(::File) ?: return Result.failure(
                    V2CoachUserFacingException("无法找到视频封面，请重新选择视频"),
                ),
                clientUploadId = job.uploadId,
                title = job.title,
                tags = job.tags,
                durationSeconds = job.durationSeconds,
                sourceKind = job.sourceKind,
                savedToLibrary = job.saveToLibrary,
                onUploadProgress = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.UPLOADING, it) },
                onPublishing = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0) },
            )
        }.getOrElse { return Result.failure(it) }
        updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0)
        val published = if (job.parentItemId == null) {
            repository.createAssetPost(
                connectionId = job.connectionId,
                title = job.title,
                text = job.text,
                assetId = assetId,
                clientRequestId = job.requestId,
                requiresCoachReview = job.requiresCoachReview,
                contentMode = V2WorkspaceContentMode.entries.firstOrNull {
                    it.wireValue == job.contentMode
                } ?: V2WorkspaceContentMode.PRACTICE,
                attachmentAssetIds = attachmentAssetIds,
                saveAssetIdsToLibrary = buildList {
                    if (job.saveToLibrary) add(assetId)
                    addAll(savedAttachmentIds)
                },
                sourceRecordId = job.sourceRecordId,
            )
        } else {
            repository.createAssetReply(
                connectionId = job.connectionId,
                parentItemId = job.parentItemId,
                text = job.text,
                assetId = assetId,
                clientRequestId = job.requestId,
                replyToItemId = job.replyToItemId,
                replyAtSeconds = job.replyAtSeconds,
                attachmentAssetIds = attachmentAssetIds,
                saveAssetIdsToLibrary = buildList {
                    if (job.saveToLibrary) add(assetId)
                    addAll(savedAttachmentIds)
                },
            )
        }
        return published.map { V2CoachDeliveryResult(workspaceItem = it) }
    }

    private suspend fun deliverLocked(
        jobId: String,
        repository: V2CoachWorkspaceRepository,
    ): Result<V2CoachDeliveryResult> {
        val blocked = mutex.withLock {
            val jobs = readJobs()
            val current = jobs.firstOrNull { it.jobId == jobId } ?: return@withLock null
            if (!current.requiresCellularUploadConfirmation(appContext.isActiveNetworkMetered())) {
                return@withLock null
            }
            val fileSizeBytes = current.localUploadBytes()
            val message = "大视频正在等待 Wi-Fi；如需使用手机流量，请先确认"
            val updated = jobs.map { job ->
                if (job.jobId == jobId) job.copy(lastError = message) else job
            }
            writeJobs(updated)
            _state.value = V2CoachDeliveryState(
                pendingCount = updated.size,
                message = message,
                jobs = updated,
            )
            V2CellularUploadConfirmationRequired(jobId, fileSizeBytes)
        }
        if (blocked != null) return Result.failure(blocked)

        val job = mutex.withLock {
            val jobs = readJobs()
            val current = jobs.firstOrNull { it.jobId == jobId }
                ?: return@withLock null
            val attempted = current.copy(
                attemptCount = current.attemptCount + 1,
                lastError = null,
                pausedByUser = false,
            )
            val updated = jobs.map { if (it.jobId == jobId) attempted else it }
            writeJobs(updated)
            _state.value = V2CoachDeliveryState(
                pendingCount = updated.size,
                message = "正在发送内容",
                jobs = updated,
                activeJobId = jobId,
                activePhase = V2CoachDeliveryPhase.PREPARING,
            )
            attempted
        } ?: return Result.failure(V2CoachUserFacingException("发送草稿已失效，请重新选择内容"))

        val result = try {
            deliverJob(job, repository)
        } catch (cancelled: CancellationException) {
            val jobs = mutex.withLock { readJobs() }
            val stoppedByUser = jobs.any { it.jobId == jobId && it.pausedByUser }
            _state.value = V2CoachDeliveryState(
                pendingCount = jobs.size,
                message = if (stoppedByUser) "发送已停止，内容仍保留在队列中" else "发送已转交后台队列",
                jobs = jobs,
            )
            throw cancelled
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
        mutex.withLock {
            val jobs = readJobs()
            if (result.isSuccess) {
                job.deleteFiles()
                val remaining = jobs.filterNot { it.jobId == job.jobId }
                writeJobs(remaining)
                _state.value = V2CoachDeliveryState(
                    pendingCount = remaining.size,
                    message = "内容已送达",
                    jobs = remaining,
                    completion = result.getOrNull()?.let { deliveryCompletion(job, it) },
                )
            } else {
                val message = result.exceptionOrNull()?.let {
                    coachThrowableError("内容暂未送达，请稍后重试", it)
                }?.take(160) ?: "内容暂未送达，请稍后重试"
                val failed = jobs.map { current ->
                    if (current.jobId == job.jobId) current.copy(lastError = message) else current
                }
                writeJobs(failed)
                _state.value = V2CoachDeliveryState(
                    pendingCount = failed.size,
                    message = "内容暂未送达，稍后会继续恢复",
                    jobs = failed,
                )
            }
        }
        if (result.isFailure) scheduleRecoverySafely(job.ownerUserId)
        return result
    }

    private fun updateActiveProgress(
        jobId: String,
        phase: V2CoachDeliveryPhase,
        progress: Double,
    ) {
        val current = _state.value
        if (current.activeJobId != jobId) return
        val normalized = progress.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0
        val percent = (normalized * 100).toInt()
        _state.value = current.copy(
            message = when (phase) {
                V2CoachDeliveryPhase.PREPARING -> phase.displayText
                V2CoachDeliveryPhase.UPLOADING -> "${phase.displayText} $percent%"
                V2CoachDeliveryPhase.PUBLISHING -> phase.displayText
            },
            activePhase = phase,
            activeProgress = normalized,
        )
    }

    private fun readJobs(): List<V2CoachDeliveryJob> = runCatching {
        json.decodeFromString(
            ListSerializer(V2CoachDeliveryJob.serializer()),
            preferences.getString(KEY_JOBS, "[]") ?: "[]",
        )
    }.getOrDefault(emptyList())

    private fun writeJobs(jobs: List<V2CoachDeliveryJob>) {
        preferences.edit {
            putString(KEY_JOBS, json.encodeToString(ListSerializer(V2CoachDeliveryJob.serializer()), jobs))
        }
    }

    /** Persistence is the acceptance boundary; an OS scheduler hiccup must not reject its payload. */
    private fun scheduleRecoverySafely(ownerUserId: String) {
        runCatching { scheduleRecovery(ownerUserId) }.onFailure {
            val current = _state.value
            _state.value = current.copy(
                message = "内容已保存在发送队列；下次打开应用时会继续",
            )
        }
    }

    private fun V2CoachDeliveryJob.deleteFiles() {
        videoPath?.let(::File)?.delete()
        coverPath?.let(::File)?.delete()
        attachments.forEach { attachment ->
            attachment.videoPath?.let(::File)?.delete()
            attachment.coverPath?.let(::File)?.delete()
        }
    }

    private fun V2CoachDeliveryJob.hasRecoverablePayload(): Boolean =
        boundedCoachAttachments(attachments).all { it.hasRecoverablePayload() } && when (sourceKind) {
        "text" -> text.isNotBlank() || attachments.isNotEmpty()
        "library_asset" -> !existingAssetId.isNullOrBlank() && !parentItemId.isNullOrBlank()
        "voice_recording" -> videoPath?.let(::File)?.isFile == true
        else -> videoPath?.let(::File)?.isFile == true && coverPath?.let(::File)?.isFile == true
    }

    private fun V2CoachQueuedAttachment.hasRecoverablePayload(): Boolean =
        if (!existingAssetId.isNullOrBlank()) {
            true
        } else {
            videoPath?.let(::File)?.isFile == true && coverPath?.let(::File)?.isFile == true
        }

    private suspend fun resolveAttachments(
        job: V2CoachDeliveryJob,
        repository: V2CoachWorkspaceRepository,
    ): Result<List<V2CoachQueuedAttachment>> = runCatching {
        val resolved = boundedCoachAttachments(job.attachments).toMutableList()
        for (index in resolved.indices) {
            val attachment = resolved[index]
            if (!attachment.existingAssetId.isNullOrBlank()) continue
            val video = attachment.videoPath?.let(::File)?.takeIf(File::isFile)
                ?: throw V2CoachUserFacingException("一段待发送视频已不在本机，请移除后重新发送")
            val cover = attachment.coverPath?.let(::File)?.takeIf(File::isFile)
                ?: throw V2CoachUserFacingException("一段待发送视频缺少封面，请移除后重新发送")
            val savedToLibrary = attachment.saveToLibrary &&
                !(job.parentItemId != null && attachment.sourceKind == "album")
            val assetId = repository.uploadWorkspaceVideo(
                connectionId = job.connectionId,
                videoFile = video,
                coverFile = cover,
                clientUploadId = attachment.uploadId,
                title = attachment.title,
                tags = emptyList(),
                durationSeconds = attachment.durationSeconds,
                sourceKind = attachment.sourceKind,
                savedToLibrary = savedToLibrary,
                onUploadProgress = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.UPLOADING, it) },
                onPublishing = { updateActiveProgress(job.jobId, V2CoachDeliveryPhase.PUBLISHING, 1.0) },
            ).getOrThrow()
            resolved[index] = attachment.copy(existingAssetId = assetId, saveToLibrary = savedToLibrary)
            persistResolvedAttachments(job.jobId, resolved)
        }
        resolved
    }

    private suspend fun persistResolvedAttachments(
        jobId: String,
        attachments: List<V2CoachQueuedAttachment>,
    ) = mutex.withLock {
        val jobs = readJobs()
        writeJobs(jobs.map { job ->
            if (job.jobId == jobId) job.copy(attachments = boundedCoachAttachments(attachments)) else job
        })
    }

    private companion object {
        const val KEY_JOBS = "jobs"
        const val LIFETIME_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

private class V2CoachDeliveryStopped(jobId: String) :
    CancellationException("User stopped coach delivery $jobId")

private fun Context.isActiveNetworkMetered(): Boolean {
    val connectivity = getSystemService(ConnectivityManager::class.java) ?: return false
    val activeNetwork = connectivity.activeNetwork ?: return false
    val capabilities = connectivity.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        connectivity.isActiveNetworkMetered
}
