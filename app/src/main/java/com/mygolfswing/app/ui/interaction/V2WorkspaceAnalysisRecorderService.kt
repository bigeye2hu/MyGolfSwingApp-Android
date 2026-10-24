package com.mygolfswing.app.ui.interaction

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

internal enum class V2AnalysisRecordingPhase {
    IDLE,
    STARTING,
    RECORDING,
    PAUSED,
    FINISHED,
    FAILED,
}

internal data class V2AnalysisRecordingState(
    val recordingId: String? = null,
    val phase: V2AnalysisRecordingPhase = V2AnalysisRecordingPhase.IDLE,
    val outputPath: String? = null,
    val error: String? = null,
    val includeMicrophone: Boolean = true,
    val elapsedMs: Long = 0L,
    val phaseStartedElapsedRealtimeMs: Long? = null,
)

internal fun currentWorkspaceRecordingElapsedMs(
    state: V2AnalysisRecordingState,
    nowElapsedRealtimeMs: Long,
): Long {
    val activePhaseMs = if (
        state.phase == V2AnalysisRecordingPhase.RECORDING &&
        state.phaseStartedElapsedRealtimeMs != null
    ) {
        (nowElapsedRealtimeMs - state.phaseStartedElapsedRealtimeMs).coerceAtLeast(0L)
    } else {
        0L
    }
    return (state.elapsedMs + activePhaseMs).coerceAtLeast(0L)
}

internal fun formatWorkspaceRecordingElapsed(elapsedMs: Long): String {
    val totalSeconds = elapsedMs.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

internal object V2WorkspaceAnalysisRecorder {
    private val _state = MutableStateFlow(V2AnalysisRecordingState())
    val state: StateFlow<V2AnalysisRecordingState> = _state.asStateFlow()

    fun start(
        context: Context,
        resultCode: Int,
        captureData: Intent,
        includeMicrophone: Boolean = true,
    ): String {
        val recordingId = UUID.randomUUID().toString()
        val output = File(context.cacheDir, "workspace-analysis-$recordingId.mp4")
        _state.value = V2AnalysisRecordingState(
            recordingId = recordingId,
            phase = V2AnalysisRecordingPhase.STARTING,
            outputPath = output.absolutePath,
            includeMicrophone = includeMicrophone,
        )
        val intent = Intent(context, V2WorkspaceAnalysisRecorderService::class.java)
            .setAction(V2WorkspaceAnalysisRecorderService.ACTION_START)
            .putExtra(V2WorkspaceAnalysisRecorderService.EXTRA_RECORDING_ID, recordingId)
            .putExtra(V2WorkspaceAnalysisRecorderService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(V2WorkspaceAnalysisRecorderService.EXTRA_CAPTURE_DATA, captureData)
            .putExtra(V2WorkspaceAnalysisRecorderService.EXTRA_OUTPUT_PATH, output.absolutePath)
            .putExtra(V2WorkspaceAnalysisRecorderService.EXTRA_INCLUDE_MICROPHONE, includeMicrophone)
        ContextCompat.startForegroundService(context, intent)
        return recordingId
    }

    fun pause(context: Context) = send(context, V2WorkspaceAnalysisRecorderService.ACTION_PAUSE)

    fun resume(context: Context) = send(context, V2WorkspaceAnalysisRecorderService.ACTION_RESUME)

    fun finish(context: Context) = send(context, V2WorkspaceAnalysisRecorderService.ACTION_FINISH)

    fun cancel(context: Context) = send(context, V2WorkspaceAnalysisRecorderService.ACTION_CANCEL)

    fun consume(recordingId: String) {
        if (_state.value.recordingId == recordingId) _state.value = V2AnalysisRecordingState()
    }

    internal fun update(state: V2AnalysisRecordingState) {
        _state.value = state
    }

    private fun send(context: Context, action: String) {
        context.startService(Intent(context, V2WorkspaceAnalysisRecorderService::class.java).setAction(action))
    }
}

class V2WorkspaceAnalysisRecorderService : Service() {
    private var recordingId: String? = null
    private var outputFile: File? = null
    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var finishing = false
    private var includeMicrophone = true
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (!finishing) finishRecording(cancel = true, error = "系统已停止录屏，请重新开始")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_FINISH -> finishRecording(cancel = false)
            ACTION_CANCEL -> finishRecording(cancel = true)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (recorder != null) return
        val id = intent.getStringExtra(EXTRA_RECORDING_ID) ?: return stopSelf()
        val path = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: return stopSelf()
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        includeMicrophone = intent.getBooleanExtra(EXTRA_INCLUDE_MICROPHONE, true)
        val captureData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CAPTURE_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CAPTURE_DATA)
        } ?: return failStart(id, path, "无法取得系统录屏授权，请重试")
        recordingId = id
        outputFile = File(path).also {
            it.parentFile?.mkdirs()
            if (it.exists()) it.delete()
        }
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("正在录制专业分析")
            .setContentText(
                if (includeMicrophone) "录屏和麦克风正在工作，回到挥呗即可暂停或完成"
                else "正在录制屏幕，回到挥呗即可暂停或完成",
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or if (includeMicrophone) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val metrics = resources.displayMetrics
            val (width, height) = workspaceAnalysisRecorderSize(metrics.widthPixels, metrics.heightPixels)
            val activeRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = activeRecorder
            activeRecorder.apply {
                if (includeMicrophone) setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(path)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (includeMicrophone) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(8_000_000)
                setVideoFrameRate(30)
                setVideoSize(width, height)
                if (includeMicrophone) {
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                }
                prepare()
            }
            val manager = getSystemService(MediaProjectionManager::class.java)
            val activeProjection = manager.getMediaProjection(resultCode, captureData)
                ?: error("系统未返回可用的录屏会话")
            projection = activeProjection
            activeProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
            virtualDisplay = activeProjection.createVirtualDisplay(
                "HuibeiProfessionalAnalysis",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                activeRecorder.surface,
                null,
                null,
            )
            activeRecorder.start()
            val startedAt = SystemClock.elapsedRealtime()
            V2WorkspaceAnalysisRecorder.update(
                V2AnalysisRecordingState(
                    recordingId = id,
                    phase = V2AnalysisRecordingPhase.RECORDING,
                    outputPath = path,
                    includeMicrophone = includeMicrophone,
                    phaseStartedElapsedRealtimeMs = startedAt,
                ),
            )
        } catch (throwable: Throwable) {
            finishRecording(
                cancel = true,
                error = if (includeMicrophone) {
                    "无法开始录屏，请检查麦克风权限后重试"
                } else {
                    "无法开始录屏，请稍后重试"
                },
            )
        }
    }

    private fun pauseRecording() {
        val id = recordingId ?: return
        runCatching { recorder?.pause() }
            .onSuccess {
                val current = V2WorkspaceAnalysisRecorder.state.value
                val elapsed = currentWorkspaceRecordingElapsedMs(current, SystemClock.elapsedRealtime())
                V2WorkspaceAnalysisRecorder.update(
                    V2AnalysisRecordingState(
                        recordingId = id,
                        phase = V2AnalysisRecordingPhase.PAUSED,
                        outputPath = outputFile?.absolutePath,
                        includeMicrophone = includeMicrophone,
                        elapsedMs = elapsed,
                    ),
                )
            }
            .onFailure { finishRecording(cancel = true, error = "录屏暂停失败，请重新录制") }
    }

    private fun resumeRecording() {
        val id = recordingId ?: return
        runCatching { recorder?.resume() }
            .onSuccess {
                val current = V2WorkspaceAnalysisRecorder.state.value
                V2WorkspaceAnalysisRecorder.update(
                    V2AnalysisRecordingState(
                        recordingId = id,
                        phase = V2AnalysisRecordingPhase.RECORDING,
                        outputPath = outputFile?.absolutePath,
                        includeMicrophone = includeMicrophone,
                        elapsedMs = current.elapsedMs,
                        phaseStartedElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                    ),
                )
            }
            .onFailure { finishRecording(cancel = true, error = "录屏继续失败，请重新录制") }
    }

    private fun finishRecording(cancel: Boolean, error: String? = null) {
        if (finishing) return
        finishing = true
        val id = recordingId
        val file = outputFile
        val current = V2WorkspaceAnalysisRecorder.state.value
        val finalElapsedMs = currentWorkspaceRecordingElapsedMs(current, SystemClock.elapsedRealtime())
        val stoppedCleanly = runCatching { recorder?.stop() }.isSuccess
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        projection?.unregisterCallback(projectionCallback)
        runCatching { projection?.stop() }
        projection = null
        if (cancel || !stoppedCleanly || file?.isFile != true || file.length() <= 0L) file?.delete()
        val finalState = when {
            error != null -> V2AnalysisRecordingState(
                recordingId = id,
                phase = V2AnalysisRecordingPhase.FAILED,
                error = error,
                includeMicrophone = includeMicrophone,
                elapsedMs = finalElapsedMs,
            )
            cancel -> V2AnalysisRecordingState(
                recordingId = id,
                phase = V2AnalysisRecordingPhase.IDLE,
                elapsedMs = finalElapsedMs,
            )
            file?.isFile == true && file.length() > 0L ->
                V2AnalysisRecordingState(
                    recordingId = id,
                    phase = V2AnalysisRecordingPhase.FINISHED,
                    outputPath = file.absolutePath,
                    includeMicrophone = includeMicrophone,
                    elapsedMs = finalElapsedMs,
                )
            else -> V2AnalysisRecordingState(
                recordingId = id,
                phase = V2AnalysisRecordingPhase.FAILED,
                error = "录屏时间太短，请重新录制",
                includeMicrophone = includeMicrophone,
                elapsedMs = finalElapsedMs,
            )
        }
        V2WorkspaceAnalysisRecorder.update(finalState)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        finishing = false
    }

    private fun failStart(id: String, path: String, message: String) {
        File(path).delete()
        V2WorkspaceAnalysisRecorder.update(
            V2AnalysisRecordingState(id, V2AnalysisRecordingPhase.FAILED, error = message),
        )
        stopSelf()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "专业分析录屏", NotificationManager.IMPORTANCE_LOW).apply {
                description = "录制教练的视频分析，可选择是否包含麦克风声音"
                setShowBadge(false)
            },
        )
    }

    override fun onDestroy() {
        if (recorder != null && !finishing) finishRecording(cancel = true)
        super.onDestroy()
    }

    companion object {
        internal const val ACTION_START = "com.mygolfswing.app.action.START_ANALYSIS_RECORDING"
        internal const val ACTION_PAUSE = "com.mygolfswing.app.action.PAUSE_ANALYSIS_RECORDING"
        internal const val ACTION_RESUME = "com.mygolfswing.app.action.RESUME_ANALYSIS_RECORDING"
        internal const val ACTION_FINISH = "com.mygolfswing.app.action.FINISH_ANALYSIS_RECORDING"
        internal const val ACTION_CANCEL = "com.mygolfswing.app.action.CANCEL_ANALYSIS_RECORDING"
        internal const val EXTRA_RECORDING_ID = "recording_id"
        internal const val EXTRA_RESULT_CODE = "result_code"
        internal const val EXTRA_CAPTURE_DATA = "capture_data"
        internal const val EXTRA_OUTPUT_PATH = "output_path"
        internal const val EXTRA_INCLUDE_MICROPHONE = "include_microphone"
        private const val CHANNEL_ID = "workspace_analysis_recording"
        private const val NOTIFICATION_ID = 24_901
    }
}

internal fun workspaceAnalysisRecorderSize(rawWidth: Int, rawHeight: Int): Pair<Int, Int> {
    val width = rawWidth.coerceAtLeast(2)
    val height = rawHeight.coerceAtLeast(2)
    val scale = minOf(1.0, 1_920.0 / maxOf(width, height).toDouble())
    return ((width * scale).roundToInt() / 2 * 2).coerceAtLeast(2) to
        ((height * scale).roundToInt() / 2 * 2).coerceAtLeast(2)
}
