package com.mygolfswing.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 播放器只允许两种，后面沟通就用这两个名字。
 * 入口页面保留自己的标题、分享、删除；进度条、圆点和刻度颜色只从这里取。
 *
 * 回放播放器：按时间连续看完一段视频。倍速、快进快退、播放。
 * 用在视频 Memo、广场。
 *
 * 逐帧播放器：按帧看挥杆，拖进度时画面跟着帧走。
 * 用在挥杆详情、回看分析、教练分析。
 */
enum class VideoPlayerTool {
    /** 回放播放器。视频 Memo、广场。 */
    Playback,

    /** 逐帧播放器。挥杆详情、回看分析、教练分析。 */
    Frame,
}

object VideoPlayerChrome {
    val progressStart = Color(0xFF2E7D5B)
    val progressEnd = Color(0xFFE3C27D)
    val track = Color(0xFF2A2A2A)
    val knob = Color.White
    val keyTick = Color(0xFFA6CDB4)
    val keyTickSoft = Color(0xFFA6CDB4).copy(alpha = 0.72f)
    val normalTick = Color(0xFF7DA88F).copy(alpha = 0.5f)

    fun tickColor(heightRatio: Float): Color = when {
        heightRatio >= 0.9f -> keyTick
        heightRatio >= 20f / 34f -> keyTickSoft
        else -> normalTick
    }

    fun progressBrush(): Brush = Brush.horizontalGradient(listOf(progressStart, progressEnd))
}

@Composable
fun VideoPlayerProgressBar(
    tool: VideoPlayerTool,
    progress: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onScrubStart: () -> Unit = {},
    onScrubEnd: (Float) -> Unit = {},
    markerContent: @Composable BoxWithConstraintsScope.() -> Unit = {},
) {
    val clamped = progress.coerceIn(0f, 1f)
    val knobDiameter = if (tool == VideoPlayerTool.Frame) 17.dp else 16.dp
    val seekFraction = rememberUpdatedState(onSeekFraction)
    val scrubStart = rememberUpdatedState(onScrubStart)
    val scrubEnd = rememberUpdatedState(onScrubEnd)
    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                var fraction = down.position.x / size.width.toFloat().coerceAtLeast(1f)
                scrubStart.value()
                seekFraction.value(fraction.coerceIn(0f, 1f))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    change.consume()
                    fraction = change.position.x / size.width.toFloat().coerceAtLeast(1f)
                    seekFraction.value(fraction.coerceIn(0f, 1f))
                }
                scrubEnd.value(fraction.coerceIn(0f, 1f))
            }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        VideoPlayerProgressTrack(
            progress = clamped,
            knobDiameter = knobDiameter,
            markerContent = markerContent,
        )
    }
}

@Composable
fun VideoPlayerProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    knobDiameter: Dp = 16.dp,
    markerContent: @Composable BoxWithConstraintsScope.() -> Unit = {},
) {
    val clamped = progress.coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(VideoPlayerChrome.track),
        )
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(VideoPlayerChrome.progressBrush()),
        )
        markerContent()
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - knobDiameter) * clamped)
                .size(knobDiameter)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(VideoPlayerChrome.knob),
        )
    }
}
