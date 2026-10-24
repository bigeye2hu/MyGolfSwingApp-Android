package com.mygolfswing.app.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

class V2AsyncDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)
        val taskId = inputData.getString(KEY_TASK_ID)?.takeIf(String::isNotBlank) ?: return Result.failure()
        runCatching { setForeground(createForegroundInfo(taskId)) }
        return try {
            if (ServiceLocator.asyncDownloadQueue.runTask(taskId)) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            Result.success()
        }
    }

    private fun createForegroundInfo(taskId: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    V2AsyncDownloadQueue.CHANNEL_PROGRESS,
                    "视频后台下载",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, V2AsyncDownloadQueue.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("挥呗正在后台下载视频")
            .setContentText("可返回应用查看进度或暂停任务")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setLocalOnly(true)
            .setProgress(0, 0, true)
            .build()
        val notificationId = V2AsyncDownloadQueue.NOTIFICATION_ID_BASE + (taskId.hashCode() and 0x0FFF)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}
