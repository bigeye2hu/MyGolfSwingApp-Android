package com.mygolfswing.app.ui.swing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

/** 挥杆记录库：对齐 iOS `JustSwingCalendarView` 的日期选择 + 当天视频网格。 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun V2JustSwingCalendarScreen(
    onBack: () -> Unit,
    onOpenClip: (UUID) -> Unit,
    onStartRecording: () -> Unit = {},
    initialFilter: V2SwingClipRetention? = null,
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val swingError by V2SwingSessionStore.lastError.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var displayedMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var expandedCalendar by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedClipIds by remember { mutableStateOf(setOf<UUID>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var didApplyInitialFilter by remember(initialFilter) { mutableStateOf(initialFilter == null) }
    var didApplyLatestClipDate by remember { mutableStateOf(false) }
    var transientError by remember { mutableStateOf<String?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    val clipDays = remember(sessions) {
        sessions.flatMap { session -> session.clips.map { session.date } }.toSet()
    }
    val dayClips = remember(sessions, selectedDate) {
        V2SwingSessionStore.clipsForDay(selectedDate).sortedByDescending { it.timestamp }
    }
    val clips = remember(dayClips, selectedFilter) {
        selectedFilter?.let { filter -> dayClips.filter { it.retention == filter } } ?: dayClips
    }

    LaunchedEffect(sessions, initialFilter, didApplyInitialFilter) {
        val filter = initialFilter ?: return@LaunchedEffect
        if (didApplyInitialFilter) return@LaunchedEffect
        val latestDate = sessions
            .flatMap { session -> session.clips.map { clip -> session.date to clip } }
            .filter { (_, clip) -> clip.retention == filter }
            .maxByOrNull { (_, clip) -> clip.timestamp }
            ?.first
        if (latestDate != null) {
            selectedDate = latestDate
            selectedFilter = filter
            didApplyInitialFilter = true
        }
    }

    LaunchedEffect(sessions, initialFilter, didApplyLatestClipDate) {
        if (initialFilter != null || didApplyLatestClipDate) return@LaunchedEffect
        if (dayClips.isNotEmpty()) {
            didApplyLatestClipDate = true
            return@LaunchedEffect
        }
        val latestDate = sessions
            .flatMap { session -> session.clips.map { clip -> session.date to clip } }
            .maxByOrNull { (_, clip) -> clip.timestamp }
            ?.first
            ?: return@LaunchedEffect
        selectedDate = latestDate
        didApplyLatestClipDate = true
    }

    LaunchedEffect(swingError) {
        val message = swingError ?: return@LaunchedEffect
        transientError = message
        V2SwingSessionStore.consumeLastError()
        delay(2_200)
        if (transientError == message) transientError = null
    }

    LaunchedEffect(selectionMode, clips) {
        if (selectionMode && clips.isEmpty()) {
            selectionMode = false
            selectedClipIds = emptySet()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite)
            .v2NavigationBarPadding(),
    ) {
        SwingCalendarTopBar(
            selectionMode = selectionMode,
            hasClips = clips.isNotEmpty(),
            onClose = onBack,
            onCancelSelection = {
                selectionMode = false
                selectedClipIds = emptySet()
            },
            onEnterSelection = { selectionMode = true },
            onToggleSelectAll = {
                selectedClipIds = if (selectedClipIds.size == clips.size) {
                    emptySet()
                } else {
                    clips.map { it.id }.toSet()
                }
            },
            onInfo = { showInfoSheet = true },
        )

        transientError?.let { message ->
            Text(
                text = message,
                color = DSColors.flagRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DSSpacing.lg, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DSColors.flagRed.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }

        DateHeader(
            day = selectedDate,
            clipCount = dayClips.size,
            expanded = expandedCalendar,
            onToggleCalendar = {
                if (!expandedCalendar) displayedMonth = selectedDate.withDayOfMonth(1)
                expandedCalendar = !expandedCalendar
            },
        )

        if (expandedCalendar) {
            MonthCalendar(
                selectedDate = selectedDate,
                displayedMonth = displayedMonth,
                clipDays = clipDays,
                onMonthChange = { displayedMonth = it },
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
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.08f)))

        if (initialFilter != null) {
            RetentionFilterBar(
                selectedFilter = selectedFilter,
                clips = dayClips,
                onFilterChange = {
                    selectedFilter = it
                    selectionMode = false
                    selectedClipIds = emptySet()
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DSSpacing.lg, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(filterTitle(selectedFilter), fontWeight = FontWeight.Bold, color = DSColors.inkGreen, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
            }
        }

        if (clips.isEmpty()) {
            EmptyDayState(
                filter = selectedFilter,
                onClearFilter = { selectedFilter = null },
                modifier = Modifier.weight(1f),
            )
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
                    RetentionBanner(
                        clips = clips,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp, bottom = 16.dp),
                    )
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
        SwingBulkDeleteConfirmationSheet(
            count = selectedClipIds.size,
            onConfirm = {
                    V2SwingSessionStore.deleteClips(selectedClipIds)
                    selectedClipIds = emptySet()
                    selectionMode = false
                    showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showInfoSheet) {
        SwingInfoSheet(onDismiss = { showInfoSheet = false })
    }

}

@Composable
private fun SwingCalendarTopBar(
    selectionMode: Boolean,
    hasClips: Boolean,
    onClose: () -> Unit,
    onCancelSelection: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(DSLayout.topBarHeight)
            .padding(horizontal = DSSpacing.sm)
            .testTag("swing-calendar-top-bar"),
    ) {
        if (selectionMode) {
            TextButton(
                onClick = onCancelSelection,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text("取消", color = DSColors.fairway, fontSize = 15.sp)
            }
        } else {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = DSColors.inkGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = "挥杆记录",
            color = DSColors.inkGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasClips) {
                TextButton(onClick = if (selectionMode) onToggleSelectAll else onEnterSelection) {
                    Text(if (selectionMode) "全选" else "选择", color = DSColors.fairway)
                }
            }
            if (!selectionMode) {
                IconButton(onClick = onInfo) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "关于挥杆记录",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwingBulkDeleteConfirmationSheet(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF4F3F1),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                .v2NavigationBarPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "确认删除 $count 个挥杆视频？",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp),
                )
                Text(
                    text = "视频文件将被永久删除，无法恢复",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 14.dp),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.08f)))
                Text(
                    text = "删除",
                    color = DSColors.flagRed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onConfirm).padding(vertical = 16.dp),
                )
            }
            Text(
                text = "取消",
                color = DSColors.fairway,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable(onClick = onDismiss).padding(vertical = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwingInfoSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("关于挥杆记录", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                ) {
                    Text("完成", color = DSColors.fairway, fontSize = 16.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SwingInfoRow(
                    icon = Icons.Filled.PhoneAndroid,
                    iconColor = Color(0xFFF59E0B),
                    title = "本地存储",
                    description = "挥杆记录仅保存在本设备上。删除 App 或更换设备后，数据将无法恢复。建议将重要视频收藏或保存到相册。",
                )
                SwingInfoRow(
                    icon = Icons.Filled.Schedule,
                    iconColor = Color(0xFF8B5CF6),
                    title = "自动清理",
                    description = "为节省存储空间，普通视频 14 天后自动清理。收藏或存入成长池的视频永久保留。",
                )
                SwingInfoRow(
                    icon = Icons.Filled.Videocam,
                    iconColor = Color(0xFF10B981),
                    title = "遥控录制",
                    description = "配合录制入口使用，可快速开始或停止拍摄，并把值得复盘的挥杆收藏或加入成长池。",
                )
                SwingInfoRow(
                    icon = Icons.Filled.SaveAlt,
                    iconColor = Color(0xFF3B82F6),
                    title = "保存到相册",
                    description = "在挥杆详情页点击保存按钮，可将视频导出到系统相册永久保存。",
                )
            }
        }
    }
}

@Composable
private fun SwingInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFF9CA3AF), fontSize = 14.sp, lineHeight = 20.sp)
        }
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
        if (clipCount > 0) Text("$clipCount 条记录", color = DSColors.shadowGray, fontSize = 12.sp)
        IconButton(
            onClick = onToggleCalendar,
            modifier = Modifier.padding(start = 8.dp).size(32.dp).clip(RoundedCornerShape(8.dp))
                .background(if (expanded) DSColors.fairway.copy(alpha = 0.14f) else DSColors.mistGreen),
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = if (expanded) DSColors.fairway else DSColors.shadowGray, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun RetentionFilterBar(
    selectedFilter: V2SwingClipRetention?,
    clips: List<V2SwingClip>,
    onFilterChange: (V2SwingClipRetention?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            RetentionFilterChip(
                label = "全部",
                count = clips.size,
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
            )
        }
        item {
            RetentionFilterChip(
                label = "收藏",
                count = clips.count { it.retention == V2SwingClipRetention.FAVORITED },
                selected = selectedFilter == V2SwingClipRetention.FAVORITED,
                onClick = { onFilterChange(V2SwingClipRetention.FAVORITED) },
            )
        }
        item {
            RetentionFilterChip(
                label = "成长池",
                count = clips.count { it.retention == V2SwingClipRetention.PROMOTED },
                selected = selectedFilter == V2SwingClipRetention.PROMOTED,
                onClick = { onFilterChange(V2SwingClipRetention.PROMOTED) },
            )
        }
        item {
            RetentionFilterChip(
                label = "普通",
                count = clips.count { it.retention == V2SwingClipRetention.TEMPORARY },
                selected = selectedFilter == V2SwingClipRetention.TEMPORARY,
                onClick = { onFilterChange(V2SwingClipRetention.TEMPORARY) },
            )
        }
    }
}

@Composable
private fun RetentionFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) DSColors.fairway.copy(alpha = 0.14f) else DSColors.cardWhite)
            .border(1.dp, if (selected) DSColors.fairway.copy(alpha = 0.36f) else DSColors.listCardBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, color = if (selected) DSColors.fairway else DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("$count", color = DSColors.shadowGray, fontSize = 11.sp)
    }
}

private fun filterTitle(filter: V2SwingClipRetention?): String = when (filter) {
    V2SwingClipRetention.FAVORITED -> "收藏视频"
    V2SwingClipRetention.PROMOTED -> "成长池视频"
    V2SwingClipRetention.TEMPORARY -> "普通视频"
    null -> "当天视频"
}

@Composable
private fun DateStrip(selectedDate: LocalDate, clipDays: Set<LocalDate>, onSelect: (LocalDate) -> Unit) {
    // iOS JustSwingCalendarView.recentDates: 90 days ending today, oldest → left, today → right
    val days = remember {
        (0..<90).map { offset ->
            LocalDate.now().minusDays((89 - offset).toLong())
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = days.lastIndex)
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
            val cellPx = 42.dp.roundToPx()
            val spacingPx = 4.dp.roundToPx()
            -(viewportWidth / 2 - (cellPx + spacingPx) / 2)
        }
        listState.animateScrollToItem(index, scrollOffset = centerOffset)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(DSLayout.dateStripHeight).testTag("swing-date-strip"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(DSLayout.monthCellSpacing),
        verticalAlignment = Alignment.CenterVertically,
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
private fun MonthCalendar(
    selectedDate: LocalDate,
    displayedMonth: LocalDate,
    clipDays: Set<LocalDate>,
    onMonthChange: (LocalDate) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val first = displayedMonth.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val daysInMonth = first.lengthOfMonth()
    val currentMonth = LocalDate.now().withDayOfMonth(1)
    val canGoForward = !first.plusMonths(1).isAfter(currentMonth)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val cellWidth = ((maxWidth - 24.dp) / 7).coerceAtLeast(32.dp)
        Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onMonthChange(first.minusMonths(1)) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "上个月",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                first.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)),
                color = DSColors.inkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { onMonthChange(first.plusMonths(1)) },
                enabled = canGoForward,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "下个月",
                    tint = if (canGoForward) DSColors.shadowGray else Color.Transparent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        FlowRow(
            maxItemsInEachRow = 7,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, modifier = Modifier.width(cellWidth), textAlign = TextAlign.Center, fontSize = 11.sp, color = DSColors.shadowGray)
            }
            repeat(leading) { Spacer(Modifier.width(cellWidth).height(DSLayout.monthCellHeight)) }
            (1..daysInMonth).forEach { dayNumber ->
                val day = first.withDayOfMonth(dayNumber)
                DateCell(
                    day = day,
                    selected = day == selectedDate,
                    hasClip = day in clipDays,
                    enabled = !day.isAfter(LocalDate.now()),
                    compact = true,
                    cellWidth = cellWidth,
                    onTap = { onSelect(day) },
                )
            }
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
    cellWidth: Dp? = null,
    onTap: () -> Unit,
) {
    val today = day == LocalDate.now()
    val yesterday = day == LocalDate.now().minusDays(1)
    val label = when {
        !compact && today -> "今天"
        !compact && yesterday -> "昨天"
        else -> day.dayOfMonth.toString()
    }
    val cornerRadius = if (compact) 8.dp else 10.dp
    Column(
        modifier = Modifier
            .size(
                width = cellWidth ?: if (compact) DSLayout.dateCellWidth else DSLayout.dateCellWidth,
                height = if (compact) DSLayout.monthCellHeight else DSLayout.dateCellHeight,
            )
            .then(if (!compact && day == LocalDate.now()) Modifier.testTag("swing-date-cell-today") else Modifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (selected) {
                    DSColors.fairway.copy(alpha = if (compact) 0.20f else 0.15f)
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = if (compact) 1.dp else 1.5.dp,
                color = when {
                    selected -> DSColors.fairway.copy(alpha = 0.60f)
                    compact && today -> DSColors.fairway
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(cornerRadius),
            )
            .clickable(enabled = enabled, onClick = onTap)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!compact) Text(day.format(DateTimeFormatter.ofPattern("EEE", Locale.CHINA)), fontSize = 10.sp, color = DSColors.shadowGray)
        Text(
            label,
            fontSize = if (compact) 14.sp else if (label == "今天" || label == "昨天") 12.sp else 16.sp,
            color = if (!enabled) DSColors.shadowGray.copy(alpha = 0.35f) else if (selected || today) DSColors.fairway else DSColors.inkGreen,
            fontWeight = if (compact) {
                if (today) FontWeight.Bold else FontWeight.Normal
            } else if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
        )
        Box(Modifier.padding(top = if (compact) 2.dp else 3.dp).size(if (compact) 4.dp else 5.dp).clip(CircleShape).background(if (hasClip) DSColors.fairway else Color.Transparent))
    }
}

@Composable
private fun SwingClipGridCell(
    clip: V2SwingClip,
    selected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
) {
    val previewModel = remember(clip.thumbnailPath, clip.videoPath) {
        swingClipPreviewModel(clip.thumbnailPath, clip.videoPath)
    }
    Box(
        modifier = Modifier
            .aspectRatio(DSLayout.swingThumbAspectWidth / DSLayout.swingThumbAspectHeight)
            .testTag("swing-thumb")
            .clip(RoundedCornerShape(4.dp))
            .background(DSColors.deepGreen)
            .clickable(onClick = onTap),
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
            if (selected) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.30f)))
                Box(
                    Modifier
                        .matchParentSize()
                        .border(3.dp, DSColors.fairway, RoundedCornerShape(4.dp)),
                )
            }
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) DSColors.fairway else Color.White.copy(alpha = 0.80f),
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp).size(20.dp),
            )
        }
        RetentionBadge(clip.retention, Modifier.align(Alignment.TopEnd).padding(6.dp))
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.60f))))
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                clip.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(durationShort(clip.durationSec), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MissingSwingMediaPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFFE5E7EB)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SportsGolf,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.22f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun RetentionBadge(retention: V2SwingClipRetention, modifier: Modifier = Modifier) {
    val icon = when (retention) {
        V2SwingClipRetention.TEMPORARY -> null
        V2SwingClipRetention.FAVORITED -> Icons.Filled.Star
        V2SwingClipRetention.PROMOTED -> Icons.Filled.ArrowCircleUp
    } ?: return
    val backgroundColor = if (retention == V2SwingClipRetention.PROMOTED) {
        DSColors.fairway.copy(alpha = 0.85f)
    } else {
        DSColors.trophyGold.copy(alpha = 0.85f)
    }
    Icon(
        icon,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(4.dp),
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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            // iOS uses white-at-6% over its dark calendar surface. Android's calendar is
            // intentionally fixed to warm white, so preserve the resulting dark card rather
            // than applying the translucent fill directly and losing the white labels.
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (tempCount > 0 && daysLeft != null) {
            val cleanupColor = retentionCleanupColor(daysLeft)
            RetentionRow(
                icon = Icons.Filled.Schedule,
                label = "普通  ×$tempCount",
                value = if (daysLeft <= 0) "即将清理" else "${daysLeft}天后清理",
                color = cleanupColor,
            )
        }
        if (favCount > 0) {
            RetentionRow(
                icon = Icons.Filled.Star,
                label = "收藏  ×$favCount",
                value = "永久保留",
                color = DSColors.fairway,
                iconColor = DSColors.trophyGold,
            )
        }
        if (promotedCount > 0) {
            RetentionRow(
                icon = Icons.Filled.ArrowCircleUp,
                label = "成长池  ×$promotedCount",
                value = "永久保留",
                color = DSColors.fairway,
                iconColor = DSColors.fairway,
            )
        }
        Text("收藏或存入成长池可永久保留", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RetentionRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    iconColor: Color = color,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun retentionCleanupColor(daysLeft: Int): Color {
    if (daysLeft <= 0) return DSColors.flagRed
    return if (daysLeft <= 4) DSColors.trophyGold else DSColors.fairway
}

@Composable
private fun EmptyDayState(
    filter: V2SwingClipRetention?,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (filter) {
        V2SwingClipRetention.FAVORITED -> "暂无收藏视频"
        V2SwingClipRetention.PROMOTED -> "暂无成长池视频"
        V2SwingClipRetention.TEMPORARY -> "暂无普通视频"
        null -> "暂无挥杆记录"
    }
    val subtitle = when (filter) {
        V2SwingClipRetention.FAVORITED -> "这一天还没有收藏的挥杆记录"
        V2SwingClipRetention.PROMOTED -> "这一天还没有加入成长池的挥杆记录"
        V2SwingClipRetention.TEMPORARY -> "这一天还没有普通挥杆记录"
        null -> "这一天还没有拍摄记录"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.SportsGolf,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.58f),
                modifier = Modifier.size(42.dp),
            )
            Text(title, color = DSColors.inkGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (filter != null) {
                Spacer(Modifier.height(2.dp))
                TextButton(onClick = onClearFilter) {
                    Text("查看当天全部", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
