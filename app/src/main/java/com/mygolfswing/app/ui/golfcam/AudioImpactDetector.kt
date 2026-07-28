package com.mygolfswing.app.ui.golfcam

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.mygolfswing.app.ui.golfcam.model.ImpactCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

internal fun audioSamplePresentationTimeUs(
    bufferPresentationTimeUs: Long,
    frameIndex: Int,
    sampleRate: Int,
): Long {
    return bufferPresentationTimeUs + frameIndex.coerceAtLeast(0) * 1_000_000L / sampleRate.coerceAtLeast(1)
}

internal fun shouldKeepDecodedAudioSample(
    sampleTimeUs: Long,
    trimStartUs: Long,
    trimEndUs: Long,
): Boolean {
    return sampleTimeUs >= trimStartUs && sampleTimeUs < trimEndUs
}

internal fun audioCandidateBaseTimeSeconds(trimStartSec: Double, firstKeptSampleTimeUs: Long?): Double {
    val safeTrimStartSec = trimStartSec.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    return firstKeptSampleTimeUs?.let { (it / 1_000_000.0).coerceAtLeast(safeTrimStartSec) } ?: safeTrimStartSec
}

/**
 * 音频冲击帧检测器（对齐 iOS AudioImpactDetectorV4Simple）。
 *
 * 算法：
 * 1. MediaExtractor + MediaCodec 解码音频为 PCM（单声道混合）
 * 2. 10ms 窗口、2.5ms 步进，计算 RMS 能量
 * 3. 找局部最大值，过滤 sharpness = peak / 邻域均值 > 2.0 且能量 > 0.03
 * 4. 按 energy × sharpness 排序，取前 5 个
 * 5. confidence = 该峰分 / 最高峰分（第 1 名恒为 1.0）
 */
object AudioImpactDetector {

    private const val MAX_CANDIDATES = 5
    private const val SHARPNESS_THRESHOLD = 2.0f
    private const val ENERGY_THRESHOLD = 0.03f
    private const val MIN_PEAK_INTERVAL_SEC = 0.1  // 峰间距最小 100ms

    suspend fun detect(
        context: Context,
        uri: Uri,
        startSec: Double = 0.0,
        endSec: Double = Double.MAX_VALUE,
    ): List<ImpactCandidate> = withContext(Dispatchers.IO) {
        try {
            val safeStartSec = startSec.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            val safeEndSec = endSec.takeIf { it.isFinite() }?.coerceAtLeast(safeStartSec) ?: Double.MAX_VALUE
            val pcm = extractPcm(context, uri, safeStartSec, safeEndSec) ?: return@withContext emptyList()
            findPeaks(pcm.samples, pcm.sampleRate, pcm.baseTimeSec)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── PCM 解码 ──────────────────────────────────────────────────────────────

    private data class PcmData(val samples: FloatArray, val sampleRate: Int, val baseTimeSec: Double)

    private fun extractPcm(context: Context, uri: Uri, startSec: Double, endSec: Double): PcmData? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var codecStarted = false
        return try {
            extractor.setDataSource(context, uri, null)

            // 找音频轨
            val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            // 定位到开始时间
            if (startSec > 0.0) {
                extractor.seekTo((startSec * 1_000_000).toLong(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            codecStarted = true

            val allSamples = mutableListOf<Float>()
            val startUs = (startSec.coerceAtLeast(0.0) * 1_000_000).toLong()
            val endUs = if (endSec < Double.MAX_VALUE) (endSec * 1_000_000).toLong() else Long.MAX_VALUE
            var firstKeptSampleTimeUs: Long? = null
            var isEos = false
            val bufferInfo = MediaCodec.BufferInfo()

            while (!isEos) {
                // 喂数据
                val inIdx = codec.dequeueInputBuffer(5000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0 || extractor.sampleTime >= endUs) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                // 取输出
                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 5000)
                if (outIdx >= 0) {
                    val buf = codec.getOutputBuffer(outIdx)
                    if (buf != null && bufferInfo.size > 0) {
                        val outputBuffer = buf.duplicate().apply {
                            position(bufferInfo.offset)
                            limit(bufferInfo.offset + bufferInfo.size)
                        }
                        val shortBuf = outputBuffer.asShortBuffer()
                        val frameCount = shortBuf.remaining() / channelCount
                        repeat(frameCount) { f ->
                            val sampleTimeUs = audioSamplePresentationTimeUs(
                                bufferPresentationTimeUs = bufferInfo.presentationTimeUs,
                                frameIndex = f,
                                sampleRate = sampleRate,
                            )
                            if (!shouldKeepDecodedAudioSample(sampleTimeUs, startUs, endUs)) return@repeat
                            if (firstKeptSampleTimeUs == null) firstKeptSampleTimeUs = sampleTimeUs
                            var mix = 0f
                            for (ch in 0 until channelCount) {
                                mix += shortBuf.get(f * channelCount + ch) / 32768f
                            }
                            allSamples.add(mix / channelCount)
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) isEos = true
                }
            }

            if (allSamples.isEmpty()) {
                null
            } else {
                PcmData(
                    samples = allSamples.toFloatArray(),
                    sampleRate = sampleRate,
                    baseTimeSec = audioCandidateBaseTimeSeconds(startSec, firstKeptSampleTimeUs),
                )
            }
        } finally {
            if (codecStarted) runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    // ── 峰值检测 ──────────────────────────────────────────────────────────────

    private fun findPeaks(samples: FloatArray, sampleRate: Int, startSec: Double): List<ImpactCandidate> {
        val safeSampleRate = sampleRate.coerceAtLeast(1)
        val safeStartSec = startSec.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val windowSize = (safeSampleRate * 0.010).toInt().coerceAtLeast(1)   // 10ms
        val hopSize = (safeSampleRate * 0.0025).toInt().coerceAtLeast(1)     // 2.5ms
        val minPeakInterval = (MIN_PEAK_INTERVAL_SEC * safeSampleRate / hopSize).toInt()

        // 计算 RMS 能量序列
        val energyList = mutableListOf<Float>()
        var offset = 0
        while (offset + windowSize <= samples.size) {
            var sumSq = 0.0
            for (i in offset until offset + windowSize) sumSq += (samples[i] * samples[i]).toDouble()
            energyList.add(sqrt(sumSq / windowSize).toFloat())
            offset += hopSize
        }
        if (energyList.isEmpty()) return emptyList()

        val energy = energyList.toFloatArray()
        val neighborRadius = 8  // 用于计算邻域均值

        data class Peak(val index: Int, val energy: Float, val sharpness: Float)

        val peaks = mutableListOf<Peak>()

        for (i in neighborRadius until energy.size - neighborRadius) {
            val curr = energy[i]
            if (curr < ENERGY_THRESHOLD) continue

            // 是否局部最大
            var isLocal = true
            for (j in i - 3..i + 3) {
                if (j != i && energy[j] >= curr) { isLocal = false; break }
            }
            if (!isLocal) continue

            // 计算邻域均值（排除中心）
            var neighborSum = 0f
            for (j in i - neighborRadius..i + neighborRadius) {
                if (j != i) neighborSum += energy[j]
            }
            val neighborMean = neighborSum / (neighborRadius * 2)
            val sharpness = if (neighborMean > 0f) curr / neighborMean else 0f
            if (sharpness < SHARPNESS_THRESHOLD) continue

            peaks.add(Peak(i, curr, sharpness))
        }

        if (peaks.isEmpty()) return emptyList()

        // 按 energy × sharpness 排序
        val sorted = peaks.sortedByDescending { it.energy * it.sharpness }

        // 去除过于接近的峰
        val selected = mutableListOf<Peak>()
        for (p in sorted) {
            val tooClose = selected.any { abs(it.index - p.index) < minPeakInterval }
            if (!tooClose) selected.add(p)
            if (selected.size >= MAX_CANDIDATES) break
        }

        if (selected.isEmpty()) return emptyList()

        val maxScore = (selected[0].energy * selected[0].sharpness)
            .takeIf { it.isFinite() && it > 0f }
            ?: return emptyList()

        return selected.map { p ->
            val score = (p.energy * p.sharpness).takeIf { it.isFinite() && it >= 0f } ?: 0f
            val relSec = p.index.toDouble() * hopSize / safeSampleRate
            ImpactCandidate(
                timeSec = safeStartSec + relSec,
                confidence = (score / maxScore).coerceIn(0f, 1f),
            )
        }
    }
}
