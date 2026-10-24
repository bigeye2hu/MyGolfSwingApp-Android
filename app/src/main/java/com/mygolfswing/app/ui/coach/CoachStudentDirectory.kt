package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachConnectionActivitySnapshot
import com.mygolfswing.app.data.coach.V2CoachDashboard
import com.mygolfswing.app.data.coach.V2CoachStudentAge
import com.mygolfswing.app.data.coach.coachStudentConversationPreview
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.interaction.workspaceFeedDate

private enum class CoachStudentDirectoryFilter(val label: String) {
    ALL("全部"),
    REVIEW("求点评"),
    FRESH("有新互动"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoachStudentDirectory(
    connections: List<V2CoachConnection>,
    dashboard: V2CoachDashboard,
    loading: Boolean,
    error: String?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onInvite: () -> Unit,
    onOpen: (V2CoachConnection) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(CoachStudentDirectoryFilter.ALL.name) }
    val filter = CoachStudentDirectoryFilter.entries.firstOrNull { it.name == filterName }
        ?: CoachStudentDirectoryFilter.ALL
    val activityById = dashboard.connectionStates.associateBy { it.connectionId }
    val teaching = connections.filter { it.relationshipType != "friend" }
    val active = teaching.filter { it.status == "active" }
    val history = teaching.filter { it.status != "active" }
    fun pendingCount(connection: V2CoachConnection): Int {
        val activity = activityById[connection.connectionId]
        return activity?.pendingCount ?: dashboard.pendingByConnection[connection.connectionId] ?: 0
    }
    fun freshCount(connection: V2CoachConnection): Int {
        val activity = activityById[connection.connectionId]
        return activity?.newCount ?: dashboard.newByConnection[connection.connectionId] ?: 0
    }
    fun matches(connection: V2CoachConnection): Boolean {
        val needle = query.trim()
        val person = connection.student
        val textMatches = needle.isEmpty() || listOfNotNull(
            person?.nickname,
            connection.privateNote,
        ).any { it.contains(needle, ignoreCase = true) }
        if (!textMatches) return false
        return when (filter) {
            CoachStudentDirectoryFilter.ALL -> true
            CoachStudentDirectoryFilter.REVIEW -> pendingCount(connection) > 0
            CoachStudentDirectoryFilter.FRESH -> freshCount(connection) > 0
        }
    }
    val shown = active.filter(::matches)
    val reviewTotal = active.count { pendingCount(it) > 0 }
    val freshTotal = active.count { freshCount(it) > 0 }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(CoachPaper),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth()) {
                    CoachCourseHeaderImage(CoachCourseHeader.STUDENTS, Modifier.matchParentSize())
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Transparent,
                                1f to CoachPaper,
                            ),
                        ),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("我的学员", color = CoachGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "共 ${active.size} 位学员",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                            )
                        }
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(CoachGreen)
                                .clickable(onClick = onInvite),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, "邀请学员", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color.White).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(16.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = DSColors.inkGreen,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(CoachGreen),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("搜索学员姓名或备注", color = DSColors.shadowGray, fontSize = 14.sp)
                                }
                                inner()
                            },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoachDirectoryChip(
                            "${CoachStudentDirectoryFilter.ALL.label}(${active.size})",
                            filter == CoachStudentDirectoryFilter.ALL,
                        ) { filterName = CoachStudentDirectoryFilter.ALL.name }
                        CoachDirectoryChip(
                            "${CoachStudentDirectoryFilter.REVIEW.label}($reviewTotal)",
                            filter == CoachStudentDirectoryFilter.REVIEW,
                        ) { filterName = CoachStudentDirectoryFilter.REVIEW.name }
                        CoachDirectoryChip(
                            "${CoachStudentDirectoryFilter.FRESH.label}($freshTotal)",
                            filter == CoachStudentDirectoryFilter.FRESH,
                        ) { filterName = CoachStudentDirectoryFilter.FRESH.name }
                    }
                }
                }
            }
            if (error != null && active.isNotEmpty()) {
                item {
                    Text(
                        error,
                        color = DSColors.shadowGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
            when {
                loading && active.isEmpty() -> item {
                    DirectoryStatus("正在同步学员", "已有连接会在同步完成后出现。")
                }
                error != null && active.isEmpty() -> item {
                    DirectoryStatus("学员暂时没同步下来", "已有连接和历史内容不会丢失。点按页面即可重新同步。")
                }
                active.isEmpty() -> item {
                    DirectoryStatus("还没有学员", "邀请学员之后，点一行就能进入他的教学空间。")
                }
                shown.isEmpty() -> item {
                    DirectoryStatus("没有符合条件的学员", "换个搜索词，或切回全部。")
                }
                else -> items(shown, key = { it.connectionId }) { connection ->
                    CoachStudentDirectoryRow(
                        connection = connection,
                        activity = activityById[connection.connectionId],
                        pendingCount = pendingCount(connection),
                        freshCount = freshCount(connection),
                        onClick = { onOpen(connection) },
                    )
                }
            }
            if (history.isNotEmpty()) {
                item {
                    Text(
                        if (showHistory) "收起历史连接（${history.size}）" else "历史连接（${history.size}）· 只读",
                        color = CoachGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                            .clickable { showHistory = !showHistory },
                    )
                }
                if (showHistory) {
                    items(history, key = { "history-${it.connectionId}" }) { connection ->
                        CoachStudentDirectoryRow(
                            connection = connection,
                            activity = activityById[connection.connectionId],
                            pendingCount = 0,
                            freshCount = 0,
                            historical = true,
                            onClick = { onOpen(connection) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachDirectoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else CoachGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) CoachGreen else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun DirectoryStatus(title: String, detail: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title.startsWith("正在")) {
            CircularProgressIndicator(Modifier.size(22.dp), color = CoachGreen, strokeWidth = 2.dp)
        }
        Text(title, color = CoachGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = DSColors.shadowGray, fontSize = 13.sp)
    }
}

@Composable
private fun CoachStudentDirectoryRow(
    connection: V2CoachConnection,
    activity: V2CoachConnectionActivitySnapshot?,
    pendingCount: Int,
    freshCount: Int,
    historical: Boolean = false,
    onClick: () -> Unit,
) {
    val person = connection.student
    val name = person?.nickname?.trim().orEmpty().ifBlank { "学员" }
    val age = V2CoachStudentAge.entries.firstOrNull { it.wireValue == connection.ageTag }?.title
    val preview = coachStudentConversationPreview(activity, null).trim()
    val whenText = activity?.latestActivityAt?.let(::workspaceFeedDate).orEmpty()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = person?.avatar,
            fallbackText = name,
            modifier = Modifier.size(48.dp),
            fallbackFontSize = 16.sp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    name,
                    color = DSColors.inkGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                age?.let {
                    Text(
                        it,
                        color = CoachGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (historical) {
                    Text("只读", color = DSColors.shadowGray, fontSize = 11.sp)
                }
            }
            val second = when {
                preview.isNotBlank() -> preview
                pendingCount > 0 -> "有练习等待点评"
                else -> ""
            }
            if (second.isNotBlank() || whenText.isNotBlank()) {
                Text(
                    listOf(second, whenText).filter { it.isNotBlank() }.joinToString(" · "),
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (freshCount > 0) {
            Box(
                Modifier.height(18.dp).clip(CircleShape).background(Color(0xFFE85D4C)).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(freshCount.coerceAtMost(99).toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
