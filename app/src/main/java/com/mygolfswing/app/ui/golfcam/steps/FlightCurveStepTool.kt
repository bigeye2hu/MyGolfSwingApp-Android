package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * 步骤 5：飞行曲线调整（对齐 iOS FlightCurveStepTool）。
 * 显示 P1/P2 坐标 + 帧导航 + 提示文字，交互在上方视频画面进行。
 */
@Composable
fun FlightCurveStepTool(
    state: GolfCamWorkflowState,
    currentFrameIndex: Int = 0,
    totalFrames: Int = 0,
    onPrevFrame: (() -> Unit)? = null,
    onNextFrame: (() -> Unit)? = null,
) {
    val cp1 = state.flightCurveControlPoint1
    val cp2 = state.flightCurveControlPoint2
    val p1x = (cp1?.first ?: 0.3f) * 100f
    val p1y = (cp1?.second ?: 0.3f) * 100f
    val p2x = (cp2?.first ?: 0.7f) * 100f
    val p2y = (cp2?.second ?: 0.2f) * 100f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 信息行：帧信息 | P1 坐标 | P2 坐标 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 帧信息
            Column(horizontalAlignment = Alignment.Start) {
                if (totalFrames > 0) {
                    Text(
                        "帧 ${currentFrameIndex + 1}/$totalFrames",
                        color = DSColors.inkGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text("飞行曲线", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // P1 坐标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(DSColors.fairway),
                )
                Text("P1 ", color = DSColors.inkGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(
                    "(%.0f%%,%.0f%%)".format(p1x, p1y),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // P2 坐标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(DSColors.fairway),
                )
                Text("P2 ", color = DSColors.inkGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(
                    "(%.0f%%,%.0f%%)".format(p2x, p2y),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── 帧导航行：[上一帧] ...滑块区... [下一帧] ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 上一帧按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = currentFrameIndex > 0) { onPrevFrame?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    null,
                    tint = if (currentFrameIndex > 0) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "上一帧",
                    color = if (currentFrameIndex > 0) DSColors.shadowGray else DSColors.shadowGray.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                )
            }

            // 中间：提示
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.TouchApp, null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
                Text("拖动控制点调整球路弧度", color = DSColors.shadowGray, fontSize = 11.sp)
            }

            // 下一帧按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = totalFrames == 0 || currentFrameIndex < totalFrames - 1) { onNextFrame?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = if (totalFrames == 0 || currentFrameIndex < totalFrames - 1) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "下一帧",
                    color = if (totalFrames == 0 || currentFrameIndex < totalFrames - 1) DSColors.shadowGray else DSColors.shadowGray.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}
