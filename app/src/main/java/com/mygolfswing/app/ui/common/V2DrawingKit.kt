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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
    Angle,
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

/** 对齐 iOS `DrawingViewModel` */
class V2DrawingState {
    var currentTool by mutableStateOf(V2DrawingTool.Freehand)
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

    private fun bumpRevision() {
        revision++
    }

    fun begin(point: Offset) {
        editDrag = null
        inProgress = when (currentTool) {
            V2DrawingTool.Freehand -> V2DrawingStroke(currentTool, points = listOf(point), color = currentColor)
            V2DrawingTool.Line -> V2DrawingStroke(currentTool, start = point, end = point, color = currentColor)
            V2DrawingTool.Angle, V2DrawingTool.Select -> null
        }
    }

    fun move(point: Offset) {
        val current = inProgress ?: return
        inProgress = when (current.tool) {
            V2DrawingTool.Freehand -> current.copy(points = current.points + point)
            V2DrawingTool.Line -> current.copy(end = point)
            V2DrawingTool.Angle, V2DrawingTool.Select -> current
        }
    }

    fun end() {
        val stroke = inProgress ?: return
        strokes.add(stroke)
        inProgress = null
        undoStack.clear()
        bumpRevision()
        if (stroke.tool == V2DrawingTool.Line) {
            selectedStrokeId = stroke.id
            currentTool = V2DrawingTool.Select
        }
    }

    fun selectStroke(point: Offset): Boolean {
        val hit = V2DrawingHitTest.hitTestLines(point, strokes) ?: return false
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

    fun beginEdit(point: Offset): Boolean {
        if (currentTool != V2DrawingTool.Select && selectedStrokeId == null) return false
        val hit = V2DrawingHitTest.hitTestLines(point, strokes) ?: run {
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
        if (stroke.tool != V2DrawingTool.Line) return

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

val V2DrawingColors = listOf(
    Color(0xFFA666F2),
    Color(0xFFE91E63),
    Color(0xFFFF9800),
    Color(0xFF80E699),
    Color(0xFF80CCFF),
)

@Composable
fun V2DrawingToolsPanel(
    state: V2DrawingState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2DrawingToolButton(Icons.Filled.Draw, selected = state.currentTool == V2DrawingTool.Freehand) {
            state.currentTool = V2DrawingTool.Freehand
        }
        V2DrawingToolButton(Icons.Filled.HorizontalRule, selected = state.currentTool == V2DrawingTool.Line) {
            state.currentTool = V2DrawingTool.Line
        }
        V2DrawingToolButton(Icons.Filled.PanTool, selected = state.currentTool == V2DrawingTool.Select) {
            state.currentTool = V2DrawingTool.Select
        }
        V2DrawingToolButton(Icons.Filled.ChangeHistory, selected = state.currentTool == V2DrawingTool.Angle) {
            state.currentTool = V2DrawingTool.Angle
        }
        V2DrawingColors.forEach { color ->
            V2DrawingColorButton(color = color, selected = state.currentColor == color) {
                state.currentColor = color
            }
        }
        if (state.hasSelection) {
            V2DrawingToolButton(Icons.Filled.Delete, tint = Color(0xFFFF5252)) {
                state.deleteSelected()
            }
        }
        V2DrawingToolButton(Icons.Filled.DeleteSweep) { state.clearAll() }
        V2DrawingToolButton(Icons.AutoMirrored.Filled.Undo) { state.undo() }
        V2DrawingToolButton(Icons.Filled.Close, onClick = onClose)
    }
}

@Composable
fun V2DrawingOverlay(
    state: V2DrawingState,
    modifier: Modifier = Modifier,
) {
    val revision = state.revision
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.currentTool, state.selectedStrokeId) {
                detectTapGestures { point ->
                    when (state.currentTool) {
                        V2DrawingTool.Select -> {
                            if (!state.selectStroke(point)) {
                                state.clearSelection()
                            }
                        }
                        V2DrawingTool.Angle -> state.handleAngleTap(point)
                        V2DrawingTool.Freehand -> Unit
                        V2DrawingTool.Line -> Unit
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
                            V2DrawingTool.Select -> state.beginEdit(point)
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
                                    if (!state.selectStroke(endPoint)) {
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
            },
    ) {
        fun drawStroke(stroke: V2DrawingStroke, alpha: Float = 1f, selected: Boolean = false) {
            val lineWidth = if (selected) stroke.lineWidth + 1.5f else stroke.lineWidth
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
                V2DrawingTool.Line, V2DrawingTool.Angle -> {
                    val segStart = stroke.start
                    val segEnd = stroke.end
                    if (segStart != null && segEnd != null) {
                        drawLine(stroke.color.copy(alpha = alpha), segStart, segEnd, lineWidth, StrokeCap.Round)
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
                        val radius = 40f
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
                            style = Stroke(width = 3f, cap = StrokeCap.Round),
                        )
                        val midRad = Math.toRadians(((startDeg + endDeg) / 2f + 180f).toDouble())
                        val textX = vertex.x + 42f * cos(midRad).toFloat()
                        val textY = vertex.y + 42f * sin(midRad).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            "${stroke.angle!!.roundToInt()}°",
                            textX,
                            textY,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textSize = 36f
                                typeface = Typeface.DEFAULT_BOLD
                                setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
                            },
                        )
                    }
                }
                V2DrawingTool.Select -> Unit
            }
        }

        state.strokes.forEach { stroke ->
            val selected = stroke.id == state.selectedStrokeId && stroke.tool == V2DrawingTool.Line
            drawStroke(stroke, selected = selected)
        }
        state.inProgress?.let { drawStroke(it, alpha = 0.9f) }

        if (state.currentTool == V2DrawingTool.Angle) {
            state.measuredPointA?.let { drawCircle(Color.Yellow, 4f, it) }
            state.measuredPointB?.let { drawCircle(Color.Red, 5f, it) }
        }

        val selected = state.selectedStroke
        if (selected != null && selected.tool == V2DrawingTool.Line) {
            val start = selected.start
            val end = selected.end
            if (start != null && end != null) {
                val handleRadius = 13f
                drawCircle(Color.White, handleRadius, start)
                drawCircle(selected.color, handleRadius - 4f, start, style = Stroke(width = 3f))
                drawCircle(Color.White, handleRadius, end)
                drawCircle(selected.color, handleRadius - 4f, end, style = Stroke(width = 3f))
            }
        }
    }
}

@Composable
private fun V2DrawingToolButton(
    icon: ImageVector,
    selected: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(tint.copy(alpha = 0.2f))
                        .border(2.dp, tint, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun V2DrawingColorButton(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(color.copy(alpha = 0.2f))
                        .border(2.dp, color, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}

private object V2DrawingHitTest {
    private const val ENDPOINT_RADIUS = 42f
    private const val LINE_RADIUS = 22f

    fun hitTestLines(point: Offset, strokes: List<V2DrawingStroke>): LineHitResult? {
        var best: LineHitResult? = null
        for (stroke in strokes) {
            if (stroke.tool != V2DrawingTool.Line) continue
            val start = stroke.start ?: continue
            val end = stroke.end ?: continue

            val dStart = distance(point, start)
            if (dStart <= ENDPOINT_RADIUS) {
                val hit = LineHitResult(stroke.id, LineEditHandle.Start, dStart)
                if (best == null || hit.distance < best!!.distance) best = hit
            }

            val dEnd = distance(point, end)
            if (dEnd <= ENDPOINT_RADIUS) {
                val hit = LineHitResult(stroke.id, LineEditHandle.End, dEnd)
                if (best == null || hit.distance < best!!.distance) best = hit
            }

            val (dSeg, t) = distanceToSegment(point, start, end)
            if (dSeg <= LINE_RADIUS && t > 0.02f && t < 0.98f) {
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
