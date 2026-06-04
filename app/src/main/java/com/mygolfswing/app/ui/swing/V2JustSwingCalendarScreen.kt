package com.mygolfswing.app.ui.swing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

/** 挥杆记录库：对齐 iOS `JustSwingCalendarView` 的日期选择 + 当天视频网格。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2JustSwingCalendarScreen(
    onBack: () -> Unit,
    onOpenClip: (UUID) -> Unit,
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expandedCalendar by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedClipIds by remember { mutableStateOf(setOf<UUID>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val clipDays = remember(sessions) {
        sessions.flatMap { session -> session.clips.map { session.date } }.toSet()
    }
    val clips = remember(sessions, selectedDate) {
        V2SwingSessionStore.clipsForDay(selectedDate).sortedByDescending { it.timestamp }
    }

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(
            title = "挥杆记录",
            onBack = {
                if (selectionMode) {
                    selectionMode = false
                    selectedClipIds = emptySet()
                } else {
                    onBack()
                }
            },
        )

        DateHeader(
            day = selectedDate,
            clipCount = clips.size,
            expanded = expandedCalendar,
            onToggleCalendar = { expandedCalendar = !expandedCalendar },
        )

        if (expandedCalendar) {
            MonthCalendar(
                selectedDate = selectedDate,
                clipDays = clipDays,
                onSelect = {
                    selectedDate = it
                    expandedCalendar = false
                    selectionMode = false
                    selectedClipIds = emptySet()
                },
            )
        } else {
            DateStrip(
                selectedDate = selectedDate,
                clipDays = clipDays,
                onSelect = {
                    selectedDate = it
                    selectionMode = false
                    selectedClipIds = emptySet()
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DSSpacing.lg, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("当天视频", fontWeight = FontWeight.Bold, color = DSColors.inkGreen, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (clips.isNotEmpty()) {
                TextButton(
                    onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selectedClipIds = emptySet()
                        } else {
                            selectionMode = true
                        }
                    },
                ) {
                    Text(if (selectionMode) "取消" else "选择", color = DSColors.fairway)
                }
            }
        }

        if (clips.isEmpty()) {
            EmptyDayState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(clips, key = { it.id }) { clip ->
                    SwingClipGridCell(
                        clip = clip,
                        selected = clip.id in selectedClipIds,
                        selectionMode = selectionMode,
                        onTap = {
                            if (selectionMode) {
                                selectedClipIds = selectedClipIds.toggle(clip.id)
                            } else {
                                onOpenClip(clip.id)
                            }
                        },
                    )
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    RetentionBanner(clips = clips, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().background(DSColors.cardWhite).padding(DSSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已选 ${selectedClipIds.size} 项", color = DSColors.inkGreen, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { selectedClipIds = if (selectedClipIds.size == clips.size) emptySet() else clips.map { it.id }.toSet() }) {
                    Text(if (selectedClipIds.size == clips.size) "取消全选" else "全选", color = DSColors.fairway)
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedClipIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.flagRed),
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("删除")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除挥杆视频？") },
            text = { Text("将删除已选 ${selectedClipIds.size} 段视频和缩略图，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    V2SwingSessionStore.deleteClips(selectedClipIds)
                    selectedClipIds = emptySet()
                    selectionMode = false
                    showDeleteConfirm = false
                }) { Text("删除", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DateHeader(day: LocalDate, clipCount: Int, expanded: Boolean, onToggleCalendar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(monthDayString(day), color = DSColors.inkGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        if (clipCount > 0) Text("$clipCount 杆", color = DSColors.shadowGray, fontSize = 12.sp)
        IconButton(
            onClick = onToggleCalendar,
            modifier = Modifier.padding(start = 8.dp).size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (expanded) DSColors.fairway.copy(alpha = 0.14f) else DSColors.mistGreen),
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = if (expanded) DSColors.fairway else DSColors.shadowGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DateStrip(selectedDate: LocalDate, clipDays: Set<LocalDate>, onSelect: (LocalDate) -> Unit) {
    // iOS JustSwingCalendarView.recentDates: 90 days ending today, oldest → left, today → right
    val days = remember {
        (0..<90).map { offset ->
            LocalDate.now().minusDays((89 - offset).toLong())
        }
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var skipCenterOnFirstSelection by remember { mutableStateOf(true) }

    // iOS onAppear: scrollTo(selectedDate, anchor: .trailing) — today sits on the right
    LaunchedEffect(Unit) {
        delay(100)
        val index = days.indexOf(selectedDate).takeIf { it >= 0 } ?: days.lastIndex
        listState.scrollToItem(index)
        skipCenterOnFirstSelection = false
    }
    // iOS onChange(of: selectedDate): scrollTo(..., anchor: .center)
    LaunchedEffect(selectedDate) {
        if (skipCenterOnFirstSelection) return@LaunchedEffect
        val index = days.indexOf(selectedDate).takeIf { it >= 0 } ?: return@LaunchedEffect
        val viewportWidth = listState.layoutInfo.viewportSize.width
        val centerOffset = with(density) {
            val cellPx = 44.dp.roundToPx()
            val spacingPx = 6.dp.roundToPx()
            -(viewportWidth / 2 - (cellPx + spacingPx) / 2)
        }
        listState.animateScrollToItem(index, scrollOffset = centerOffset)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(days, key = { _, day -> day.toString() }) { idx, day ->
            val showMonth = idx == 0 || day.month != days[idx - 1].month
            if (showMonth) {
                Text(
                    day.format(DateTimeFormatter.ofPattern("M月", Locale.CHINA)),
                    modifier = Modifier.width(30.dp).padding(end = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.shadowGray,
                )
            }
            DateCell(
                day = day,
                selected = day == selectedDate,
                hasClip = day in clipDays,
                onTap = { onSelect(day) },
            )
        }
    }
}

@Composable
private fun MonthCalendar(selectedDate: LocalDate, clipDays: Set<LocalDate>, onSelect: (LocalDate) -> Unit) {
    val first = selectedDate.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val daysInMonth = first.lengthOfMonth()
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(first.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)), color = DSColors.inkGreen, fontWeight = FontWeight.Bold)
        FlowRow(
            maxItemsInEachRow = 7,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = DSColors.shadowGray)
            }
            repeat(leading) { Spacer(Modifier.width(44.dp).height(40.dp)) }
            (1..daysInMonth).forEach { dayNumber ->
                val day = first.withDayOfMonth(dayNumber)
                DateCell(
                    day = day,
                    selected = day == selectedDate,
                    hasClip = day in clipDays,
                    enabled = !day.isAfter(LocalDate.now()),
                    compact = true,
                    onTap = { onSelect(day) },
                )
            }
        }
    }
}

@Composable
private fun DateCell(
    day: LocalDate,
    selected: Boolean,
    hasClip: Boolean,
    enabled: Boolean = true,
    compact: Boolean = false,
    onTap: () -> Unit,
) {
    val today = day == LocalDate.now()
    Column(
        modifier = Modifier
            .size(width = 44.dp, height = if (compact) 40.dp else 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) DSColors.fairway.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected || today) DSColors.fairway.copy(alpha = 0.65f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onTap)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!compact) Text(day.format(DateTimeFormatter.ofPattern("EEE", Locale.CHINA)), fontSize = 9.sp, color = DSColors.shadowGray)
        Text(
            if (!compact && today) "今天" else day.dayOfMonth.toString(),
            fontSize = if (compact) 14.sp else 12.sp,
            color = if (!enabled) DSColors.shadowGray.copy(alpha = 0.35f) else if (selected || today) DSColors.fairway else DSColors.inkGreen,
            fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
        )
        Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(if (hasClip) DSColors.fairway else Color.Transparent))
    }
}

@Composable
private fun SwingClipGridCell(clip: V2SwingClip, selected: Boolean, selectionMode: Boolean, onTap: () -> Unit) {
    val previewModel = remember(clip.thumbnailPath, clip.videoPath) {
        swingClipPreviewModel(clip.thumbnailPath, clip.videoPath)
    }
    Box(
        modifier = Modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(4.dp)).background(DSColors.deepGreen).clickable(onClick = onTap),
    ) {
        if (previewModel != null) {
            AsyncImage(
                model = previewModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MissingSwingMediaPlaceholder(Modifier.matchParentSize())
        }
        if (clip.retention.isPermanent) {
            Box(
                modifier = Modifier.matchParentSize().border(
                    2.dp,
                    if (clip.retention == V2SwingClipRetention.PROMOTED) DSColors.fairway else DSColors.trophyGold,
                    RoundedCornerShape(4.dp),
                ),
            )
        }
        if (selectionMode) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = if (selected) 0.32f else 0.08f)))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (selected) DSColors.fairway else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp),
            )
        }
        RetentionBadge(clip.retention, Modifier.align(Alignment.TopEnd).padding(6.dp))
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(clip.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(durationShort(clip.durationSec), color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun MissingSwingMediaPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(DSColors.deepGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "文件缺失",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RetentionBadge(retention: V2SwingClipRetention, modifier: Modifier = Modifier) {
    val icon = when (retention) {
        V2SwingClipRetention.TEMPORARY -> null
        V2SwingClipRetention.FAVORITED -> Icons.Filled.Star
        V2SwingClipRetention.PROMOTED -> Icons.Filled.Upgrade
    } ?: return
    Icon(
        icon,
        contentDescription = null,
        tint = if (retention == V2SwingClipRetention.PROMOTED) DSColors.fairway else DSColors.trophyGold,
        modifier = modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(3.dp),
    )
}

@Composable
private fun RetentionBanner(clips: List<V2SwingClip>, modifier: Modifier = Modifier) {
    val tempCount = clips.count { it.retention == V2SwingClipRetention.TEMPORARY }
    val favCount = clips.count { it.retention == V2SwingClipRetention.FAVORITED }
    val promotedCount = clips.count { it.retention == V2SwingClipRetention.PROMOTED }
    val oldestTemp = clips.filter { it.retention == V2SwingClipRetention.TEMPORARY }.minByOrNull { it.timestamp }
    val daysLeft = oldestTemp?.let { 14 - ChronoUnit.DAYS.between(it.timestamp.toLocalDate(), LocalDate.now()).toInt() }
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.deepGreen).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RetentionRow("普通 ×$tempCount", daysLeft?.let { if (it <= 0) "即将清理" else "${it}天后清理" } ?: "14天后清理", DSColors.trophyGold)
        if (favCount > 0) RetentionRow("收藏 ×$favCount", "永久保留", DSColors.fairway)
        if (promotedCount > 0) RetentionRow("成长池 ×$promotedCount", "永久保留", DSColors.fairway)
        Text("收藏或存入成长池可永久保留", color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp)
    }
}

@Composable
private fun RetentionRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyDayState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("暂无挥杆记录", color = DSColors.inkGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("这一天还没有拍摄记录", color = DSColors.shadowGray, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun Set<UUID>.toggle(id: UUID): Set<UUID> =
    if (id in this) this - id else this + id

private fun monthDayString(day: LocalDate): String {
    val today = LocalDate.now()
    return when (ChronoUnit.DAYS.between(day, today)) {
        0L -> "今天"
        1L -> "昨天"
        else -> day.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }
}

private fun durationShort(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    return if (t < 60) "${t}s" else "%d:%02d".format(t / 60, t % 60)
}
