package com.mygolfswing.app.ui.memo

import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.UUID

/**
 * 视频 Memo 播放器（对齐 iOS `V2VideoMemoPlayerView`）。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2VideoMemoPlayerScreen(
    memoId: UUID,
    onBack: () -> Unit = {},
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val memo = memos.firstOrNull { it.id == memoId }
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var showToolsPanel by remember { mutableStateOf(false) }
    var showMoreTools by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var currentRateIndex by remember { mutableIntStateOf(0) }
    val playbackRates = remember { listOf(1f, 0.5f, 0.25f) }
    val drawingState = remember { DrawingState() }

    if (memo == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onBack, title = "视频 Memo")
        return
    }

    val playbackUrl = remember(memo.videoUrl) { resolvePlaybackUrl(memo.videoUrl) }
    if (playbackUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onBack,
            onRetry = null,
        )
        return
    }

    val player = remember(playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isPlaying = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    player.play()
                    currentTime = 0f
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            currentTime = (player.currentPosition / 1000f).coerceAtLeast(0f)
            val durMs = player.duration
            if (durMs > 0) duration = durMs / 1000f
            isPlaying = player.isPlaying
            delay(100)
        }
    }

    fun showCenterIcon() {
        showPlayPauseIcon = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
        )

        if (playbackError) {
            VideoPlaybackUnavailableOverlay(
                state = V2PlaybackState.UnavailablePlaybackError,
                onBack = onBack,
                onRetry = {
                    playbackError = false
                    player.setMediaItem(MediaItem.fromUri(playbackUrl))
                    player.prepare()
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    player.play()
                    isPlaying = true
                },
            )
        }

        if (showToolsPanel) {
            DrawingOverlay(
                state = drawingState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 84.dp, end = 92.dp, bottom = 180.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !showToolsPanel) {
                    if (showMoreTools) {
                        showMoreTools = false
                    } else if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                        showCenterIcon()
                    } else {
                        player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                        player.play()
                        isPlaying = true
                        showCenterIcon()
                    }
                },
        )

        TopBar(
            showToolsPanel = showToolsPanel,
            onBack = onBack,
            onToggleDrawing = { showToolsPanel = !showToolsPanel },
        )

        if (!showToolsPanel && !playbackError) {
            RightFloatingTools(
                speedText = playbackRates[currentRateIndex].speedText(),
                isSlow = currentRateIndex != 0,
                showMoreTools = showMoreTools,
                onSpeed = {
                    currentRateIndex = (currentRateIndex + 1) % playbackRates.size
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    if (player.isPlaying) player.play()
                },
                onToggleMore = { showMoreTools = !showMoreTools },
                onDelete = { showDeleteConfirm = true },
            )
        } else {
            DrawingToolsPanel(
                state = drawingState,
                onClose = { showToolsPanel = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 56.dp),
            )
        }

        if (!playbackError) BottomInfoAndProgress(
            title = memo.displayTitle ?: memo.title ?: "无标题",
            description = memo.text.orEmpty(),
            sourceLabel = memo.trainingMethod?.takeIf { it.isNotBlank() } ?: "视频备忘录",
            currentTime = currentTime,
            duration = duration,
            onSeek = { seconds ->
                player.seekTo((seconds * 1000).toLong())
                currentTime = seconds
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )

        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(56.dp),
                )
            }
            LaunchedEffect(showPlayPauseIcon) {
                if (showPlayPauseIcon) {
                    delay(800)
                    showPlayPauseIcon = false
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除备忘录") },
            text = { Text("确定删除这个视频备忘录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        V2MemoStore.delete(memo.id)
                        onBack()
                    },
                ) {
                    Text("删除", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

private enum class V2PlaybackState {
    UnavailableNoSource,
    UnavailablePlaybackError,
}

@Composable
private fun VideoPlaybackUnavailableOverlay(
    state: V2PlaybackState,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1F2937).copy(alpha = 0.96f))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (state == V2PlaybackState.UnavailableNoSource) "视频不在本机" else "视频暂时无法加载",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (state == V2PlaybackState.UnavailableNoSource) {
                    "你可能正在另一台设备查看这条记录。\n视频源文件只保存在录制它的设备上，跨设备播放暂未支持。\n标题、笔记与标签已同步。"
                } else {
                    "可能是网络问题，或这条视频源已不可用。"
                },
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            if (onRetry != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DSColors.fairway)
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("重试", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (onRetry == null) DSColors.fairway else Color.Transparent)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("返回", color = Color.White.copy(alpha = if (onRetry == null) 1f else 0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TopBar(showToolsPanel: Boolean, onBack: () -> Unit, onToggleDrawing: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, contentDescription = "关闭", onClick = onBack)
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.inkGreen)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
            Text("视频Memo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = "画线工具",
            tint = if (showToolsPanel) DSColors.goldBright else Color.White,
            onClick = onToggleDrawing,
        )
    }
}

@Composable
private fun RightFloatingTools(
    speedText: String,
    isSlow: Boolean,
    showMoreTools: Boolean,
    onSpeed: () -> Unit,
    onToggleMore: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 12.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        FloatingTextButton(primary = speedText, secondary = "速度", highlighted = isSlow, onClick = onSpeed)
        AnimatedVisibility(
            visible = showMoreTools,
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut() + slideOutHorizontally { it / 2 },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                FloatingIconButton(icon = Icons.Filled.DeleteOutline, label = "删除", onClick = onDelete)
            }
        }
        FloatingIconButton(
            icon = if (showMoreTools) Icons.Filled.Close else Icons.Filled.MoreHoriz,
            label = if (showMoreTools) "收起" else "更多",
            onClick = onToggleMore,
        )
    }
}

@Composable
private fun BottomInfoAndProgress(
    title: String,
    description: String,
    sourceLabel: String,
    currentTime: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                .padding(start = 16.dp, end = 86.dp, top = 20.dp, bottom = 6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sourceLabel, color = DSColors.goldBright, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (description.isNotBlank()) {
                    Text(description, color = Color(0xFF9CA3AF), fontSize = 12.sp, maxLines = 2)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp),
        ) {
            Row {
                Text(formatTime(currentTime), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Slider(
                value = currentTime.coerceIn(0f, duration.coerceAtLeast(0f)),
                onValueChange = onSeek,
                valueRange = 0f..duration.coerceAtLeast(0.1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3B82F6),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun DrawingToolsPanel(state: DrawingState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolButton("自由", selected = state.currentTool == DrawingTool.Freehand) { state.currentTool = DrawingTool.Freehand }
        ToolButton("直线", selected = state.currentTool == DrawingTool.Line) { state.currentTool = DrawingTool.Line }
        ToolButton("选择", selected = state.currentTool == DrawingTool.Select) { state.currentTool = DrawingTool.Select }
        ToolButton("角度", selected = state.currentTool == DrawingTool.Angle) { state.currentTool = DrawingTool.Angle }
        drawingColors.forEach { color ->
            ColorToolButton(color = color, selected = state.currentColor == color) { state.currentColor = color }
        }
        if (state.hasSelection) {
            ToolButton("删除", tint = Color(0xFFFF5252)) { state.deleteSelected() }
        }
        ToolButton("清空") { state.clearAll() }
        ToolButton("撤销") { state.undo() }
        ToolButton("关闭", onClick = onClose)
    }
}

@Composable
private fun DrawingOverlay(state: DrawingState, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.pointerInput(state.currentTool, state.selectedStrokeId, state.strokes.size) {
            var gestureStart: Offset? = null
            var lastPosition = Offset.Zero
            detectDragGestures(
                onDragStart = { point ->
                    gestureStart = point
                    lastPosition = point
                    when (state.currentTool) {
                        DrawingTool.Angle -> state.handleAngleTap(point)
                        DrawingTool.Select -> state.beginEdit(point)
                        else -> state.begin(point)
                    }
                },
                onDrag = { change, _ ->
                    lastPosition = change.position
                    if (state.editDrag != null) {
                        state.continueEdit(change.position)
                    } else if (state.currentTool != DrawingTool.Angle && state.currentTool != DrawingTool.Select) {
                        state.move(change.position)
                    }
                },
                onDragEnd = {
                    if (state.editDrag != null) {
                        state.endEdit()
                    } else if (state.currentTool == DrawingTool.Select) {
                        val start = gestureStart
                        if (start != null) {
                            val dist = kotlin.math.hypot(
                                (lastPosition.x - start.x).toDouble(),
                                (lastPosition.y - start.y).toDouble(),
                            ).toFloat()
                            if (dist < 8f) {
                                if (!state.selectStroke(lastPosition)) {
                                    state.clearSelection()
                                }
                            }
                        }
                    } else if (state.currentTool != DrawingTool.Angle) {
                        state.end()
                    }
                    gestureStart = null
                },
            )
        },
    ) {
        fun drawStroke(stroke: DrawingStroke, alpha: Float = 1f, selected: Boolean = false) {
            val lineWidth = if (selected) stroke.lineWidth + 1.5f else stroke.lineWidth
            val strokeStyle = Stroke(width = lineWidth, cap = StrokeCap.Round)
            when (stroke.tool) {
                DrawingTool.Freehand -> {
                    val points = stroke.points
                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, stroke.color.copy(alpha = alpha), style = strokeStyle)
                    }
                }
                DrawingTool.Line, DrawingTool.Angle -> {
                    val start = stroke.start
                    val end = stroke.end
                    if (start != null && end != null) {
                        drawLine(stroke.color.copy(alpha = alpha), start, end, lineWidth, StrokeCap.Round)
                    }
                    if (stroke.tool == DrawingTool.Angle && stroke.angle != null && stroke.vertex != null && start != null && end != null) {
                        val radius = 40f
                        val startAngle = Math.toDegrees(atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble()))
                        val endAngle = Math.toDegrees(
                            atan2(
                                (stroke.vertex.y - start.y).toDouble(),
                                (stroke.vertex.x - start.x).toDouble(),
                            ),
                        )
                        val midAngle = Math.toRadians((startAngle + endAngle) / 2.0 + 180.0)
                        val textX = start.x + 42f * cos(midAngle).toFloat()
                        val textY = start.y + 42f * sin(midAngle).toFloat()
                        drawArc(
                            color = stroke.color,
                            startAngle = startAngle.toFloat(),
                            sweepAngle = stroke.angle.toFloat(),
                            useCenter = false,
                            topLeft = start - Offset(radius, radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 3f, cap = StrokeCap.Round),
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${stroke.angle.roundToInt()}°",
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
                DrawingTool.Select -> Unit
            }
        }
        state.strokes.forEach { stroke ->
            drawStroke(stroke, selected = stroke.id == state.selectedStrokeId && stroke.tool == DrawingTool.Line)
        }
        state.inProgress?.let { drawStroke(it, alpha = 0.9f) }

        val selected = state.selectedStroke
        if (selected != null && selected.tool == DrawingTool.Line) {
            val start = selected.start
            val end = selected.end
            if (start != null && end != null) {
                val handleRadius = 7f
                drawCircle(Color.White, handleRadius, start)
                drawCircle(selected.color, handleRadius - 2f, start, style = Stroke(width = 2f))
                drawCircle(Color.White, handleRadius, end)
                drawCircle(selected.color, handleRadius - 2f, end, style = Stroke(width = 2f))
            }
        }
    }
}

private enum class DrawingTool { Freehand, Line, Angle, Select }

private enum class LineEditHandle { Start, End, Body }

private data class LineHitResult(val strokeId: String, val handle: LineEditHandle, val distance: Float)

private data class EditDragState(
    val strokeId: String,
    val handle: LineEditHandle,
    val dragStart: Offset,
    val originalStart: Offset,
    val originalEnd: Offset,
)

private data class DrawingStroke(
    val tool: DrawingTool,
    val points: List<Offset> = emptyList(),
    val start: Offset? = null,
    val end: Offset? = null,
    val vertex: Offset? = null,
    val angle: Double? = null,
    val color: Color,
    val lineWidth: Float = 3f,
    val id: String = java.util.UUID.randomUUID().toString(),
)

private object DrawingHitTest {
    private const val ENDPOINT_RADIUS = 22f
    private const val LINE_RADIUS = 12f

    fun hitTestLines(point: Offset, strokes: List<DrawingStroke>): LineHitResult? {
        var best: LineHitResult? = null
        for (stroke in strokes) {
            if (stroke.tool != DrawingTool.Line) continue
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
        kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

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

private class DrawingState {
    var currentTool by mutableStateOf(DrawingTool.Freehand)
    var currentColor by mutableStateOf(drawingColors.first())
    val strokes = mutableStateListOf<DrawingStroke>()
    var inProgress by mutableStateOf<DrawingStroke?>(null)
    var selectedStrokeId by mutableStateOf<String?>(null)
    var editDrag by mutableStateOf<EditDragState?>(null)
    private val undoStack = mutableListOf<DrawingStroke>()
    private var a: Offset? = null
    private var b: Offset? = null

    val hasSelection: Boolean get() = selectedStrokeId != null

    val selectedStroke: DrawingStroke?
        get() = selectedStrokeId?.let { id -> strokes.firstOrNull { it.id == id } }

    fun begin(point: Offset) {
        editDrag = null
        inProgress = when (currentTool) {
            DrawingTool.Freehand -> DrawingStroke(currentTool, points = listOf(point), color = currentColor)
            DrawingTool.Line -> DrawingStroke(currentTool, start = point, end = point, color = currentColor)
            DrawingTool.Angle, DrawingTool.Select -> null
        }
    }

    fun move(point: Offset) {
        val current = inProgress ?: return
        inProgress = when (current.tool) {
            DrawingTool.Freehand -> current.copy(points = current.points + point)
            DrawingTool.Line -> current.copy(end = point)
            DrawingTool.Angle, DrawingTool.Select -> current
        }
    }

    fun end() {
        val stroke = inProgress ?: return
        strokes.add(stroke)
        inProgress = null
        undoStack.clear()
        if (stroke.tool == DrawingTool.Line) {
            selectedStrokeId = stroke.id
            currentTool = DrawingTool.Select
        }
    }

    fun selectStroke(point: Offset): Boolean {
        val hit = DrawingHitTest.hitTestLines(point, strokes) ?: return false
        selectedStrokeId = hit.strokeId
        return true
    }

    fun clearSelection() {
        selectedStrokeId = null
        editDrag = null
    }

    fun deleteSelected() {
        val id = selectedStrokeId ?: return
        strokes.removeAll { it.id == id }
        selectedStrokeId = null
        editDrag = null
    }

    fun beginEdit(point: Offset): Boolean {
        if (currentTool != DrawingTool.Select && selectedStrokeId == null) return false
        val hit = DrawingHitTest.hitTestLines(point, strokes) ?: run {
            if (currentTool == DrawingTool.Select) clearSelection()
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
        return true
    }

    fun continueEdit(point: Offset) {
        val drag = editDrag ?: return
        val index = strokes.indexOfFirst { it.id == drag.strokeId }
        if (index < 0) return
        val stroke = strokes[index]
        if (stroke.tool != DrawingTool.Line) return

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
    }

    fun endEdit() {
        editDrag = null
    }

    fun handleAngleTap(point: Offset) {
        val first = a
        val second = b
        when {
            first == null -> a = point
            second == null -> {
                b = point
                strokes.add(DrawingStroke(DrawingTool.Angle, start = first, end = point, color = currentColor))
            }
            else -> {
                strokes.add(DrawingStroke(DrawingTool.Angle, start = second, end = point, color = currentColor))
                strokes.add(
                    DrawingStroke(
                        DrawingTool.Angle,
                        start = second,
                        end = first,
                        vertex = point,
                        angle = angleABC(first, second, point),
                        color = currentColor,
                    ),
                )
                a = null
                b = null
            }
        }
    }

    fun undo() {
        val removed = strokes.removeLastOrNull() ?: return
        if (removed.id == selectedStrokeId) {
            selectedStrokeId = null
        }
        editDrag = null
        undoStack.add(removed)
    }

    fun clearAll() {
        strokes.clear()
        undoStack.clear()
        inProgress = null
        selectedStrokeId = null
        editDrag = null
        a = null
        b = null
    }
}

private val drawingColors = listOf(
    Color(0xFFA666F2),
    Color(0xFFE91E63),
    Color(0xFFFF9800),
    Color(0xFF80E699),
    Color(0xFF80CCFF),
)

private fun angleABC(a: Offset, b: Offset, c: Offset): Double {
    val ba = Offset(a.x - b.x, a.y - b.y)
    val bc = Offset(c.x - b.x, c.y - b.y)
    val dot = ba.x * bc.x + ba.y * bc.y
    val magBA = sqrt(ba.x * ba.x + ba.y * ba.y)
    val magBC = sqrt(bc.x * bc.x + bc.y * bc.y)
    if (magBA <= 0f || magBC <= 0f) return 0.0
    return Math.toDegrees(acos((dot / (magBA * magBC)).coerceIn(-1f, 1f).toDouble()))
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FloatingTextButton(primary: String, secondary: String, highlighted: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(primary, color = if (highlighted) DSColors.goldBright else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(secondary, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FloatingIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToolButton(
    label: String,
    selected: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ColorToolButton(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) color.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

private fun Float.speedText(): String = when {
    this == 1f -> "1x"
    this == 0.5f -> "0.5x"
    this == 0.25f -> "0.25x"
    else -> "${this}x"
}

private fun resolvePlaybackUrl(videoUrl: String?): String? {
    if (videoUrl.isNullOrBlank()) return null
    val trimmed = videoUrl.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val localPath = V2MemoBridge.localVideoAbsolutePath(trimmed) ?: return null
    return if (File(localPath).exists()) "file://$localPath" else null
}

private fun formatTime(seconds: Float): String {
    val safe = seconds.coerceAtLeast(0f)
    val total = safe.toInt()
    val minutes = total / 60
    val secs = total % 60
    val tenths = ((safe - total) * 10).roundToInt().coerceIn(0, 9)
    return "%d:%02d.%d".format(minutes, secs, tenths)
}
