package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors

private enum class CoachSwitcherFilter { ALL, REVIEW, FRESH }

@Composable
internal fun CoachStudentSwitcherDrawer(
    connections: List<V2CoachConnection>,
    currentConnectionId: String,
    workspaceItems: List<V2CoachWorkspaceItem>,
    onDismiss: () -> Unit,
    onSelect: (V2CoachConnection) -> Unit,
) {
    val teaching = connections.filter { it.relationshipType != "friend" && it.status == "active" }
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(CoachSwitcherFilter.ALL.name) }
    val filter = CoachSwitcherFilter.entries.firstOrNull { it.name == filterName } ?: CoachSwitcherFilter.ALL
    fun roots(connectionId: String) = workspaceItems.filter {
        it.connectionId == connectionId && it.parentItemId == null && !it.isRemovedContent
    }
    fun wantsReview(connectionId: String) = roots(connectionId).any {
        it.requiresCoachReview == true && it.reviewState != "responded"
    }
    fun hasFresh(connectionId: String) = roots(connectionId).any {
        it.latestReplySenderRole == "student" && it.reviewState != "responded"
    }
    val shown = teaching.filter { connection ->
        val needle = query.trim()
        val named = needle.isEmpty() || listOfNotNull(connection.student?.nickname, connection.privateNote)
            .any { it.contains(needle, ignoreCase = true) }
        named && when (filter) {
            CoachSwitcherFilter.ALL -> true
            CoachSwitcherFilter.REVIEW -> wantsReview(connection.connectionId)
            CoachSwitcherFilter.FRESH -> hasFresh(connection.connectionId)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onDismiss)) {
            Column(
                Modifier.fillMaxHeight().width(320.dp).background(CoachPaper).clickable(onClick = {}),
            ) {
                Box(Modifier.fillMaxWidth().height(132.dp)) {
                    CoachCourseHeaderImage(CoachCourseHeader.STUDENTS, Modifier.fillMaxSize())
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, top = 48.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("切换学员", color = CoachGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.86f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, "关闭", tint = CoachGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(19.dp))
                            .background(Color.White).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
                            cursorBrush = SolidColor(CoachGreen),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("搜索学员", color = DSColors.shadowGray, fontSize = 14.sp)
                                inner()
                            },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SwitcherChip("全部(${teaching.size})", filter == CoachSwitcherFilter.ALL) {
                            filterName = CoachSwitcherFilter.ALL.name
                        }
                        SwitcherChip("求点评", filter == CoachSwitcherFilter.REVIEW) {
                            filterName = CoachSwitcherFilter.REVIEW.name
                        }
                        SwitcherChip("有新互动", filter == CoachSwitcherFilter.FRESH) {
                            filterName = CoachSwitcherFilter.FRESH.name
                        }
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(shown, key = { it.connectionId }) { connection ->
                        val current = connection.connectionId == currentConnectionId
                        val person = connection.student
                        val name = person?.nickname?.trim().orEmpty().ifBlank { "学员" }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (!current) onSelect(connection)
                                else onDismiss()
                            }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            V2UserAvatar(person?.avatar, Modifier.size(42.dp), fallbackText = name, fallbackFontSize = 15.sp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    name,
                                    color = DSColors.inkGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val status = when {
                                    wantsReview(connection.connectionId) -> "求点评"
                                    hasFresh(connection.connectionId) -> "有新回复"
                                    else -> null
                                }
                                status?.let {
                                    Text(it, color = CoachGreen, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            if (current) {
                                Icon(Icons.Filled.Check, "当前学员", tint = CoachGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitcherChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else CoachGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) CoachGreen else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
