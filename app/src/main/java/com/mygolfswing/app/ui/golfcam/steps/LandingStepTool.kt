package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.LandingOutput

/** 最短飞行时间（秒）*/
private const val MIN_FLIGHT_SEC = 0.5
/** 最长飞行时间（秒）*/
private const val MAX_FLIGHT_SEC = 8.0

/**
 * 步骤 4：落点选择（对齐 iOS LandingStepTool）。
 * 显示飞行时间 + 位置坐标 + 简化时间滑块 + 提示文字。
 * 落点位置交互在视频画面进行。
 */
@Composable
fun LandingStepTool(
    state: GolfCamWorkflowState = GolfCamWorkflowState(),
    onFlightTimeChange: ((Double) -> Unit)? = null,
) {
    val landing = state.landing
    val xPct = (landing?.normalizedX ?: 0.5f) * 100f
    val yPct = (landing?.normalizedY ?: 0.5f) * 100f
    val flightTime = landing?.flightTimeSec ?: 4.5

    // 本地 sliderValue 用于即时反馈，变化时同步给外部
    var sliderValue by remember(flightTime) { mutableDoubleStateOf(flightTime) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 信息行：飞行时间 | 落点 | 位置坐标 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 飞行时间
            Column(horizontalAlignment = Alignment.Start) {
                Text("飞行时间", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.1f".format(sliderValue)}s",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // 落点标识
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "落点",
                    color = DSColors.inkGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // 位置坐标
            Column(horizontalAlignment = Alignment.End) {
                Text("位置", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.0f".format(xPct)}%, ${"%.0f".format(yPct)}%",
                    color = DSColors.inkGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── 飞行时间滑块（对应 iOS 缩略图时间轴）──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DSColors.mistGreen)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 滑块
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { v ->
                        sliderValue = v.coerceIn(MIN_FLIGHT_SEC.toFloat(), MAX_FLIGHT_SEC.toFloat()).toDouble()
                        onFlightTimeChange?.invoke(sliderValue)
                    },
                    valueRange = MIN_FLIGHT_SEC.toFloat()..MAX_FLIGHT_SEC.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = DSColors.fairway,
                        activeTrackColor = DSColors.fairway,
                        inactiveTrackColor = DSColors.fairway.copy(alpha = 0.3f),
                    ),
                )
                // 时间标签
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0.5s", color = DSColors.shadowGray, fontSize = 10.sp)
                    Text("4.0s", color = DSColors.shadowGray, fontSize = 10.sp)
                    Text("8.0s", color = DSColors.shadowGray, fontSize = 10.sp)
                }
            }
        }

        // ── 提示文字 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text(
                "滑动选择落点时间，拖动画面中的落点标记",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
            )
        }
    }
}
