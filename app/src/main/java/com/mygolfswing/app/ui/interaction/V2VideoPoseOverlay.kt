package com.mygolfswing.app.ui.interaction

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.scale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

internal data class V2PosePoint(
    val type: Int,
    val x: Float,
    val y: Float,
)

internal data class V2PoseConnection(
    val startType: Int,
    val endType: Int,
    val isAlignmentGuide: Boolean = false,
)

internal const val V2_POSE_REQUEST_INTERVAL_MS = 160L
internal const val V2_POSE_MAX_INFERENCE_SIDE = 1_280
internal const val V2_POSE_MIN_CONFIDENCE = 0.24f

internal val v2PoseConnections = listOf(
    V2PoseConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, true),
    V2PoseConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
    V2PoseConnection(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
    V2PoseConnection(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
    V2PoseConnection(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
    V2PoseConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, true),
    V2PoseConnection(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, true),
    V2PoseConnection(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
    V2PoseConnection(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
    V2PoseConnection(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
    V2PoseConnection(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
    V2PoseConnection(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
)

/**
 * Extracts a local-video frame and runs the bundled ML Kit detector. The 160 ms gate mirrors the
 * current iOS Vision overlay and keeps pose inference off the UI thread while scrubbing/playback.
 */
internal class V2VideoPoseAnalyzer(sourceFile: File) : Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val retriever = MediaMetadataRetriever().apply { setDataSource(sourceFile.absolutePath) }
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build(),
    )
    private val inFlight = AtomicBoolean(false)
    private val generation = AtomicLong(0L)
    @Volatile private var closed = false
    @Volatile private var lastRequestMs = Long.MIN_VALUE

    fun requestPose(
        positionMs: Long,
        force: Boolean = false,
        onResult: (List<V2PosePoint>) -> Unit,
    ) {
        val safePosition = positionMs.coerceAtLeast(0L)
        if (closed || inFlight.get()) return
        if (!force && lastRequestMs != Long.MIN_VALUE &&
            kotlin.math.abs(safePosition - lastRequestMs) <= V2_POSE_REQUEST_INTERVAL_MS
        ) return
        lastRequestMs = safePosition
        if (!inFlight.compareAndSet(false, true)) return
        val requestGeneration = generation.get()

        executor.execute {
            if (closed || generation.get() != requestGeneration) {
                inFlight.set(false)
                return@execute
            }
            val original = runCatching {
                retriever.getFrameAtTime(
                    safePosition * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )
            }.getOrNull()
            if (original == null) {
                finish(requestGeneration, emptyList(), onResult)
                return@execute
            }
            val inferenceBitmap = original.scaledForPoseDetection()
            val image = InputImage.fromBitmap(inferenceBitmap, 0)
            runCatching {
                detector.process(image)
                    .addOnSuccessListener { pose ->
                        val width = inferenceBitmap.width.coerceAtLeast(1).toFloat()
                        val height = inferenceBitmap.height.coerceAtLeast(1).toFloat()
                        val points = pose.allPoseLandmarks.mapNotNull { landmark ->
                            if (landmark.inFrameLikelihood <= V2_POSE_MIN_CONFIDENCE) return@mapNotNull null
                            V2PosePoint(
                                type = landmark.landmarkType,
                                x = (landmark.position.x / width).coerceIn(0f, 1f),
                                y = (landmark.position.y / height).coerceIn(0f, 1f),
                            )
                        }
                        finish(requestGeneration, points, onResult)
                    }
                    .addOnFailureListener {
                        finish(requestGeneration, emptyList(), onResult)
                    }
                    .addOnCompleteListener {
                        if (inferenceBitmap !== original) inferenceBitmap.recycle()
                        original.recycle()
                    }
            }.onFailure {
                if (inferenceBitmap !== original) inferenceBitmap.recycle()
                original.recycle()
                finish(requestGeneration, emptyList(), onResult)
            }
        }
    }

    private fun finish(
        requestGeneration: Long,
        points: List<V2PosePoint>,
        onResult: (List<V2PosePoint>) -> Unit,
    ) {
        inFlight.set(false)
        if (closed || generation.get() != requestGeneration) return
        mainHandler.post {
            if (!closed && generation.get() == requestGeneration) onResult(points)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        generation.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        executor.execute {
            runCatching { retriever.release() }
            runCatching { detector.close() }
        }
        executor.shutdown()
    }

    private fun Bitmap.scaledForPoseDetection(): Bitmap {
        val largestSide = max(width, height)
        if (largestSide <= V2_POSE_MAX_INFERENCE_SIDE) return this
        val scale = V2_POSE_MAX_INFERENCE_SIDE.toFloat() / largestSide.toFloat()
        return scale(
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
        )
    }
}

@Composable
internal fun V2VideoPoseOverlay(
    points: List<V2PosePoint>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val mapped = points.associate { point ->
            point.type to Offset(point.x * size.width, point.y * size.height)
        }
        v2PoseConnections.forEach { connection ->
            val start = mapped[connection.startType] ?: return@forEach
            val end = mapped[connection.endType] ?: return@forEach
            drawLine(
                color = if (connection.isAlignmentGuide) Color(0xFF58FF42) else Color(0xFFFFFF3B),
                start = start,
                end = end,
                strokeWidth = if (connection.isAlignmentGuide) 4f else 5f,
                cap = StrokeCap.Round,
            )
        }
        mapped.values.forEach { point ->
            drawCircle(color = Color(0xFFFFD33D), radius = 5f, center = point)
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = 5f,
                center = point,
                style = Stroke(width = 1f),
            )
        }
    }
}
