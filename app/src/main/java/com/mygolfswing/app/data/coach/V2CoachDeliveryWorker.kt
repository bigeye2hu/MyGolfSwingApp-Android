package com.mygolfswing.app.data.coach

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

class V2CoachDeliveryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)
        val ownerUserId = inputData.getString(KEY_OWNER_USER_ID)?.takeIf(String::isNotBlank)
            ?: return Result.failure()
        val currentUserId = ServiceLocator.session.current().userId
        if (currentUserId != ownerUserId) return Result.success()
        val pendingCount = ServiceLocator.coachDeliveryQueue.automaticPendingCount(ownerUserId)
        if (pendingCount == 0) return Result.success()
        // A foreground worker is the Android counterpart to iOS' background URLSession:
        // it keeps long non-chunkable uploads important to the OS and visible to the user.
        runCatching { setForeground(createForegroundInfo(ownerUserId, pendingCount)) }
        return try {
            ServiceLocator.coachDeliveryQueue.recover(ownerUserId, ServiceLocator.coachWorkspaceRepo)
            if (ServiceLocator.coachDeliveryQueue.automaticPendingCount(ownerUserId) == 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun createForegroundInfo(ownerUserId: String, pendingCount: Int): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "空间内容发送",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "在后台可靠发送视频、语音和空间内容"
                setShowBadge(false)
            },
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("挥呗正在发送内容")
            .setContentText("$pendingCount 条内容正在可靠发送，完成前请保持网络连接")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setLocalOnly(true)
            .setProgress(0, 0, true)
            .build()
        val notificationId = NOTIFICATION_ID_BASE + (ownerUserId.hashCode() and 0x0FFF)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val KEY_OWNER_USER_ID = "owner_user_id"
        private const val CHANNEL_ID = "coach_delivery"
        private const val NOTIFICATION_ID_BASE = 24_000
    }
}
