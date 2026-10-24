package com.mygolfswing.app.ui.common

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.UUID

/** 对齐 iOS `DrawingKit.swift` */
enum class V2DrawingTool {
    Freehand,
    Line,
    Arrow,
    Angle,
    Circle,
    Rectangle,
    Select,
}

internal enum class LineEditHandle { Start, End, Body }

private data class LineHitResult(val strokeId: String, val handle: LineEditHandle, val distance: Float)

internal data class EditDragState(
    val strokeId: String,
    val handle: LineEditHandle,
    val dragStart: Offset,
    val originalStart: Offset,
    val originalEnd: Offset,
)

data class V2DrawingStroke(
    val tool: V2DrawingTool,
    val points: List<Offset> = emptyList(),
    val start: Offset? = null,
    val end: Offset? = null,
    val vertex: Offset? = null,
    val angle: Double? = null,
    val color: Color,
    val lineWidth: Float = 3f,
    val id: String = UUID.randomUUID().toString(),
)

internal data class V2ArrowHeadPoints(
    val firstWing: Offset,
    val secondWing: Offset,
)

internal fun v2ArrowHeadPoints(
    start: Offset,
    end: Offset,
    headLength: Float = 22f,
    wingRadians: Double = Math.PI / 7.0,
): V2ArrowHeadPoints {
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    return V2ArrowHeadPoints(
        firstWing = Offset(
            x = end.x - headLength * cos(angle - wingRadians).toFloat(),
            y = end.y - headLength * sin(angle - wingRadians).toFloat(),
        ),
        secondWing = Offset(
            x = end.x - headLength * cos(angle + wingRadians).toFloat(),
            y = end.y - headLength * sin(angle + wingRadians).toFloat(),
        ),
    )
}

/** 对齐 iOS `DrawingViewModel` */
class V2DrawingState {
    private var currentToolState by mutableStateOf(V2DrawingTool.Line)
    var currentTool: V2DrawingTool
        get() = currentToolState
        set(value) {
            if (currentToolState == value) return
            val previous = currentToolState
            currentToolState = value
            inProgress = null
            editDrag = null
            if (previous == V2DrawingTool.Angle && value != V2DrawingTool.Angle) {
                cancelIncompleteAngleMeasurement()
            }
            if (value != V2DrawingTool.Select) selectedStrokeId = null
            bumpRevision()
        }
    var currentColor by mutableStateOf(V2DrawingColors.first())
    val strokes: SnapshotStateList<V2DrawingStroke> = mutableStateListOf()
    var inProgress by mutableStateOf<V2DrawingStroke?>(null)
    var selectedStrokeId by mutableStateOf<String?>(null)
    internal var editDrag by mutableStateOf<EditDragState?>(null)
    var revision by mutableIntStateOf(0)
        private set

    private val undoStack = mutableListOf<V2DrawingStroke>()
    private var measuredA: Offset? = null
    private var measuredB: Offset? = null

    val hasSelection: Boolean get() = selectedStrokeId != null

    val selectedStroke: V2DrawingStroke?
        get() = selectedStrokeId?.let { id -> strokes.firstOrNull { it.id == id } }

    val measuredPointA: Offset? get() = measuredA
    val measuredPointB: Offset? get() = measuredB
    val angleHintText: String
        get() = when {
            measuredA == null -> "点选第 1 点：杆身或身体线起点"
            measuredB == null -> "点选第 2 点：角度顶点"
            else -> "点选第 3 点：另一条参考线"
        }

    private fun bumpRevision() {
        revision++
    }

    private fun cancelIncompleteAngleMeasurement() {
        val first = measuredA
        val second = measuredB
        if (first != null && second != null) {
            val last = strokes.lastOrNull()
            if (last?.tool == V2DrawingTool.Angle && last.angle == null && last.start == first && last.end == second) {
                strokes.removeLastOrNull()
            }
        }
        measuredA = null
        measuredB = null
    }

    fun begin(point: Offset) {
        editDrag = null
        inProgress = when (currentTool) {
            V2DrawingTool.Freehand -> V2DrawingStroke(currentTool, points = listOf(point), color = currentColor)
            V2DrawingTool.Line, V2DrawingTool.Arrow, V2DrawingTool.Circle, V2DrawingTool.Rectangle ->
                V2DrawingStroke(currentTool, start = point, end = point, color = currentColor)
            V2DrawingTool.Angle, V2DrawingTool.Select -> null
        }
    }

    fun move(point: Offset) {
        val current = inProgress ?: return
        inProgress = when (current.tool) {
            V2DrawingTool.Freehand -> current.copy(points = current.points + point)
            V2DrawingTool.Line, V2DrawingTool.Arrow, V2DrawingTool.Circle, V2DrawingTool.Rectangle -> current.copy(end = point)
            V2DrawingTool.Angle, V2DrawingTool.Select -> current
        }
    }

    fun end() {
        val stroke = inProgress ?: return
        strokes.add(stroke)
        inProgress = null
        undoStack.clear()
        val start = stroke.start
        val endPoint = stroke.end
        val longEnough = start != null && endPoint != null && (endPoint - start).getDistance() >= 8f
        if (stroke.tool == V2DrawingTool.Line && longEnough) {
            selectedStrokeId = stroke.id
            currentTool = V2DrawingTool.Select
        }
        bumpRevision()
    }

    fun selectStroke(point: Offset, densityScale: Float = 1f): Boolean {
        val hit = V2DrawingHitTest.hitTestLines(point, strokes, densityScale) ?: return false
        selectedStrokeId = hit.strokeId
        bumpRevision()
        return true
    }

    fun clearSelection() {
        selectedStrokeId = null
        editDrag = null
        bumpRevision()
    }

    fun deleteSelected() {
        val id = selectedStrokeId ?: return
        strokes.removeAll { it.id == id }
        selectedStrokeId = null
        editDrag = null
        bumpRevision()
    }

    fun beginEdit(point: Offset, densityScale: Float = 1f): Boolean {
        if (currentTool != V2DrawingTool.Select && selectedStrokeId == null) return false
        val hit = V2DrawingHitTest.hitTestLines(point, strokes, densityScale) ?: run {
            if (currentTool == V2DrawingTool.Select) clearSelection()
            return false
        }
        selectedStrokeId = hit.strokeId
        val stroke = strokes.firstOrNull { it.id == hit.strokeId } ?: return false
        val start = stroke.start ?: return false
        val end = stroke.end ?: return false
        editDrag = EditDragState(
            strokeId = hit.strokeId,
            handle = hit.handle,
            dragStart = point,
            originalStart = start,
            originalEnd = end,
        )
        bumpRevision()
        return true
    }

    fun continueEdit(point: Offset) {
        val drag = editDrag ?: return
        val index = strokes.indexOfFirst { it.id == drag.strokeId }
        if (index < 0) return
        val stroke = strokes[index]
        if (stroke.tool != V2DrawingTool.Line && stroke.tool != V2DrawingTool.Arrow) return

        val updated = when (drag.handle) {
            LineEditHandle.Start -> stroke.copy(start = point)
            LineEditHandle.End -> stroke.copy(end = point)
            LineEditHandle.Body -> {
                val dx = point.x - drag.dragStart.x
                val dy = point.y - drag.dragStart.y
                stroke.copy(
                    start = Offset(drag.originalStart.x + dx, drag.originalStart.y + dy),
                    end = Offset(drag.originalEnd.x + dx, drag.originalEnd.y + dy),
                )
            }
        }
        strokes[index] = updated
        bumpRevision()
    }

    fun endEdit() {
        editDrag = null
        bumpRevision()
    }

    fun handleAngleTap(point: Offset) {
        val first = measuredA
        val second = measuredB
        when {
            first == null -> {
                measuredA = point
                bumpRevision()
            }
            second == null -> {
                measuredB = point
                strokes.add(V2DrawingStroke(V2DrawingTool.Angle, start = first, end = point, color = currentColor))
                bumpRevision()
            }
            else -> {
                strokes.add(V2DrawingStroke(V2DrawingTool.Angle, start = second, end = point, color = currentColor))
                strokes.add(
                    V2DrawingStroke(
                        V2DrawingTool.Angle,
                        start = second,
                        end = first,
                        vertex = point,
                        angle = angleAbc(first, second, point),
                        color = currentColor,
                    ),
                )
                measuredA = null
                measuredB = null
                bumpRevision()
            }
        }
    }

    fun undo() {
        val removed = strokes.removeLastOrNull() ?: return
        if (removed.id == selectedStrokeId) selectedStrokeId = null
        editDrag = null
        undoStack.add(removed)
        bumpRevision()
    }

    fun clearAll() {
        strokes.clear()
        undoStack.clear()
        inProgress = null
        selectedStrokeId = null
        editDrag = null
        measuredA = null
        measuredB = null
        bumpRevision()
    }
}

internal val V2SelectionArrowIcon: ImageVector = ImageVector.Builder(
    name = "V2SelectionArrow",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(5.8f, 3f)
        lineTo(5.8f, 19.2f)
        lineTo(9.9f, 15.1f)
        lineTo(13.6f, 21f)
        lineTo(16.2f, 19.5f)
        lineTo(12.5f, 13.6f)
        lineTo(18.2f, 13.6f)
        close()
    }
}.build()

internal fun v2SelectionToolTint(active: Boolean, color: Color): Color =
    if (active) color else Color(0xFF8E8E93)

val V2DrawingColors = listOf(
    Color(0xFFFF4D00),
    Color(0xFFFFE23B),
    Color(0xFF55E65A),
    Color(0xFF58D5E8),
    Color(0xFF3BA7FF),
    Color.White,
)

@Composable
fun V2DrawingToolsPanel(
    state: V2DrawingState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showSelectTool: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var showMoreTools by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val hasDrawings = state.strokes.isNotEmpty() || state.inProgress != null

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showColorPicker) {
            V2DrawingColorPalette(
                selectedColor = state.currentColor,
                onSelect = { color ->
                    state.currentColor = color
                    showColorPicker = false
                },
                modifier = Modifier.padding(top = 106.dp),
            )
        }

        Column(
            modifier = Modifier
                .width(52.dp)
                .heightIn(max = if (expanded) 560.dp else 340.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.64f))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            V2DrawingToolButton(Icons.Filled.Close, "关闭绘图工具") {
                showColorPicker = false
                showMoreTools = false
                onClose()
            }
            V2DrawingToolButton(
                Icons.Filled.HorizontalRule,
                "杆身或目标线",
                selected = state.currentTool == V2DrawingTool.Line,
                tint = state.currentColor,
            ) { state.currentTool = V2DrawingTool.Line }
            V2DrawingToolButton(
                Icons.Filled.Draw,
                "挥杆轨迹曲线",
                selected = state.currentTool == V2DrawingTool.Freehand,
                tint = state.currentColor,
            ) { state.currentTool = V2DrawingTool.Freehand }
            V2DrawingColorSelector(
                color = state.currentColor,
                selected = showColorPicker,
                onClick = { showColorPicker = !showColorPicker },
            )

            if (expanded) {
                V2DrawingToolButton(
                    Icons.Filled.SouthWest,
                    "挥杆方向箭头",
                    selected = state.currentTool == V2DrawingTool.Arrow,
                    tint = state.currentColor,
                ) { state.currentTool = V2DrawingTool.Arrow }
                V2DrawingToolButton(
                    Icons.Filled.ChangeHistory,
                    "身体和杆面角度",
                    selected = state.currentTool == V2DrawingTool.Angle,
                    tint = V2DrawingColors[1],
                ) { state.currentTool = V2DrawingTool.Angle }
                if (showSelectTool) {
                    V2DrawingToolButton(
                        V2SelectionArrowIcon,
                        "选择和调整标记",
                        selected = state.currentTool == V2DrawingTool.Select,
                        tint = v2SelectionToolTint(
                            state.currentTool == V2DrawingTool.Select,
                            state.currentColor,
                        ),
                    ) { state.currentTool = V2DrawingTool.Select }
                }
                V2DrawingToolButton(
                    Icons.Filled.MoreHoriz,
                    if (showMoreTools) "收起更多标注工具" else "展开更多标注工具",
                    selected = showMoreTools,
                    tint = if (showMoreTools) state.currentColor else Color.White,
                ) { showMoreTools = !showMoreTools }

                if (showMoreTools) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        V2DrawingToolButton(
                            Icons.Outlined.Circle,
                            "圈出重点区域",
                            selected = state.currentTool == V2DrawingTool.Circle,
                            tint = state.currentColor,
                        ) { state.currentTool = V2DrawingTool.Circle }
                        V2DrawingToolButton(
                            Icons.Outlined.CropSquare,
                            "框选身体区域",
                            selected = state.currentTool == V2DrawingTool.Rectangle,
                            tint = state.currentColor,
                        ) { state.currentTool = V2DrawingTool.Rectangle }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.width(30.dp),
                    color = Color.White.copy(alpha = 0.18f),
                )
                if (state.hasSelection) {
                    V2DrawingToolButton(Icons.Filled.Delete, "删除选中标记", tint = Color(0xFFFF6B6B)) {
                        state.deleteSelected()
                    }
                }
                V2DrawingToolButton(
                    Icons.AutoMirrored.Filled.Undo,
                    "撤销上一笔",
                    enabled = hasDrawings,
                ) { state.undo() }
                V2DrawingToolButton(
                    Icons.Filled.LayersClear,
                    "清空所有标记",
                    enabled = hasDrawings,
                ) { showClearConfirm = true }
                V2DrawingToolButton(Icons.Filled.ExpandLess, "收起绘图工具") {
                    expanded = false
                    showMoreTools = false
                    showColorPicker = false
                }
            } else {
                V2DrawingToolButton(Icons.Filled.ExpandMore, "展开绘图工具") {
                    expanded = true
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空所有标记？") },
            confirmButton = {
                TextButton(onClick = {
                    state.clearAll()
                    showClearConfirm = false
                }) { Text("清空", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            },
        )
    }
}

private val V2DrawingColorLabels = listOf("橙色", "黄色", "绿色", "青色", "蓝色", "白色")

@Composable
private fun V2DrawingColorPalette(
    selectedColor: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        V2DrawingColors.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { color ->
                    val index = V2DrawingColors.indexOf(color)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (color == selectedColor) 2.4.dp else 1.dp,
                                Color.White.copy(alpha = if (color == selectedColor) 1f else 0.22f),
                                CircleShape,
                            )
                            .clickable { onSelect(color) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = "${V2DrawingColorLabels[index]}标注",
                            tint = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V2DrawingColorSelector(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) color.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) color else Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape),
        )
        Icon(Icons.Filled.Palette, contentDescription = "选择标注颜色", tint = Color.Transparent, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun V2DrawingQuickActions(
    state: V2DrawingState,
    modifier: Modifier = Modifier,
) {
    var confirmingClear by remember { mutableStateOf(false) }
    if (state.strokes.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val selectActive = state.currentTool == V2DrawingTool.Select
            V2DrawingQuickActionButton(
                V2SelectionArrowIcon,
                "箭头",
                v2SelectionToolTint(selectActive, state.currentColor),
            ) {
                confirmingClear = false
                state.currentTool = V2DrawingTool.Select
            }
            V2DrawingQuickActionButton(Icons.Filled.LayersClear, "清空所有标记", Color(0xFFFF8A80)) {
                confirmingClear = !confirmingClear
            }
            V2DrawingQuickActionButton(Icons.AutoMirrored.Filled.Undo, "撤销上一笔标记", Color.White) {
                confirmingClear = false
                state.undo()
            }
        }
        if (confirmingClear) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "清空",
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        confirmingClear = false
                        state.clearAll()
                    },
                )
                Text(
                    "取消",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { confirmingClear = false },
                )
            }
        }
    }
}

@Composable
private fun V2DrawingQuickActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun V2DrawingOverlay(
    state: V2DrawingState,
    modifier: Modifier = Modifier,
    interactionEnabled: Boolean = true,
) {
    val revision = state.revision
    val densityScale = LocalDensity.current.density
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!interactionEnabled) {
                    Modifier
                } else {
                    Modifier
                        .pointerInput(state.currentTool, state.selectedStrokeId) {
                            detectTapGestures { point ->
                                when (state.currentTool) {
                                    V2DrawingTool.Select -> {
                                        if (!state.selectStroke(point, densityScale)) {
                                            state.clearSelection()
                                        }
                                    }
                                    V2DrawingTool.Angle -> state.handleAngleTap(point)
                                    V2DrawingTool.Freehand -> Unit
                                    V2DrawingTool.Line, V2DrawingTool.Arrow, V2DrawingTool.Circle, V2DrawingTool.Rectangle -> Unit
                                }
                            }
                        }
                        .pointerInput(state.currentTool, state.selectedStrokeId) {
                            var gestureStart: Offset? = null
                            var lastPosition = Offset.Zero
                            detectDragGestures(
                                onDragStart = { point ->
                                    gestureStart = point
                                    lastPosition = point
                                    if (state.editDrag != null) return@detectDragGestures
                                    when (state.currentTool) {
                                        V2DrawingTool.Select -> state.beginEdit(point, densityScale)
                                        V2DrawingTool.Angle -> Unit
                                        else -> if (state.inProgress == null) state.begin(point)
                                    }
                                },
                                onDrag = { change, _ ->
                                    lastPosition = change.position
                                    if (state.editDrag != null) {
                                        state.continueEdit(change.position)
                                        return@detectDragGestures
                                    }
                                    when (state.currentTool) {
                                        V2DrawingTool.Select -> Unit
                                        V2DrawingTool.Angle -> Unit
                                        else -> state.move(change.position)
                                    }
                                },
                                onDragEnd = {
                                    val endPoint = lastPosition
                                    val startPoint = gestureStart
                                    val distance = if (startPoint != null) {
                                        hypot(
                                            (endPoint.x - startPoint.x).toDouble(),
                                            (endPoint.y - startPoint.y).toDouble(),
                                        ).toFloat()
                                    } else {
                                        0f
                                    }

                                    if (state.editDrag != null) {
                                        state.endEdit()
                                    } else when (state.currentTool) {
                                        V2DrawingTool.Select -> {
                                            if (distance < 8f) {
                                                if (!state.selectStroke(endPoint, densityScale)) {
                                                    state.clearSelection()
                                                }
                                            }
                                        }
                                        V2DrawingTool.Angle -> {
                                            if (distance < 8f) state.handleAngleTap(endPoint)
                                        }
                                        else -> state.end()
                                    }
                                    gestureStart = null
                                },
                                onDragCancel = {
                                    if (state.editDrag != null) state.endEdit()
                                    gestureStart = null
                                },
                            )
                        }
                },
            ),
    ) {
        fun drawStroke(stroke: V2DrawingStroke, alpha: Float = 1f, selected: Boolean = false) {
            val lineWidth = (if (selected) stroke.lineWidth + 1.5f else stroke.lineWidth) * densityScale
            val strokeStyle = Stroke(width = lineWidth, cap = StrokeCap.Round)
            when (stroke.tool) {
                V2DrawingTool.Freehand -> {
                    val points = stroke.points
                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, stroke.color.copy(alpha = alpha), style = strokeStyle)
                    }
                }
                V2DrawingTool.Line, V2DrawingTool.Arrow, V2DrawingTool.Angle -> {
                    val segStart = stroke.start
                    val segEnd = stroke.end
                    if (segStart != null && segEnd != null) {
                        drawLine(stroke.color.copy(alpha = alpha), segStart, segEnd, lineWidth, StrokeCap.Round)
                        if (stroke.tool == V2DrawingTool.Arrow) {
                            val head = v2ArrowHeadPoints(segStart, segEnd, headLength = 22f * densityScale)
                            drawLine(stroke.color.copy(alpha = alpha), head.firstWing, segEnd, lineWidth, StrokeCap.Round)
                            drawLine(stroke.color.copy(alpha = alpha), segEnd, head.secondWing, lineWidth, StrokeCap.Round)
                        }
                    }
                    if (stroke.tool == V2DrawingTool.Angle &&
                        stroke.angle != null &&
                        stroke.vertex != null &&
                        segStart != null &&
                        segEnd != null
                    ) {
                        val vertex = segStart
                        val a = segEnd
                        val c = stroke.vertex
                        val radius = 40f * densityScale
                        val startDeg = angleDegrees(vertex, a)
                        val endDeg = angleDegrees(vertex, c)
                        var sweep = endDeg - startDeg
                        while (sweep <= -180f) sweep += 360f
                        while (sweep > 180f) sweep -= 360f
                        drawArc(
                            color = stroke.color,
                            startAngle = startDeg,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = vertex - Offset(radius, radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 3f * densityScale, cap = StrokeCap.Round),
                        )
                        val midRad = Math.toRadians(((startDeg + endDeg) / 2f + 180f).toDouble())
                        val textX = vertex.x + 42f * densityScale * cos(midRad).toFloat()
                        val textY = vertex.y + 42f * densityScale * sin(midRad).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            "${stroke.angle!!.roundToInt()}°",
                            textX,
                            textY,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textSize = 18.sp.toPx()
                                typeface = Typeface.DEFAULT_BOLD
                                setShadowLayer(4f * densityScale, 0f, densityScale, android.graphics.Color.BLACK)
                            },
                        )
                    }
                }
                V2DrawingTool.Circle, V2DrawingTool.Rectangle -> {
                    val start = stroke.start
                    val end = stroke.end
                    if (start != null && end != null) {
                        val topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y))
                        val size = Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y))
                        if (stroke.tool == V2DrawingTool.Circle) {
                            drawOval(
                                color = stroke.color.copy(alpha = alpha),
                                topLeft = topLeft,
                                size = size,
                                style = strokeStyle,
                            )
                        } else {
                            drawRect(
                                color = stroke.color.copy(alpha = alpha),
                                topLeft = topLeft,
                                size = size,
                                style = strokeStyle,
                            )
                        }
                    }
                }
                V2DrawingTool.Select -> Unit
            }
        }

        state.strokes.forEach { stroke ->
            val selected = stroke.id == state.selectedStrokeId &&
                (stroke.tool == V2DrawingTool.Line || stroke.tool == V2DrawingTool.Arrow)
            drawStroke(stroke, selected = selected)
        }
        state.inProgress?.let { drawStroke(it, alpha = 0.9f) }

        if (state.currentTool == V2DrawingTool.Angle) {
            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 13.sp.toPx()
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val hintHeight = 31f * densityScale
            val hintWidth = hintPaint.measureText(state.angleHintText) + 24f * densityScale
            val hintLeft = (size.width - hintWidth) / 2f
            val hintTop = 18f * densityScale
            val hintCorner = hintHeight / 2f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.68f),
                topLeft = Offset(hintLeft, hintTop),
                size = Size(hintWidth, hintHeight),
                cornerRadius = CornerRadius(hintCorner, hintCorner),
            )
            drawRoundRect(
                color = V2DrawingColors[1].copy(alpha = 0.75f),
                topLeft = Offset(hintLeft, hintTop),
                size = Size(hintWidth, hintHeight),
                cornerRadius = CornerRadius(hintCorner, hintCorner),
                style = Stroke(width = densityScale),
            )
            val hintBaseline = hintTop + hintHeight / 2f - (hintPaint.ascent() + hintPaint.descent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(state.angleHintText, size.width / 2f, hintBaseline, hintPaint)

            fun drawAngleMarker(label: String, point: Offset, color: Color) {
                val radius = 12f * densityScale
                drawCircle(Color.Black.copy(alpha = 0.62f), radius, point)
                drawCircle(color, radius, point, style = Stroke(width = 2f * densityScale))
                val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 11.sp.toPx()
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val baseline = point.y - (markerPaint.ascent() + markerPaint.descent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(label, point.x, baseline, markerPaint)
            }
            state.measuredPointA?.let { drawAngleMarker("A", it, V2DrawingColors[1]) }
            state.measuredPointB?.let { drawAngleMarker("B", it, Color(0xFFFC4C02)) }
        }

        val selected = state.selectedStroke
        if (selected != null && (selected.tool == V2DrawingTool.Line || selected.tool == V2DrawingTool.Arrow)) {
            val start = selected.start
            val end = selected.end
            if (start != null && end != null) {
                val handleRadius = 11f * densityScale
                drawCircle(Color.White, handleRadius, start)
                drawCircle(selected.color, handleRadius, start, style = Stroke(width = 3f * densityScale))
                drawCircle(Color.White, handleRadius, end)
                drawCircle(selected.color, handleRadius, end, style = Stroke(width = 3f * densityScale))
                val midpoint = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                drawCircle(Color.Black.copy(alpha = 0.62f), 9f * densityScale, midpoint)
                drawCircle(selected.color.copy(alpha = 0.9f), 9f * densityScale, midpoint, style = Stroke(width = 2f * densityScale))
                val cross = 4f * densityScale
                drawLine(Color.White.copy(alpha = 0.92f), midpoint - Offset(cross, 0f), midpoint + Offset(cross, 0f), densityScale, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.92f), midpoint - Offset(0f, cross), midpoint + Offset(0f, cross), densityScale, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun V2DrawingToolButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    tint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) tint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f))
            .border(
                if (selected) 1.6.dp else 1.dp,
                if (selected) tint else Color.White.copy(alpha = 0.08f),
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private object V2DrawingHitTest {
    private const val ENDPOINT_RADIUS = 26f
    private const val LINE_RADIUS = 18f

    fun hitTestLines(point: Offset, strokes: List<V2DrawingStroke>, densityScale: Float = 1f): LineHitResult? {
        var best: LineHitResult? = null
        for (stroke in strokes) {
            if (stroke.tool != V2DrawingTool.Line && stroke.tool != V2DrawingTool.Arrow) continue
            val start = stroke.start ?: continue
            val end = stroke.end ?: continue

            val dStart = distance(point, start)
            if (dStart <= ENDPOINT_RADIUS * densityScale) {
                val hit = LineHitResult(stroke.id, LineEditHandle.Start, dStart)
                if (best == null || hit.distance < best!!.distance) best = hit
            }

            val dEnd = distance(point, end)
            if (dEnd <= ENDPOINT_RADIUS * densityScale) {
                val hit = LineHitResult(stroke.id, LineEditHandle.End, dEnd)
                if (best == null || hit.distance < best!!.distance) best = hit
            }

            val (dSeg, t) = distanceToSegment(point, start, end)
            if (dSeg <= LINE_RADIUS * densityScale && t > 0.02f && t < 0.98f) {
                val hit = LineHitResult(stroke.id, LineEditHandle.Body, dSeg)
                if (best == null || hit.distance < best!!.distance) best = hit
            }
        }
        return best
    }

    private fun distance(a: Offset, b: Offset): Float =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun distanceToSegment(point: Offset, segStart: Offset, segEnd: Offset): Pair<Float, Float> {
        val dx = segEnd.x - segStart.x
        val dy = segEnd.y - segStart.y
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-4f) return distance(point, segStart) to 0f
        var t = ((point.x - segStart.x) * dx + (point.y - segStart.y) * dy) / len2
        t = t.coerceIn(0f, 1f)
        val proj = Offset(segStart.x + t * dx, segStart.y + t * dy)
        return distance(point, proj) to t
    }
}

private fun angleDegrees(vertex: Offset, point: Offset): Float =
    Math.toDegrees(atan2((point.y - vertex.y).toDouble(), (point.x - vertex.x).toDouble())).toFloat()

private fun angleAbc(a: Offset, b: Offset, c: Offset): Double {
    val ba = Offset(a.x - b.x, a.y - b.y)
    val bc = Offset(c.x - b.x, c.y - b.y)
    val dot = ba.x * bc.x + ba.y * bc.y
    val magBA = sqrt(ba.x * ba.x + ba.y * ba.y)
    val magBC = sqrt(bc.x * bc.x + bc.y * bc.y)
    if (magBA <= 0f || magBC <= 0f) return 0.0
    return Math.toDegrees(acos((dot / (magBA * magBC)).coerceIn(-1f, 1f).toDouble()))
}
