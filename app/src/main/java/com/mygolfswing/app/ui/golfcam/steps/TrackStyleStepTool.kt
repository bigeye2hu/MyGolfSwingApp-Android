package com.mygolfswing.app.ui.golfcam.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.model.TrajectoryPresetKind
import com.mygolfswing.app.ui.golfcam.model.defaultTrackColorPresets

// ── 样式类别 ──────────────────────────────────────────────────

private enum class StyleCategory(val label: String, val icon: ImageVector) {
    COLOR("颜色", Icons.Filled.Palette),
    WIDTH("宽度", Icons.Filled.LinearScale),
    SPEED("速度", Icons.Filled.Speed),
    TAIL("拖尾", Icons.Filled.Air),
    TRAJECTORY("弹道", Icons.Filled.ShowChart),
    DISTANCE("距离", Icons.Filled.SocialDistance),
}

/**
 * 步骤 6：轨迹样式调整（对齐 iOS TrackStyleStepTool）。
 * 颜色 / 宽度 / 速度 / 拖尾 / 弹道 / 距离 分类 Tab。
 */
@Composable
fun TrackStyleStepTool(state: GolfCamWorkflowState, onResult: (TrackStyleSelection) -> Unit) {
    val flightCurve = state.flightCurve ?: return
    val prev = state.trackStyle?.style
    var style by remember { mutableStateOf(prev ?: TrackStyle()) }
    var selectedCategory by remember { mutableStateOf(StyleCategory.COLOR) }

    LaunchedEffect(style) {
        onResult(TrackStyleSelection(style = style, flightCurve = flightCurve))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── 类别选择 Tab 行 ──
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(StyleCategory.entries) { category ->
                val isSelected = category == selectedCategory
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) DSColors.mistGreen else Color.Transparent,
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) DSColors.fairway.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        tint = if (isSelected) DSColors.fairway else DSColors.shadowGray,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        category.label,
                        color = if (isSelected) DSColors.inkGreen else DSColors.shadowGray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        HorizontalDivider(color = DSColors.mistGreen, thickness = 1.dp)

        // ── 内容区域 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            when (selectedCategory) {
                StyleCategory.COLOR -> ColorContent(style = style, onChange = { style = it })
                StyleCategory.WIDTH -> SliderContent(
                    label = "细  ←  轨迹宽度  →  粗",
                    value = style.widthPct.toFloat(),
                    range = 5f..160f,
                    displayText = "%.0f".format(style.widthPct),
                    onChange = { style = style.copy(widthPct = it.toDouble()) },
                )
                StyleCategory.SPEED -> SliderContent(
                    label = "慢  ←  轨迹速度  →  快",
                    value = style.speedLevel.toFloat(),
                    range = GolfCamSpeedConfig.MIN_LEVEL.toFloat()..GolfCamSpeedConfig.MAX_LEVEL.toFloat(),
                    displayText = GolfCamSpeedConfig.percentText(style.speedLevel),
                    onChange = { style = style.copy(speedLevel = it.toDouble()) },
                )
                StyleCategory.TAIL -> SliderContent(
                    label = "短  ←  拖尾长度  →  全程",
                    value = style.tailHalfLife.toFloat(),
                    range = 5f..100f,
                    displayText = "%.0f".format(style.tailHalfLife),
                    onChange = { style = style.copy(tailHalfLife = it.toDouble()) },
                )
                StyleCategory.TRAJECTORY -> TrajectoryPresetContent(style = style, onChange = { style = it })
                StyleCategory.DISTANCE -> DistanceContent(style = style, onChange = { style = it })
            }
        }
    }
}

@Composable
private fun TrajectoryPresetContent(style: TrackStyle, onChange: (TrackStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("弹道算法", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(
                style.trajectoryPreset.displayName,
                color = DSColors.inkGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TrajectoryPresetKind.entries) { preset ->
                val selected = preset == style.trajectoryPreset
                Column(
                    modifier = Modifier
                        .width(112.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) DSColors.fairway else DSColors.mistGreen.copy(alpha = 0.65f))
                        .border(
                            width = if (selected) 0.dp else 1.dp,
                            color = DSColors.fairway.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onChange(style.copy(trajectoryPreset = preset)) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        preset.displayName,
                        color = if (selected) Color.White else DSColors.inkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        trajectoryPresetSubtitle(preset),
                        color = if (selected) Color.White.copy(alpha = 0.8f) else DSColors.shadowGray,
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun trajectoryPresetSubtitle(preset: TrajectoryPresetKind): String {
    return when (preset) {
        TrajectoryPresetKind.STANDARD -> "均衡飞行节奏"
        TrajectoryPresetKind.HIGH_ARC -> "顶点更高更柔"
        TrajectoryPresetKind.LOW_PIERCING -> "低平穿透更快"
        TrajectoryPresetKind.SOFT_LANDING -> "短杆柔和落地"
        TrajectoryPresetKind.GOLF_CAM_LIKE -> "学习 GolfCam 节奏"
    }
}

// ── 颜色内容 ────────────────────────────────────────────────

@Composable
private fun ColorContent(style: TrackStyle, onChange: (TrackStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 不透明度滑块
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("不透明度", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text("${(style.colorAlpha * 100).toInt()}%", color = DSColors.inkGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = style.colorAlpha.toFloat(),
                onValueChange = { onChange(style.copy(colorAlpha = it.toDouble())) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth().height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = DSColors.fairway,
                    activeTrackColor = DSColors.fairway,
                    inactiveTrackColor = DSColors.fairway.copy(alpha = 0.25f),
                ),
            )
        }

        // 颜色预设圆点
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(defaultTrackColorPresets) { preset ->
                val selected = preset.name == style.colorPreset.name
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (selected) 44.dp else 40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(preset.gradientColors.map { Color(it) }))
                        .then(
                            if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        )
                        .clickable { onChange(style.copy(colorPreset = preset)) },
                ) {
                    if (selected) {
                        Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── 通用滑块内容 ─────────────────────────────────────────────

@Composable
private fun SliderContent(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayText: String,
    onChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(range.start.toInt().toString(), color = DSColors.shadowGray, fontSize = 10.sp)
            Text(displayText, color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(range.endInclusive.toInt().toString(), color = DSColors.shadowGray, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = DSColors.fairway,
                activeTrackColor = DSColors.fairway,
                inactiveTrackColor = DSColors.fairway.copy(alpha = 0.25f),
            ),
        )
    }
}

// ── 距离内容 ────────────────────────────────────────────────

@Composable
private fun DistanceContent(style: TrackStyle, onChange: (TrackStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("显示距离标注", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("在球头实时显示飞行距离", color = DSColors.shadowGray, fontSize = 11.sp)
            }
            Switch(
                checked = style.showDistance,
                onCheckedChange = { onChange(style.copy(showDistance = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DSColors.fairway,
                ),
            )
        }
        if (style.showDistance) {
            SliderContent(
                label = "距离 (码)",
                value = style.totalDistance.toFloat(),
                range = 10f..400f,
                displayText = "${style.totalDistance.toInt()}码",
                onChange = { onChange(style.copy(totalDistance = it.toDouble())) },
            )
        }
    }
}
