package com.mygolfswing.app.ui.interaction

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.UUID

data class V2WorkspaceVoiceRecording(
    val file: File,
    val durationSeconds: Double,
)

class V2WorkspaceVoiceRecorder(
    private val context: Context,
    private val directoryName: String = "coach_voice_drafts",
) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAtMillis: Long = 0

    val isRecording: Boolean get() = recorder != null
    val currentAmplitude: Int get() = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun start(): Result<Unit> = runCatching {
        check(recorder == null) { "录音已经开始" }
        val root = File(context.cacheDir, directoryName).apply { mkdirs() }
        val file = File(root, "voice_${UUID.randomUUID()}.m4a")
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioSamplingRate(44_100)
            next.setAudioChannels(1)
            next.setAudioEncodingBitRate(96_000)
            next.setMaxDuration(60_000)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
        } catch (error: Throwable) {
            next.release()
            file.delete()
            throw error
        }
        output = file
        recorder = next
        startedAtMillis = SystemClock.elapsedRealtime()
    }

    fun stop(cancel: Boolean = false): Result<V2WorkspaceVoiceRecording?> = runCatching {
        val current = recorder ?: return@runCatching null
        val file = output
        val duration = (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0) / 1_000.0
        recorder = null
        output = null
        startedAtMillis = 0
        try {
            current.stop()
        } catch (error: RuntimeException) {
            file?.delete()
            throw error
        } finally {
            current.release()
        }
        if (cancel || duration < 0.45 || file == null || !file.isFile || file.length() == 0L) {
            file?.delete()
            null
        } else {
            V2WorkspaceVoiceRecording(file, duration)
        }
    }
}
