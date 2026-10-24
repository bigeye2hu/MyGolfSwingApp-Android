package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import java.util.Locale

internal const val IMPACT_POSITION_TOOL_VERTICAL_SPACING_DP = 8
internal const val IMPACT_POSITION_COORD_LABEL_FONT_SP = 11
internal const val IMPACT_POSITION_COORD_VALUE_FONT_SP = 14
internal const val IMPACT_POSITION_TOOL_MARK_SIZE_DP = 16
internal const val IMPACT_POSITION_TOOL_MARK_SPACING_DP = 6
internal const val IMPACT_POSITION_TOOL_TITLE_FONT_SP = 13
internal const val IMPACT_POSITION_HINT_ICON_SIZE_DP = 14
internal const val IMPACT_POSITION_HINT_SPACING_DP = 8
internal const val IMPACT_POSITION_HINT_FONT_SP = 12
internal const val IMPACT_POSITION_STEP_TOOL_PANEL_HEIGHT_DP = 72

internal fun formatImpactPositionPercent(value: Float): String {
    val safeValue = value.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 50f
    return String.format(Locale.US, "%.1f%%", safeValue)
}

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 8.dp)
            .testTag("golfcam-impact-position-tool"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IMPACT_POSITION_TOOL_VERTICAL_SPACING_DP.dp),
    ) {
        // ── 坐标信息行：X | 击球点 | Y ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // X 坐标
            Column(horizontalAlignment = Alignment.Start) {
                Text("X", color = DSColors.shadowGray, fontSize = IMPACT_POSITION_COORD_LABEL_FONT_SP.sp)
                Text(
                    formatImpactPositionPercent(xPct),
                    color = DSColors.inkGreen,
                    fontSize = IMPACT_POSITION_COORD_VALUE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // 击球点标识
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IMPACT_POSITION_TOOL_MARK_SPACING_DP.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.impact_mark),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(DSColors.cardWhite),
                    modifier = Modifier.size(IMPACT_POSITION_TOOL_MARK_SIZE_DP.dp),
                )
                Text(
                    "击球点",
                    color = DSColors.inkGreen,
                    fontSize = IMPACT_POSITION_TOOL_TITLE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Y 坐标
            Column(horizontalAlignment = Alignment.End) {
                Text("Y", color = DSColors.shadowGray, fontSize = IMPACT_POSITION_COORD_LABEL_FONT_SP.sp)
                Text(
                    formatImpactPositionPercent(yPct),
                    color = DSColors.inkGreen,
                    fontSize = IMPACT_POSITION_COORD_VALUE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── 提示文字 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IMPACT_POSITION_HINT_SPACING_DP.dp),
        ) {
            Icon(
                Icons.Filled.TouchApp,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(IMPACT_POSITION_HINT_ICON_SIZE_DP.dp),
            )
            Text(
                "拖动或双击画面移动击球标记到球的位置",
                color = DSColors.shadowGray,
                fontSize = IMPACT_POSITION_HINT_FONT_SP.sp,
            )
        }
    }
}
