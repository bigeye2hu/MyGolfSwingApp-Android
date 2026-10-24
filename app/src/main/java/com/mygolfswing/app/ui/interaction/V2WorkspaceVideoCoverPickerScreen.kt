package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2WorkspaceVideoCoverPickerScreen(
    prepared: V2PreparedWorkspaceVideo,
    saving: Boolean,
    error: String?,
    onSkip: () -> Unit,
    onConfirm: (Double) -> Unit,
    frameLoader: suspend (V2PreparedWorkspaceVideo, Double) -> File = ::previewWorkspaceVideoCover,
) {
    val duration = prepared.durationSeconds?.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    var selectedSeconds by rememberSaveable(prepared.file.absolutePath) {
        mutableDoubleStateOf(
            normalizedWorkspaceCoverTimeSeconds(duration, prepared.coverTimeSeconds),
        )
    }
    var preview by remember(prepared.file.absolutePath) { mutableStateOf<File?>(null) }
    var previewLoading by remember(prepared.file.absolutePath) { mutableStateOf(true) }
    var previewError by remember(prepared.file.absolutePath) { mutableStateOf<String?>(null) }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = DSColors.fairway,
        activeTrackColor = DSColors.fairway,
        inactiveTrackColor = DSColors.shadowGray.copy(alpha = 0.24f),
    )

    V2BackHandler(enabled = !saving, onBack = onSkip)

    LaunchedEffect(prepared.file.absolutePath, selectedSeconds) {
        delay(90)
        previewLoading = true
        previewError = null
        try {
            val generated = frameLoader(prepared, selectedSeconds)
            preview?.takeIf { it != prepared.cover }?.delete()
            preview = generated
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            preview?.takeIf { it != prepared.cover }?.delete()
            preview = null
            previewError = "无法读取这一帧，请稍微移动时间点"
        } finally {
            previewLoading = false
        }
    }

    DisposableEffect(prepared.file.absolutePath) {
        onDispose { preview?.takeIf { it != prepared.cover }?.delete() }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().v2FullScreenInsets()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSkip, enabled = !saving) { Text("取消", color = DSColors.fairway) }
            Spacer(Modifier.weight(1f))
            Text("选择视频封面", color = DSColors.deepGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(52.dp))
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                AsyncImage(
                    model = preview,
                    contentDescription = "所选视频封面",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (previewLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("正在读取画面…", color = Color.White, fontSize = 12.sp)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        previewError ?: "这个时间点暂时无法生成封面",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("拖到最能说明动作的位置", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(formatCoverTime(selectedSeconds), color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = selectedSeconds.toFloat(),
                onValueChange = { selectedSeconds = quantizedCoverPickerTime(it.toDouble(), duration) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(0.1f),
                enabled = !saving,
                colors = sliderColors,
                interactionSource = sliderInteractionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = sliderInteractionSource,
                        colors = sliderColors,
                        thumbSize = DpSize(20.dp, 20.dp),
                    )
                },
                track = { sliderState ->
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        val centerY = size.height / 2f
                        val strokeWidth = size.height
                        drawLine(
                            color = DSColors.shadowGray.copy(alpha = 0.24f),
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = DSColors.fairway,
                            start = Offset(0f, centerY),
                            end = Offset(size.width * sliderState.coercedValueAsFraction, centerY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                },
            )
            Row(Modifier.fillMaxWidth()) {
                Text("0:00", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    formatCoverTime(duration),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("选择封面", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "从视频中选择一个画面作为封面",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            androidx.compose.material3.Icon(
                Icons.Filled.GppGood,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "只会改变列表和对话中的封面，不会裁剪或修改原视频",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        error?.let { Text(it, color = DSColors.flagRed, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
            Button(
                onClick = { onConfirm(selectedSeconds) },
                enabled = !saving && preview != null && !previewLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("确认封面", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun coverPickerStep(duration: Double): Double {
    val safeDuration = duration.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    return maxOf(0.04, safeDuration / 300.0)
}

internal fun quantizedCoverPickerTime(seconds: Double, duration: Double): Double {
    val safeDuration = duration.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    val step = coverPickerStep(safeDuration)
    val safeSeconds = seconds.takeIf(Double::isFinite)?.coerceIn(0.0, safeDuration) ?: 0.0
    return ((safeSeconds / step).roundToInt() * step).coerceIn(0.0, safeDuration)
}

internal fun formatCoverTime(seconds: Double): String {
    val value = seconds.takeIf(Double::isFinite)?.coerceAtLeast(0.0)?.roundToInt() ?: 0
    return String.format(Locale.CHINA, "%d:%02d", value / 60, value % 60)
}
