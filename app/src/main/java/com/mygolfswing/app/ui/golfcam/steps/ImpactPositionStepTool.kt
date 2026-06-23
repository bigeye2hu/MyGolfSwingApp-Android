package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState

/**
 * 步骤 3：击球位置标注（对齐 iOS ImpactPositionStepTool）。
 * 显示 X/Y 坐标信息行 + 提示文字，交互在上方视频画面进行。
 */
@Composable
fun ImpactPositionStepTool(state: GolfCamWorkflowState = GolfCamWorkflowState()) {
    val pos = state.impactPosition
    val xPct = (pos?.normalizedX ?: 0.5f) * 100f
    val yPct = (pos?.normalizedY ?: 0.5f) * 100f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 坐标信息行：X | 击球点 | Y ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // X 坐标
            Column(horizontalAlignment = Alignment.Start) {
                Text("X", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.1f".format(xPct)}%",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // 击球点标识
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Filled.GpsFixed,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "击球点",
                    color = DSColors.inkGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Y 坐标
            Column(horizontalAlignment = Alignment.End) {
                Text("Y", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.1f".format(yPct)}%",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── 提示文字 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.TouchApp, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text(
                "拖动或点击画面移动击球标记到球的位置",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
            )
        }
    }
}
