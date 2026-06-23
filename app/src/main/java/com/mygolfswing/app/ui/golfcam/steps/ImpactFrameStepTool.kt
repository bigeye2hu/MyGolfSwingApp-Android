package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.ImpactFrameSelection
import kotlin.math.roundToInt

/**
 * 步骤 2：击球帧选择（对齐 iOS 设计稿 - 击球帧界面）。
 * 显示 距离开始 + 当前帧信息 + 帧条导航 + 提示文字。
 */
@Composable
fun ImpactFrameStepTool(
    player: ExoPlayer,
    durationMs: Long,
    state: GolfCamWorkflowState,
    onResult: (ImpactFrameSelection) -> Unit,
) {
    val startMs = state.trimResult?.startMs ?: 0L
    val endMs = if ((state.trimResult?.endMs ?: 0L) > 0L) state.trimResult!!.endMs else durationMs

    val initialSliderValue = if (durationMs > 0L) {
        val initial = state.impactFrame?.timeMs ?: ((startMs + endMs) / 2)
        initial.toFloat() / durationMs
    } else 0.5f

    var sliderValue by remember(durationMs) { mutableFloatStateOf(initialSliderValue) }

    LaunchedEffect(durationMs) {
        if (durationMs > 0L) {
            val timeMs = (sliderValue * durationMs).toLong().coerceIn(startMs, endMs.coerceAtLeast(1L))
            player.seekTo(timeMs)
            player.pause()
        }
    }

    LaunchedEffect(sliderValue, durationMs) {
        if (durationMs > 0L) {
            val timeMs = (sliderValue * durationMs).toLong()
                .coerceIn(startMs, endMs.coerceAtLeast(startMs + 1L))
            player.seekTo(timeMs)
            val fps = 30.0
            val frameIndex = (timeMs / 1000.0 * fps).roundToInt()
            val totalFrames = (durationMs / 1000.0 * fps).roundToInt()
            onResult(ImpactFrameSelection(frameIndex = frameIndex, timeMs = timeMs))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (durationMs > 0L) {
            val currentMs = (sliderValue * durationMs).toLong()
            val fps = 30.0
            val currentFrame = (currentMs / 1000.0 * fps).roundToInt()
            val totalFrames = (durationMs / 1000.0 * fps).roundToInt()
            val elapsedSec = (currentMs - startMs) / 1000.0

            // ── 帧信息行：距离开始 | 当前帧 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("距离开始", color = DSColors.shadowGray, fontSize = 10.sp)
                    Text(
                        "${"%.2f".format(elapsedSec)}s",
                        color = DSColors.inkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("当前帧", color = DSColors.shadowGray, fontSize = 10.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(DSColors.fairway, RoundedCornerShape(2.dp)),
                        )
                        Text(
                            "$currentFrame/$totalFrames",
                            color = DSColors.inkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ── 帧条导航（上一帧 / 滑块 / 下一帧）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 上一帧按钮
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DSColors.mistGreen)
                        .clickable {
                            val newFrame = (currentFrame - 1).coerceAtLeast(0)
                            sliderValue = (newFrame / fps / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "上一帧",
                        tint = DSColors.inkGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // 帧进度滑块（带 frameStrip 样式背景）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DSColors.mistGreen)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = DSColors.fairway,
                            activeTrackColor = DSColors.fairway,
                            inactiveTrackColor = DSColors.shadowGray.copy(alpha = 0.25f),
                        ),
                    )
                }

                // 下一帧按钮
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DSColors.mistGreen)
                        .clickable {
                            val newFrame = (currentFrame + 1).coerceAtMost(totalFrames)
                            sliderValue = (newFrame / fps / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "下一帧",
                        tint = DSColors.inkGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── 提示文字 ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.5.dp, DSColors.shadowGray, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(DSColors.shadowGray, CircleShape),
                    )
                }
                Text(
                    "滑动选择球杆击中球的瞬间，上方视频同步跳帧",
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = DSColors.fairway,
                    strokeWidth = 2.dp,
                )
                Text("正在加载视频...", color = DSColors.shadowGray, fontSize = 13.sp)
            }
        }
    }
}
