package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingSession
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.swing.swingClipPreviewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

internal enum class V2MemoRecordFilter(val title: String) {
    SWING("挥杆记录"), VIDEO("视频笔记"), NOTE("随手记"),
}

private val memoTagListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it.toList() },
)

internal fun V2MemoRecordFilter.createTitle(): String = when (this) {
    V2MemoRecordFilter.SWING -> "拍摄"
    V2MemoRecordFilter.VIDEO -> "添加"
    V2MemoRecordFilter.NOTE -> "写一条"
}

internal fun V2MemoRecordFilter.listSpacingDp(): Int = when (this) {
    V2MemoRecordFilter.SWING, V2MemoRecordFilter.VIDEO, V2MemoRecordFilter.NOTE -> 8
}

/**
 * Mirrors iOS `alignSwingDay`: open on the latest recorded day, then only jump
 * back to today when a genuinely newer clip is captured today.
 */
internal fun swingDayAlignmentTarget(
    latest: LocalDateTime?,
    previousLatest: LocalDateTime?,
    didAlign: Boolean,
    today: LocalDate = LocalDate.now(),
): LocalDate? {
    latest ?: return null
    if (!didAlign) return latest.toLocalDate()
    if (previousLatest == null || !latest.isAfter(previousLatest)) return null
    return latest.toLocalDate().takeIf { it == today }
}

internal fun V2SwingClip.recordTitle(): String =
    notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "挥杆 ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))}"

internal fun V2SwingClip.matchesRecordQuery(query: String, location: String? = null): Boolean {
    val keyword = query.trim()
    return keyword.isBlank() || recordTitle().contains(keyword, ignoreCase = true) ||
        notes.orEmpty().contains(keyword, ignoreCase = true) ||
        location.orEmpty().contains(keyword, ignoreCase = true)
}

internal fun swingRecordDayTitle(
    day: LocalDate,
    count: Int,
    today: LocalDate = LocalDate.now(),
): String {
    val pattern = if (day.year == today.year) "M月d日" else "yyyy年M月d日"
    val date = day.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
    return if (count > 0) "$date · ${count}条挥杆记录" else date
}

internal fun groupSwingRecords(
    clipsWithLocation: List<Pair<V2SwingClip, String?>>,
    query: String,
    selectedDay: LocalDate,
): Map<LocalDate, List<Pair<V2SwingClip, String?>>> {
    val keyword = query.trim()
    return clipsWithLocation
        .filter { (clip, location) ->
            if (keyword.isBlank()) clip.timestamp.toLocalDate() == selectedDay
            else clip.matchesRecordQuery(keyword, location)
        }
        .groupBy { it.first.timestamp.toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .mapValues { (_, pairs) -> pairs.sortedBy { it.first.timestamp } }
}

/**
 * Keep the render boundary as strict as iOS `V2MemoView`, which passes empty
 * arrays to the record browser while the user is signed out. The repositories
 * are account-scoped too, but the UI must not depend on asynchronous store
 * cleanup to hide a previous account's in-memory snapshot.
 */
internal fun visibleMemoRecords(isLoggedIn: Boolean, memos: List<V2Memo>): List<V2Memo> =
    if (isLoggedIn) memos else emptyList()

internal fun visibleSwingSessions(
    isLoggedIn: Boolean,
    sessions: List<V2SwingSession>,
): List<V2SwingSession> = if (isLoggedIn) sessions else emptyList()

/** iOS `V2MemoRecordBrowser` 对齐版：Memo 主入口统一浏览三类记录。 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun V2MemoRecordBrowserScreen(
    isLoggedIn: Boolean,
    readOnly: Boolean = false,
    applyStatusBarPadding: Boolean = true,
    requestedFilter: String? = null,
    onRequestedFilterConsumed: () -> Unit = {},
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onOpenMemo: (UUID) -> Unit,
    onOpenSwingClip: (UUID) -> Unit,
    onOpenJustSwing: () -> Unit,
    onRequestLogin: () -> Unit,
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val categoryGroups by V2CategoryStore.groups.collectAsState()
    var filterName by rememberSaveable { mutableStateOf(V2MemoRecordFilter.SWING.name) }
    val filter = V2MemoRecordFilter.valueOf(filterName)
    LaunchedEffect(requestedFilter) {
        val next = requestedFilter ?: return@LaunchedEffect
        filterName = next
        onRequestedFilterConsumed()
    }
    var query by rememberSaveable { mutableStateOf("") }
    var videoTags by rememberSaveable(stateSaver = memoTagListSaver) { mutableStateOf<List<String>>(emptyList()) }
    var noteTags by rememberSaveable(stateSaver = memoTagListSaver) { mutableStateOf<List<String>>(emptyList()) }
    var showCategoryFilter by rememberSaveable { mutableStateOf(false) }
    var calendarExpanded by rememberSaveable { mutableStateOf(true) }
    var visibleMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDayText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var didAlignSwingDay by remember { mutableStateOf(false) }
    var previousLatestSwingClipTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var editingClipId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleMonth = YearMonth.parse(visibleMonthText)
    val selectedDay = LocalDate.parse(selectedDayText)
    val accountMemos = remember(memos, isLoggedIn) { visibleMemoRecords(isLoggedIn, memos) }
    val accountSessions = remember(sessions, isLoggedIn) { visibleSwingSessions(isLoggedIn, sessions) }
    val clipsWithLocation = remember(accountSessions) {
        accountSessions.flatMap { session -> session.clips.map { it to session.locationName } }
    }
    val markedDays = remember(clipsWithLocation) { clipsWithLocation.map { it.first.timestamp.toLocalDate() }.toSet() }
    val latestSwingClipTime = remember(clipsWithLocation) {
        clipsWithLocation.maxOfOrNull { it.first.timestamp }
    }
    val videoSource = remember(accountMemos) { accountMemos.filter { it.kind != V2MemoKind.TEXT } }
    val noteSource = remember(accountMemos) { accountMemos.filter { it.kind == V2MemoKind.TEXT } }
    val selectedTags = if (filter == V2MemoRecordFilter.VIDEO) videoTags else noteTags
    val videoMemos = remember(videoSource, query, videoTags) {
        videoSource.filter { it.matchesVideoNoteQuery(query) && it.matchesSelectedTags(videoTags) }
            .sortedByDescending { it.createdAt }
    }
    val noteMemos = remember(noteSource, query, noteTags) {
        noteSource.filter { it.matchesTextNoteQuery(query) && it.matchesSelectedTags(noteTags) }
            .sortedByDescending { it.createdAt }
    }
    val visibleSwingGroups = remember(clipsWithLocation, query, selectedDay) {
        groupSwingRecords(clipsWithLocation, query, selectedDay)
    }

    LaunchedEffect(latestSwingClipTime) {
        swingDayAlignmentTarget(
            latest = latestSwingClipTime,
            previousLatest = previousLatestSwingClipTime,
            didAlign = didAlignSwingDay,
        )?.let { target ->
            selectedDayText = target.toString()
            visibleMonthText = YearMonth.from(target).toString()
            didAlignSwingDay = true
        }
        previousLatestSwingClipTime = latestSwingClipTime
    }

    V2RoomPage(
        Modifier.fillMaxSize().then(if (applyStatusBarPadding) Modifier.v2StatusBarPadding() else Modifier),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("我的记录", color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (readOnly) {
                    Text(
                        "只读",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                } else {
                    Button(
                        onClick = {
                            when (filter) {
                                V2MemoRecordFilter.SWING -> if (isLoggedIn) onOpenJustSwing() else onRequestLogin()
                                V2MemoRecordFilter.VIDEO -> if (isLoggedIn) onCreateVideoMemo() else onRequestLogin()
                                V2MemoRecordFilter.NOTE -> if (isLoggedIn) onCreateTextMemo() else onRequestLogin()
                            }
                        },
                        modifier = Modifier.height(DSLayout.compactButtonHeight).semantics { contentDescription = filter.createTitle() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(
                            if (filter == V2MemoRecordFilter.SWING) Icons.Filled.Videocam else if (filter == V2MemoRecordFilter.NOTE) Icons.Filled.Edit else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(filter.createTitle(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                if (filter == V2MemoRecordFilter.SWING) {
                    IconButton(
                        onClick = { calendarExpanded = !calendarExpanded },
                        modifier = Modifier.padding(start = 8.dp).size(30.dp).clip(CircleShape).background(DSColors.mistGreen),
                    ) {
                        Icon(
                            if (calendarExpanded) Icons.Filled.ExpandLess else Icons.Filled.CalendarMonth,
                            contentDescription = if (calendarExpanded) "收起月历" else "展开月历",
                            tint = DSColors.fairway,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showCategoryFilter = true },
                        modifier = Modifier.padding(start = 8.dp).size(30.dp).clip(CircleShape).background(DSColors.mistGreen),
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "打开分类筛选",
                            tint = DSColors.fairway,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            V2MemoRecordSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = when (filter) {
                    V2MemoRecordFilter.SWING -> "搜索挥杆记录"
                    V2MemoRecordFilter.VIDEO -> "搜索视频笔记"
                    V2MemoRecordFilter.NOTE -> "搜索随手记"
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(50))
                    .background(DSColors.mistGreen.copy(alpha = 0.8f)).padding(4.dp),
            ) {
                V2MemoRecordFilter.entries.forEach { item ->
                    Text(
                        item.title,
                        color = if (filter == item) Color.White else DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(50))
                            .background(if (filter == item) DSColors.fairway else Color.Transparent)
                            .clickable { filterName = item.name; query = "" }
                            .padding(top = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            if (filter != V2MemoRecordFilter.SWING) {
                MemoCategoryFilterBar(
                    selectedTags = selectedTags,
                    onOpen = { showCategoryFilter = true },
                    onRemove = { tag ->
                        val next = selectedTags.filterNot { it == tag }
                        if (filter == V2MemoRecordFilter.VIDEO) videoTags = next else noteTags = next
                    },
                    onClear = {
                        if (filter == V2MemoRecordFilter.VIDEO) videoTags = emptyList() else noteTags = emptyList()
                    },
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).testTag("memo-record-scroll-content"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(filter.listSpacingDp().dp),
            ) {
                when (filter) {
                V2MemoRecordFilter.SWING -> {
                    if (calendarExpanded) {
                        item {
                            // iOS uses 12pt between the calendar and day section,
                            // then 10pt between the header and record cards.
                            Box(Modifier.padding(bottom = 2.dp)) {
                                MemoMonthCalendar(
                                    month = visibleMonth,
                                    selectedDay = selectedDay,
                                    markedDays = markedDays,
                                    onPrevious = { visibleMonthText = visibleMonth.minusMonths(1).toString() },
                                    onNext = { visibleMonthText = visibleMonth.plusMonths(1).toString() },
                                    onSelect = { selectedDayText = it.toString() },
                                )
                            }
                        }
                    }
                    if (query.isBlank()) {
                        val pairs = visibleSwingGroups[selectedDay].orEmpty()
                        item { SwingRecordDayHeader(day = selectedDay, count = pairs.size) }
                        if (pairs.isEmpty()) {
                            item { QuietRecordEmpty("这天没有挥杆记录") }
                        } else {
                            items(pairs, key = { it.first.id }) { (clip, location) ->
                                SwingRecordCard(
                                    clip = clip,
                                    location = location,
                                    onOpen = { if (isLoggedIn) onOpenSwingClip(clip.id) else onRequestLogin() },
                                    onEdit = { if (isLoggedIn) editingClipId = clip.id.toString() else onRequestLogin() },
                                )
                            }
                        }
                    } else if (visibleSwingGroups.isEmpty()) {
                        item { QuietRecordEmpty("没有找到挥杆记录") }
                    } else {
                        visibleSwingGroups.forEach { (day, pairs) ->
                            item { SwingRecordDayHeader(day = day, count = pairs.size) }
                            items(pairs, key = { it.first.id }) { (clip, location) ->
                                SwingRecordCard(
                                    clip = clip,
                                    location = location,
                                    onOpen = { if (isLoggedIn) onOpenSwingClip(clip.id) else onRequestLogin() },
                                    onEdit = { if (isLoggedIn) editingClipId = clip.id.toString() else onRequestLogin() },
                                )
                            }
                        }
                    }
                }
                V2MemoRecordFilter.VIDEO -> {
                    if (videoMemos.isEmpty()) {
                        item {
                            QuietRecordEmpty(
                                memoFilteredEmptyText(
                                    hasAny = videoSource.isNotEmpty(),
                                    query = query,
                                    none = "还没有视频笔记",
                                    notFound = "没有找到视频笔记",
                                    filtered = "当前筛选下没有视频笔记",
                                ),
                            )
                        }
                    }
                    items(videoMemos, key = { it.id }) { memo ->
                        VideoNoteRecordCard(memo = memo, onOpen = { if (isLoggedIn) onOpenMemo(memo.id) else onRequestLogin() })
                    }
                }
                V2MemoRecordFilter.NOTE -> {
                    if (noteMemos.isEmpty()) {
                        item {
                            QuietRecordEmpty(
                                memoFilteredEmptyText(
                                    hasAny = noteSource.isNotEmpty(),
                                    query = query,
                                    none = "还没有随手记",
                                    notFound = "没有找到随手记",
                                    filtered = "当前筛选下没有随手记",
                                ),
                            )
                        }
                    }
                    items(noteMemos, key = { it.id }) { memo ->
                        TextNoteRecordCard(memo = memo, onOpen = { if (isLoggedIn) onOpenMemo(memo.id) else onRequestLogin() })
                    }
                }
                }
            }
        }
    }

    if (showCategoryFilter && filter != V2MemoRecordFilter.SWING) {
        val source = if (filter == V2MemoRecordFilter.VIDEO) videoSource else noteSource
        MemoCategoryFilterSheet(
            sections = memoCategoryFilterSections(source, categoryGroups),
            selectedTags = selectedTags,
            onToggle = { name ->
                val next = if (name in selectedTags) selectedTags.filterNot { it == name } else selectedTags + name
                if (filter == V2MemoRecordFilter.VIDEO) videoTags = next else noteTags = next
            },
            onDismiss = { showCategoryFilter = false },
        )
    }

    editingClipId?.let { rawId ->
        val clip = clipsWithLocation.firstOrNull { it.first.id.toString() == rawId }?.first
        if (clip != null) {
            MemoSwingNoteSheet(
                initialNote = clip.notes.orEmpty(),
                onDismiss = { editingClipId = null },
                onSave = {
                    V2SwingSessionStore.updateClipNotes(clip.id, it)
                    editingClipId = null
                },
            )
        }
    }
}

/** The active iOS record browser uses a 44pt filled search row, not an outlined field. */
@Composable
private fun V2MemoRecordSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(DSLayout.searchFieldHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.cardWhite)
            .padding(horizontal = 12.dp)
            .testTag("memo-record-search"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = DSColors.shadowGray,
            modifier = Modifier.size(15.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(placeholder, color = DSColors.shadowGray, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 13.sp),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoMonthCalendar(
    month: YearMonth,
    selectedDay: LocalDate,
    markedDays: Set<LocalDate>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value % 7
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月", tint = DSColors.fairway)
            }
            Spacer(Modifier.weight(1f))
            Text(month.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)), color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下个月", tint = DSColors.fairway)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(it, color = DSColors.shadowGray, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val cells: List<LocalDate?> = buildList {
            repeat(leading) { add(null) }
            (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).height(32.dp).then(if (day != null) Modifier.clickable { onSelect(day) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val selected = day == selectedDay
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(if (selected) DSColors.fairway else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(day.dayOfMonth.toString(), color = if (selected) Color.White else DSColors.inkGreen, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                if (day in markedDays && !selected) Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(DSColors.fairway))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SwingRecordDayHeader(day: LocalDate, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            swingRecordDayTitle(day, count),
            color = DSColors.inkGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (day == LocalDate.now()) RecordTodayBadge()
    }
}

@Composable
internal fun SwingRecordCard(clip: V2SwingClip, location: String?, onOpen: () -> Unit, onEdit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite).clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(width = 112.dp, height = 76.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.deepGreen), contentAlignment = Alignment.Center) {
                val preview = remember(clip.thumbnailPath, clip.videoPath) { swingClipPreviewModel(clip.thumbnailPath, clip.videoPath) }
                if (preview != null) AsyncImage(preview, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (clip.durationSec > 0) {
                    Text(
                        "%d:%02d".format(clip.durationSec.toInt() / 60, clip.durationSec.toInt() % 60),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.68f)).padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    clip.recordTitle(),
                    color = DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(end = 32.dp),
                )
                Text(
                    buildString {
                        append(clip.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)))
                        location?.trim()?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp)
                .clip(CircleShape).background(DSColors.mistGreen)
                .semantics { contentDescription = if (clip.notes.isNullOrBlank()) "写一句心得" else "修改心得" },
        ) {
            Icon(Icons.Filled.Edit, null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
        }
    }
}

internal fun V2Memo.matchesVideoNoteQuery(query: String): Boolean {
    val keyword = query.trim()
    if (keyword.isBlank()) return true
    return displayTitle.orEmpty().contains(keyword, ignoreCase = true) ||
        text.orEmpty().contains(keyword, ignoreCase = true) ||
        videoSourceChipFor(this).first.contains(keyword, ignoreCase = true) ||
        tags.any { it.contains(keyword, ignoreCase = true) }
}

internal fun V2Memo.matchesTextNoteQuery(query: String): Boolean {
    val keyword = query.trim()
    if (keyword.isBlank()) return true
    return displayTitle.orEmpty().contains(keyword, ignoreCase = true) ||
        text.orEmpty().contains(keyword, ignoreCase = true) ||
        tags.any { it.contains(keyword, ignoreCase = true) }
}

internal data class V2MemoCategoryFilterSection(
    val id: String,
    val title: String,
    val names: List<String>,
)

internal fun V2Memo.matchesSelectedTags(selectedTags: List<String>): Boolean {
    if (selectedTags.isEmpty()) return true
    val names = tags.map(String::trim).filter(String::isNotEmpty).toSet()
    return selectedTags.any { it in names }
}

internal fun memoCategoryFilterSections(
    memos: List<V2Memo>,
    groups: List<V2CategoryGroup>,
): List<V2MemoCategoryFilterSection> {
    val used = memos.flatMap { it.tags }.map(String::trim).filter(String::isNotEmpty).toSet()
    if (used.isEmpty()) return emptyList()
    val claimed = mutableSetOf<String>()
    val sections = mutableListOf<V2MemoCategoryFilterSection>()
    groups.filter { it.items.isNotEmpty() }.forEach { group ->
        val names = group.items.map { it.name }.filter { name -> name in used && claimed.add(name) }
        if (names.isNotEmpty()) sections += V2MemoCategoryFilterSection(group.id, group.name, names)
    }
    val leftovers = used.subtract(claimed).sorted()
    if (leftovers.isNotEmpty()) sections += V2MemoCategoryFilterSection("other", "其他", leftovers)
    return sections
}

internal fun memoFilteredEmptyText(hasAny: Boolean, query: String, none: String, notFound: String, filtered: String): String =
    when {
        !hasAny -> none
        query.trim().isNotEmpty() -> notFound
        else -> filtered
    }

internal data class V2TextNotePreview(val display: String, val canToggle: Boolean)

internal fun textNotePreview(raw: String, expanded: Boolean): V2TextNotePreview {
    val text = raw.trim()
    if (text.isEmpty()) return V2TextNotePreview("", false)
    val isShort = text.length <= 120 && text.lines().size <= 4
    if (expanded || isShort) return V2TextNotePreview(text, !isShort)
    val kept = mutableListOf<String>()
    var used = 0
    for (paragraph in text.lines()) {
        if (kept.size >= 4) break
        val piece = paragraph.trim()
        if (piece.isEmpty()) {
            if (kept.isNotEmpty()) kept += ""
            continue
        }
        if (used > 0 && used + piece.length > 120) break
        if (piece.length > 120 && kept.isEmpty()) {
            kept += sentencePrefix(piece, 120)
            break
        }
        kept += paragraph
        used += piece.length
        if (used >= 120) break
    }
    val result = kept.joinToString("\n").trim().ifEmpty { sentencePrefix(text, 120) }
    return V2TextNotePreview(result, true)
}

private fun sentencePrefix(paragraph: String, limit: Int): String {
    val head = paragraph.take(limit)
    val index = head.indexOfLast { it in setOf('。', '！', '？', '!', '?', '；', ';') }
    return if (index >= 12) head.take(index + 1) else head
}

internal fun textNoteDateText(date: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String = when {
    date.toLocalDate() == now.toLocalDate() -> "今天"
    date.toLocalDate() == now.toLocalDate().minusDays(1) -> "昨天"
    date.year == now.year -> date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    else -> date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
}

@Composable
internal fun VideoNoteRecordCard(memo: V2Memo, onOpen: () -> Unit) {
    val title = memo.displayTitle?.trim().orEmpty().ifBlank { "未命名视频笔记" }
    val note = memo.text?.trim()?.takeIf { it.isNotBlank() && it != title }
    val sourceTitle = videoSourceChipFor(memo).first
    val needsRecovery = remember(memo.videoUrl, memo.isOnlineVideo, memo.isPlazaVideo) {
        !memo.isOnlineVideo && !memo.isPlazaVideo && !memo.videoUrl.isNullOrBlank() && !memoVideoPlayable(memo.videoUrl)
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite).border(1.dp, DSColors.listCardBorder, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(width = 120.dp, height = 84.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.fairway),
                contentAlignment = Alignment.Center,
            ) {
                V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
                memo.durationSec?.takeIf { it > 0 }?.let { seconds ->
                    val whole = seconds.toInt()
                    Text(
                        "%d:%02d".format(whole / 60, whole % 60),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(5.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.68f)).padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "$sourceTitle · ${V2MemoListGrouping.metaPrefix(memo.createdAt)}",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (memo.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        memo.tags.take(2).forEach { tag ->
                            Text(
                                tag,
                                color = DSColors.fairway,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(DSColors.mistGreen).padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                note?.let { Text(it, color = DSColors.shadowGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        if (needsRecovery) {
            Row(
                modifier = Modifier.fillMaxWidth().background(DSColors.trophyGold.copy(alpha = 0.06f)).clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Download, null, tint = DSColors.trophyGold, modifier = Modifier.size(15.dp))
                Text("视频待恢复", color = DSColors.trophyGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = DSColors.trophyGold, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
internal fun TextNoteRecordCard(memo: V2Memo, onOpen: () -> Unit) {
    var expanded by remember(memo.id) { mutableStateOf(false) }
    val title = memo.displayTitle?.trim().orEmpty().ifBlank { "未命名随手记" }
    val body = memo.text?.trim().orEmpty().takeIf { it.isNotBlank() && it != title }.orEmpty()
    val preview = textNotePreview(body, expanded)
    val tags = memo.tags.map(String::trim).filter(String::isNotEmpty).take(2)
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite).border(1.dp, DSColors.listCardBorder, RoundedCornerShape(16.dp))
            .testTag("memo-text-note-card"),
    ) {
        Canvas(
            modifier = Modifier.matchParentSize().testTag("memo-text-note-accent"),
        ) {
            val inset = 12.dp.toPx()
            drawRoundRect(
                color = DSColors.fairway.copy(alpha = 0.72f),
                topLeft = Offset(0f, inset),
                size = Size(3.dp.toPx(), (size.height - inset * 2).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                if (preview.display.isNotEmpty()) {
                    Text(preview.display, color = DSColors.inkGreen.copy(alpha = 0.86f), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            if (preview.canToggle) {
                Text(
                    if (expanded) "收起" else "展开",
                    color = DSColors.fairway,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(textNoteDateText(memo.createdAt), color = DSColors.shadowGray, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (tags.isNotEmpty()) Text(tags.joinToString(" · "), color = DSColors.fairway, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("更多", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpen))
            }
        }
    }
}

@Composable
private fun MemoCategoryFilterBar(
    selectedTags: List<String>,
    onOpen: () -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.height(32.dp).clip(CircleShape).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.listCardBorder, CircleShape)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = DSColors.inkGreen, modifier = Modifier.size(12.dp))
                Text("分类筛选", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            selectedTags.forEach { tag ->
                Row(
                    modifier = Modifier.height(32.dp).clip(CircleShape).background(DSColors.mistGreen).padding(start = 12.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(tag, color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "去掉$tag",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(10.dp).clickable { onRemove(tag) },
                    )
                }
            }
        }
        if (selectedTags.isNotEmpty()) {
            Text(
                "清除",
                color = DSColors.fairway,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MemoCategoryFilterSheet(
    sections: List<V2MemoCategoryFilterSection>,
    selectedTags: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().v2NavigationBarPadding().padding(bottom = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("分类筛选", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("完成", color = DSColors.fairway, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onDismiss))
            }
            if (sections.isEmpty()) {
                Text(
                    "这些记录还没有分类",
                    color = DSColors.shadowGray,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                )
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    sections.forEach { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(section.title, color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                section.names.forEach { name ->
                                    val selected = name in selectedTags
                                    Text(
                                        name,
                                        color = if (selected) DSColors.fairway else DSColors.inkGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.height(32.dp).clip(CircleShape)
                                            .background(if (selected) DSColors.mistGreen else DSColors.cardWhite)
                                            .border(1.dp, if (selected) DSColors.fairway else DSColors.listCardBorder, CircleShape)
                                            .clickable { onToggle(name) }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietRecordEmpty(text: String) {
    Text(text, color = DSColors.shadowGray, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp))
}

@Composable
private fun RecordTodayBadge() {
    Text("今天", color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).background(DSColors.mistGreen).padding(horizontal = 8.dp, vertical = 3.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoSwingNoteSheet(initialNote: String, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var draft by remember(initialNote) { mutableStateOf(initialNote.trim().take(40)) }
    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().v2NavigationBarPadding().padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("写一句心得", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "会显示成这条记录的标题，时间仍留在下面。",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it.take(40) },
                singleLine = true,
                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 16.sp),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxWidth().height(36.dp).focusRequester(focusRequester)
                    .clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp).testTag("memo-swing-note-input"),
                decorationBox = { innerField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text("例如：下杆时重心偏右", color = DSColors.shadowGray, fontSize = 16.sp)
                        }
                        innerField()
                    }
                },
            )
            Text(
                "${draft.length}/40",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End),
            )
            Button(
                onClick = { onSave(draft.trim().takeIf { it.isNotBlank() }) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
            ) { Text("完成", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
