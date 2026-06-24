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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamDistanceConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrackStyleCloudConfigLoader
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection

// ── 样式类别 ──────────────────────────────────────────────────

private enum class StyleCategory(val label: String, val icon: ImageVector) {
    COLOR("颜色", Icons.Filled.Palette),
    WIDTH("宽度", Icons.Filled.LinearScale),
    SPEED("速度", Icons.Filled.Speed),
    TAIL("拖尾", Icons.Filled.Air),
    DISTANCE("距离", Icons.Filled.SocialDistance),
    ;

    companion object {
        val userVisibleCases = listOf(COLOR, WIDTH, SPEED, TAIL, DISTANCE)
    }
}

/**
 * 步骤 6：轨迹样式调整（对齐 iOS TrackStyleStepTool）。
 * 颜色 / 宽度 / 速度 / 拖尾 / 距离 分类 Tab。
 */
@Composable
fun TrackStyleStepTool(state: GolfCamWorkflowState, onResult: (TrackStyleSelection) -> Unit) {
    val flightCurve = state.flightCurve ?: return
    val prev = state.trackStyle?.style
    val fallbackConfig = remember { TrackStyleCloudConfigLoader.localFallback() }
    var style by remember { mutableStateOf(prev ?: fallbackConfig.defaultStyle) }
    var colorPresets by remember { mutableStateOf(fallbackConfig.presets) }
    var selectedCategory by remember { mutableStateOf(StyleCategory.COLOR) }

    LaunchedEffect(Unit) {
        TrackStyleCloudConfigLoader.fetch()
            .onSuccess { config ->
                colorPresets = config.presets
                style = if (prev == null) {
                    config.defaultStyle
                } else {
                    val matchedPreset = config.presets.firstOrNull { it.name == style.colorPreset.name }
                    if (matchedPreset != null) {
                        style.copy(
                            colorPreset = matchedPreset,
                            colorHex = style.colorHex ?: matchedPreset.bandOuterColor(),
                            glowOpacity = matchedPreset.glowAlpha.toDouble(),
                        )
                    } else {
                        style
                    }
                }
            }
    }

    LaunchedEffect(style) {
        onResult(TrackStyleSelection(style = style, flightCurve = flightCurve))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── 类别选择 Tab 行 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            StyleCategory.userVisibleCases.forEach { category ->
                val isSelected = category == selectedCategory
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) DSColors.mistGreen.copy(alpha = 0.72f) else Color.Transparent,
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) DSColors.fairway.copy(alpha = 0.28f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
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
                StyleCategory.COLOR -> ColorContent(style = style, presets = colorPresets, onChange = { style = it })
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
                StyleCategory.DISTANCE -> DistanceContent(style = style, onChange = { style = it })
            }
        }
    }
}

// ── 颜色内容 ────────────────────────────────────────────────

@Composable
private fun ColorContent(
    style: TrackStyle,
    presets: List<com.mygolfswing.app.ui.golfcam.model.TrackColorPreset>,
    onChange: (TrackStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 不透明度滑块
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(style.colorPreset.gradientColors.map { Color(it) }))
                    .border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
        }

        // 颜色预设圆点
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(presets) { preset ->
                val selected = preset.name == style.colorPreset.name
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(if (selected) 0.dp else 2.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(preset.gradientColors.map { Color(it) }))
                        .then(
                            if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        )
                        .clickable {
                            onChange(
                                style.copy(
                                    colorPreset = preset,
                                    colorHex = preset.bandOuterColor(),
                                    glowOpacity = preset.glowAlpha.toDouble(),
                                ),
                            )
                        },
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.54f))
            .border(1.dp, DSColors.fairway.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
    var distanceText by remember(style.totalDistance) {
        mutableStateOf(GolfCamDistanceConfig.displayText(style.totalDistance))
    }
    fun commitDistance() {
        val value = distanceText.toDoubleOrNull() ?: style.totalDistance
        val clamped = GolfCamDistanceConfig.clamp(value)
        distanceText = GolfCamDistanceConfig.displayText(clamped)
        onChange(style.copy(totalDistance = clamped))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Switch(
                checked = style.showDistance,
                onCheckedChange = { enabled ->
                    onChange(
                        style.copy(
                            showDistance = enabled,
                            totalDistance = if (enabled && style.totalDistance <= 0.0) 150.0 else style.totalDistance,
                        ),
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DSColors.fairway,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = DSColors.shadowGray.copy(alpha = 0.38f),
                ),
            )
            Text(
                "显示距离标注",
                color = if (style.showDistance) DSColors.inkGreen else DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (style.showDistance) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, DSColors.fairway.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextField(
                        value = distanceText,
                        onValueChange = { text ->
                            val sanitized = GolfCamDistanceConfig.inputText(text)
                            distanceText = sanitized
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { commitDistance() }),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = DSColors.inkGreen,
                        ),
                        modifier = Modifier.width(92.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    Text(
                        style.distanceUnit.abbreviation,
                        color = DSColors.fairway,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(DSColors.fairway.copy(alpha = 0.12f))
                            .clickable { commitDistance() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                    }
                }

                UnitToggleButton(
                    unitLabel = style.distanceUnit.displayName,
                    nextUnitLabel = style.distanceUnit.toggle().displayName,
                    onClick = {
                        val currentDistance = GolfCamDistanceConfig.clamp(distanceText.toDoubleOrNull() ?: style.totalDistance)
                        val nextUnit = style.distanceUnit.toggle()
                        val converted = GolfCamDistanceConfig.convert(
                            distance = currentDistance,
                            from = style.distanceUnit,
                            to = nextUnit,
                        )
                        distanceText = GolfCamDistanceConfig.displayText(converted)
                        onChange(
                            style.copy(
                                distanceUnit = nextUnit,
                                totalDistance = converted,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun UnitToggleButton(
    unitLabel: String,
    nextUnitLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.72f))
            .border(1.dp, DSColors.fairway.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(unitLabel, color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text(nextUnitLabel, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
