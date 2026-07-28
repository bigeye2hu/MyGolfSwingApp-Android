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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.draw.shadow
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
import com.mygolfswing.app.ui.golfcam.model.trackStyleAfterCloudConfigLoaded
import java.util.Locale

internal const val TRACK_STYLE_CATEGORY_HEIGHT_DP = 36
internal const val TRACK_STYLE_CONTENT_HEIGHT_DP = 108
internal const val TRACK_STYLE_CATEGORY_HORIZONTAL_PADDING_DP = 12
internal const val TRACK_STYLE_CATEGORY_TOP_PADDING_DP = 2
internal const val TRACK_STYLE_CATEGORY_TAB_HEIGHT_DP = 32
internal const val TRACK_STYLE_CATEGORY_ICON_SIZE_DP = 13
internal const val TRACK_STYLE_CATEGORY_LABEL_FONT_SP = 10
internal const val TRACK_STYLE_COLOR_PICKER_SIZE_DP = 34
internal const val TRACK_STYLE_COLOR_ROW_SPACING_DP = 12
internal const val TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP = 16
internal const val TRACK_STYLE_COLOR_PRESET_OUTER_DP = 44
internal const val TRACK_STYLE_COLOR_PRESET_SWATCH_DP = 38
internal const val TRACK_STYLE_COLOR_PRESET_SPACING_DP = 10
internal const val TRACK_STYLE_COLOR_PRESET_SELECTED_STROKE_DP = 2
internal const val TRACK_STYLE_COLOR_PRESET_CHECK_SIZE_DP = 11
internal const val TRACK_STYLE_SLIDER_VERTICAL_PADDING_DP = 14
internal const val TRACK_STYLE_SLIDER_LABEL_FONT_SP = 11
internal const val TRACK_STYLE_DISTANCE_VERTICAL_SPACING_DP = 10
internal const val TRACK_STYLE_DISTANCE_INPUT_FONT_SP = 28
internal const val TRACK_STYLE_DISTANCE_INPUT_WIDTH_DP = 80
internal const val TRACK_STYLE_DISTANCE_INPUT_CORNER_RADIUS_DP = 12
internal const val TRACK_STYLE_DISTANCE_INPUT_HORIZONTAL_PADDING_DP = 14
internal const val TRACK_STYLE_DISTANCE_INPUT_VERTICAL_PADDING_DP = 8
internal const val TRACK_STYLE_DISTANCE_UNIT_FONT_SP = 14
internal const val TRACK_STYLE_DISTANCE_CONFIRM_ICON_SIZE_DP = 20
internal const val TRACK_STYLE_DISTANCE_UNIT_TOGGLE_SIZE_DP = 56
internal const val TRACK_STYLE_DISTANCE_UNIT_TOGGLE_CORNER_RADIUS_DP = 12
internal const val TRACK_STYLE_DISTANCE_UNIT_TOGGLE_LABEL_FONT_SP = 13
internal const val TRACK_STYLE_DISTANCE_UNIT_TOGGLE_ICON_SIZE_DP = 10
private val STYLE_CATEGORY_HEIGHT = TRACK_STYLE_CATEGORY_HEIGHT_DP.dp
private val STYLE_CONTENT_HEIGHT = TRACK_STYLE_CONTENT_HEIGHT_DP.dp

internal fun formatTrackStyleInteger(value: Double): String {
    val safeValue = value.takeIf { it.isFinite() } ?: 0.0
    return String.format(Locale.US, "%.0f", safeValue)
}

internal fun formatTrackStyleOpacityPercent(alpha: Double): String {
    val safeAlpha = alpha.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.75
    return "${(safeAlpha * 100).toInt()}%"
}

internal fun trackStyleSliderValue(
    value: Double,
    min: Float,
    max: Float,
    fallback: Float,
): Float {
    val safeMin = min.takeIf { it.isFinite() } ?: 0f
    val safeMax = max.takeIf { it.isFinite() && it >= safeMin } ?: safeMin
    val safeFallback = fallback.takeIf { it.isFinite() }?.coerceIn(safeMin, safeMax) ?: safeMin
    return value.takeIf { it.isFinite() }
        ?.toFloat()
        ?.coerceIn(safeMin, safeMax)
        ?: safeFallback
}

internal data class TrackStyleDistanceEditResult(
    val style: TrackStyle,
    val displayText: String,
)

internal fun commitTrackStyleDistanceInput(
    style: TrackStyle,
    rawText: String,
): TrackStyleDistanceEditResult {
    val value = rawText.toDoubleOrNull() ?: style.totalDistance
    val clamped = GolfCamDistanceConfig.clamp(value)
    return TrackStyleDistanceEditResult(
        style = style.copy(totalDistance = clamped),
        displayText = GolfCamDistanceConfig.displayText(clamped),
    )
}

internal fun toggleTrackStyleDistanceUnit(
    style: TrackStyle,
    rawText: String,
): TrackStyleDistanceEditResult {
    val currentDistance = GolfCamDistanceConfig.clamp(rawText.toDoubleOrNull() ?: style.totalDistance)
    val nextUnit = style.distanceUnit.toggle()
    val converted = GolfCamDistanceConfig.convert(
        distance = currentDistance,
        from = style.distanceUnit,
        to = nextUnit,
    )
    return TrackStyleDistanceEditResult(
        style = style.copy(
            distanceUnit = nextUnit,
            totalDistance = converted,
        ),
        displayText = GolfCamDistanceConfig.displayText(converted),
    )
}

internal fun trackStyleDistanceVisibilityChange(
    style: TrackStyle,
    enabled: Boolean,
): TrackStyle {
    val currentDistance = style.totalDistance.takeIf { it.isFinite() }
    val distance = if (enabled && (currentDistance == null || currentDistance <= 0.0)) {
        GolfCamDistanceConfig.DEFAULT_DISTANCE
    } else {
        GolfCamDistanceConfig.clamp(style.totalDistance)
    }
    return style.copy(
        showDistance = enabled,
        totalDistance = distance,
    )
}

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

internal fun visibleTrackStyleCategoryLabels(): List<String> {
    return StyleCategory.userVisibleCases.map { it.label }
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
                style = trackStyleAfterCloudConfigLoaded(
                    currentStyle = style,
                    initialFallbackStyle = fallbackConfig.defaultStyle,
                    restoredStyle = prev,
                    config = config,
                )
            }
    }

    LaunchedEffect(style, flightCurve) {
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
                .height(STYLE_CATEGORY_HEIGHT)
                .padding(horizontal = TRACK_STYLE_CATEGORY_HORIZONTAL_PADDING_DP.dp)
                .padding(top = TRACK_STYLE_CATEGORY_TOP_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StyleCategory.userVisibleCases.forEach { category ->
                val isSelected = category == selectedCategory
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(TRACK_STYLE_CATEGORY_TAB_HEIGHT_DP.dp)
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
                        modifier = Modifier.size(TRACK_STYLE_CATEGORY_ICON_SIZE_DP.dp),
                    )
                    Text(
                        category.label,
                        color = if (isSelected) DSColors.inkGreen else DSColors.shadowGray,
                        fontSize = TRACK_STYLE_CATEGORY_LABEL_FONT_SP.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }

        // ── 内容区域 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(STYLE_CONTENT_HEIGHT)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (selectedCategory) {
                StyleCategory.COLOR -> ColorContent(style = style, presets = colorPresets, onChange = { style = it })
                StyleCategory.WIDTH -> SliderContent(
                    label = "细  ←  轨迹宽度  →  粗",
                    value = trackStyleSliderValue(style.widthPct, 5f, 160f, 40f),
                    range = 5f..160f,
                    onChange = { style = style.copy(widthPct = it.toDouble()) },
                )
                StyleCategory.SPEED -> SliderContent(
                    label = "慢  ←  轨迹速度  →  快",
                    value = trackStyleSliderValue(
                        style.speedLevel,
                        GolfCamSpeedConfig.MIN_LEVEL.toFloat(),
                        GolfCamSpeedConfig.MAX_LEVEL.toFloat(),
                        GolfCamSpeedConfig.DEFAULT_LEVEL.toFloat(),
                    ),
                    range = GolfCamSpeedConfig.MIN_LEVEL.toFloat()..GolfCamSpeedConfig.MAX_LEVEL.toFloat(),
                    onChange = { style = style.copy(speedLevel = it.toDouble()) },
                )
                StyleCategory.TAIL -> SliderContent(
                    label = "短  ←  拖尾长度  →  全程",
                    value = trackStyleSliderValue(style.tailHalfLife, 5f, 100f, 70f),
                    range = 5f..100f,
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
    val opacityValue = trackStyleSliderValue(style.colorAlpha, 0f, 1f, 0.75f)
    Column(
        modifier = Modifier.padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 不透明度滑块
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(TRACK_STYLE_COLOR_ROW_SPACING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(TRACK_STYLE_COLOR_PICKER_SIZE_DP.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color(style.colorPreset.bandOuterColor()).copy(alpha = 0.16f),
                        spotColor = Color(style.colorPreset.bandOuterColor()).copy(alpha = 0.24f),
                    )
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
                    Text(formatTrackStyleOpacityPercent(style.colorAlpha), color = DSColors.inkGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Slider(
                    value = opacityValue,
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
            modifier = Modifier.padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(TRACK_STYLE_COLOR_PRESET_SPACING_DP.dp),
        ) {
            items(presets) { preset ->
                val selected = preset.name == style.colorPreset.name
                val glowColor = Color(preset.bandOuterColor())
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(TRACK_STYLE_COLOR_PRESET_OUTER_DP.dp)
                        .shadow(
                            elevation = if (selected) 7.dp else 3.dp,
                            shape = CircleShape,
                            ambientColor = glowColor.copy(alpha = if (selected) 0.34f else 0.12f),
                            spotColor = glowColor.copy(alpha = if (selected) 0.52f else 0.20f),
                        )
                        .border(
                            width = if (selected) TRACK_STYLE_COLOR_PRESET_SELECTED_STROKE_DP.dp else 0.dp,
                            color = Color.White.copy(alpha = if (selected) 0.90f else 0f),
                            shape = CircleShape,
                        )
                        .padding((TRACK_STYLE_COLOR_PRESET_OUTER_DP - TRACK_STYLE_COLOR_PRESET_SWATCH_DP).dp / 2)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(preset.gradientColors.map { Color(it) }))
                        .border(1.dp, Color.White.copy(alpha = if (selected) 0.18f else 0.10f), CircleShape)
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
                            modifier = Modifier.size(TRACK_STYLE_COLOR_PRESET_CHECK_SIZE_DP.dp),
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
    onChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TRACK_STYLE_SLIDER_VERTICAL_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = DSColors.shadowGray, fontSize = TRACK_STYLE_SLIDER_LABEL_FONT_SP.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
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
        val result = commitTrackStyleDistanceInput(style, distanceText)
        distanceText = result.displayText
        onChange(result.style)
    }

    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TRACK_STYLE_DISTANCE_VERTICAL_SPACING_DP.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Switch(
                checked = style.showDistance,
                onCheckedChange = { enabled ->
                    onChange(trackStyleDistanceVisibilityChange(style, enabled))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(TRACK_STYLE_DISTANCE_INPUT_CORNER_RADIUS_DP.dp))
                    .background(DSColors.mistGreen.copy(alpha = if (style.showDistance) 0.55f else 0.28f))
                    .padding(
                        horizontal = TRACK_STYLE_DISTANCE_INPUT_HORIZONTAL_PADDING_DP.dp,
                        vertical = TRACK_STYLE_DISTANCE_INPUT_VERTICAL_PADDING_DP.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TextField(
                    value = distanceText,
                    onValueChange = { text ->
                        distanceText = GolfCamDistanceConfig.inputText(text)
                    },
                    enabled = style.showDistance,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitDistance() }),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = TRACK_STYLE_DISTANCE_INPUT_FONT_SP.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (style.showDistance) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.40f),
                    ),
                    modifier = Modifier.width(TRACK_STYLE_DISTANCE_INPUT_WIDTH_DP.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                Text(
                    style.distanceUnit.abbreviation,
                    color = DSColors.fairway.copy(alpha = if (style.showDistance) 1.0f else 0.35f),
                    fontSize = TRACK_STYLE_DISTANCE_UNIT_FONT_SP.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = DSColors.fairway.copy(alpha = if (style.showDistance) 1f else 0.35f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(TRACK_STYLE_DISTANCE_CONFIRM_ICON_SIZE_DP.dp)
                        .clickable(enabled = style.showDistance) { commitDistance() },
                )
            }

            UnitToggleButton(
                enabled = style.showDistance,
                unitLabel = style.distanceUnit.displayName,
                nextUnitLabel = style.distanceUnit.toggle().displayName,
                onClick = {
                    val result = toggleTrackStyleDistanceUnit(style, distanceText)
                    distanceText = result.displayText
                    onChange(result.style)
                },
            )
        }
    }
}

@Composable
private fun UnitToggleButton(
    enabled: Boolean,
    unitLabel: String,
    nextUnitLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(TRACK_STYLE_DISTANCE_UNIT_TOGGLE_SIZE_DP.dp)
            .clip(RoundedCornerShape(TRACK_STYLE_DISTANCE_UNIT_TOGGLE_CORNER_RADIUS_DP.dp))
            .background(DSColors.mistGreen.copy(alpha = if (enabled) 0.55f else 0.28f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(unitLabel, color = DSColors.inkGreen.copy(alpha = if (enabled) 1f else 0.35f), fontSize = TRACK_STYLE_DISTANCE_UNIT_TOGGLE_LABEL_FONT_SP.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = DSColors.fairway.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(TRACK_STYLE_DISTANCE_UNIT_TOGGLE_ICON_SIZE_DP.dp))
            Text(nextUnitLabel, color = DSColors.shadowGray.copy(alpha = if (enabled) 1f else 0.42f), fontSize = TRACK_STYLE_DISTANCE_UNIT_TOGGLE_LABEL_FONT_SP.sp, fontWeight = FontWeight.Bold)
        }
    }
}
