package com.mygolfswing.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2ProfileReflector
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2GolfProfileData
import com.mygolfswing.app.data.model.V2ProfileDates
import com.mygolfswing.app.data.model.V2ProfileItem
import com.mygolfswing.app.data.model.V2ProfileSource
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * V2「AI 训练档案」页（对齐 iOS `V2AIArchiveView`）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AIArchiveScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit = {},
) {
    val accountProfile by V2ProfileStore.profile.collectAsState()
    val profile by V2GolfProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val isEmpty = V2GolfProfileStore.isProfileEmpty
    var isSyncing by remember { mutableStateOf(false) }
    var syncToast by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var addingSection by remember { mutableStateOf("") }
    var newItemText by remember { mutableStateOf("") }
    var newItemSource by remember { mutableStateOf(V2ProfileSource.USER_MANUAL) }
    val scope = rememberCoroutineScope()
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(accountProfile.isLoggedIn) {
        if (accountProfile.isLoggedIn) {
            V2GolfProfileStore.reloadFromLocal()
            V2GolfProfileStore.refreshFromCloud()
        }
    }

    LaunchedEffect(syncToast) {
        if (syncToast != null) {
            delay(2500)
            syncToast = null
        }
    }

    fun showToast(text: String) {
        syncToast = text
    }

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(Modifier.fillMaxSize()) {
            V2NavTopBar(
                title = "AI 训练档案",
                onBack = onBack,
                trailing = {
                    TextButton(onClick = onBack) {
                        Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg)
                    .padding(top = DSSpacing.md, bottom = DSSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                if (!accountProfile.isLoggedIn) {
                    ArchiveLoginRequiredCard(onLogin = onRequestLogin)
                } else {
                    if (isEmpty) EmptyHeaderCard()

                    ManualSyncCard(
                        isSyncing = isSyncing,
                        lastSyncHint = lastSyncHintText(profile),
                        onSync = {
                            if (isSyncing) return@ManualSyncCard
                            scope.launch {
                                isSyncing = true
                                val cid = V2AIChatStore.currentSession.value.id
                                val result = try {
                                    V2ProfileReflector.reflectAll(cid)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    V2ProfileReflector.Result.Failed(e.message ?: "未知错误")
                                } finally {
                                    isSyncing = false
                                }
                                when (result) {
                                    is V2ProfileReflector.Result.Updated -> {
                                        val msg = when {
                                            result.itemsAdded > 0 && result.summaryChanged ->
                                                "已更新 ${result.itemsAdded} 项 + 状态总结"
                                            result.itemsAdded > 0 -> "已更新 ${result.itemsAdded} 项"
                                            else -> "已更新 状态总结"
                                        }
                                        showToast(msg)
                                    }
                                    V2ProfileReflector.Result.NoChange ->
                                        showToast("AI 看了一遍，暂无新内容可提取")
                                    V2ProfileReflector.Result.Empty ->
                                        showToast("还没有对话 / 笔记 / 目标可分析，先去用一会儿")
                                    V2ProfileReflector.Result.Skipped ->
                                        showToast("上一次同步还在跑，请稍等")
                                    is V2ProfileReflector.Result.Failed ->
                                        showToast(aiArchiveSyncError("同步失败，请稍后重试", result.message))
                                }
                            }
                        },
                    )

                    StatusSummarySection(profile)

                    ProfileItemSection(
                        title = "训练重点",
                        icon = Icons.Default.TrackChanges,
                        accent = DSColors.fairway,
                        section = "currentFocus",
                        items = profile.currentFocus,
                        onAdd = {
                            addingSection = "currentFocus"
                            newItemText = ""
                            newItemSource = V2ProfileSource.USER_MANUAL
                            showAddSheet = true
                        },
                        onRemove = { V2GolfProfileStore.removeItem("currentFocus", it) },
                    )

                    ProfileItemSection(
                        title = "主要痛点",
                        icon = Icons.Default.Warning,
                        accent = DSColors.flagRed,
                        section = "painPoints",
                        items = profile.painPoints,
                        onAdd = {
                            addingSection = "painPoints"
                            newItemText = ""
                            newItemSource = V2ProfileSource.USER_MANUAL
                            showAddSheet = true
                        },
                        onRemove = { V2GolfProfileStore.removeItem("painPoints", it) },
                    )

                    ProfileItemSection(
                        title = "优势项",
                        icon = Icons.Default.Star,
                        accent = DSColors.trophyGold,
                        section = "strengths",
                        items = profile.strengths,
                        onAdd = {
                            addingSection = "strengths"
                            newItemText = ""
                            newItemSource = V2ProfileSource.USER_MANUAL
                            showAddSheet = true
                        },
                        onRemove = { V2GolfProfileStore.removeItem("strengths", it) },
                    )

                    TrainingGoalsSection(goals = goals)

                    ProfileStringSection(
                        title = "教练指导",
                        icon = Icons.Default.Person,
                        accent = DSColors.deepGreen,
                        section = "coachNotes",
                        items = profile.coachNotes,
                        emptyText = "记录教练给你的关键要求后，AI 回答会一起参考。",
                        onAdd = {
                            addingSection = "coachNotes"
                            newItemText = ""
                            newItemSource = V2ProfileSource.COACH_ADVICE
                            showAddSheet = true
                        },
                        onRemove = { V2GolfProfileStore.removeItem("coachNotes", it) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = syncToast != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit = fadeOut(),
        ) {
            syncToast?.let { text ->
                Text(
                    text,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(DSColors.inkGreen.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
            containerColor = DSColors.warmWhite,
        ) {
            AddItemSheetContent(
                section = addingSection,
                text = newItemText,
                source = newItemSource,
                onTextChange = { newItemText = it },
                onSourceChange = { newItemSource = it },
                onCancel = { showAddSheet = false },
                onConfirm = {
                    val trimmed = newItemText.trim()
                    if (trimmed.isNotEmpty()) {
                        V2GolfProfileStore.addItem(addingSection, trimmed, newItemSource)
                        showAddSheet = false
                    }
                },
            )
        }
    }
}

private fun lastSyncHintText(profile: V2GolfProfileData): String? {
    if (profile.isEmpty) return null
    val lastSync = V2ProfileDates.parseIsoOrNull(profile.summaryUpdatedAt) ?: return null
    val conv = V2AIChatStore.currentSession.value
    val hasNew = conv.updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()
        .isAfter(lastSync)
    if (hasNew) return null
    val relative = V2ProfileDates.relativeLabel(profile.summaryUpdatedAt) ?: return null
    return "上次同步：$relative · 暂无新对话"
}

private fun aiArchiveSyncError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

@Composable
private fun EmptyHeaderCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(28.dp),
            )
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("档案还是空的", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DSColors.inkGreen)
        Spacer(Modifier.height(8.dp))
        Text(
            "在下方各板块点击「+」添加内容，或者和 AI 聊天时它会自动帮你填充",
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun ArchiveLoginRequiredCard(onLogin: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .background(DSColors.mistGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Memory, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
        }
        Text("登录后查看 AI 训练档案", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DSColors.inkGreen)
        Text(
            "档案会结合你的聊天、目标和 Memo 自动沉淀，需要账号态才能同步和长期保存。",
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onLogin,
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            shape = RoundedCornerShape(50),
        ) {
            Text("去登录", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ManualSyncCard(
    isSyncing: Boolean,
    lastSyncHint: String?,
    onSync: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(DSColors.mistGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("立即让 AI 同步档案", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DSColors.inkGreen)
                Text(
                    "基于最近对话提取痛点 / 训练重点 / 状态",
                    fontSize = 11.sp,
                    color = DSColors.shadowGray,
                    lineHeight = 14.sp,
                )
            }
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSyncing) DSColors.shadowGray else DSColors.fairway,
                    disabledContainerColor = DSColors.shadowGray,
                    disabledContentColor = Color.White,
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, Modifier.size(12.dp))
                }
                Spacer(Modifier.size(6.dp))
                Text(if (isSyncing) "同步中…" else "立即同步", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (lastSyncHint != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(start = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = DSColors.fairway.copy(0.7f), modifier = Modifier.size(11.dp))
                Spacer(Modifier.size(6.dp))
                Text(lastSyncHint, fontSize = 11.sp, color = DSColors.shadowGray)
            }
        }
    }
}

@Composable
private fun StatusSummarySection(profile: V2GolfProfileData) {
    val summary = profile.statusSummary?.takeIf { it.isNotBlank() } ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen)))
            .padding(DSSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(8.dp))
            Text("AI 状态总结", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.weight(1f))
            V2ProfileDates.relativeLabel(profile.summaryUpdatedAt)?.let { rel ->
                Text(rel, fontSize = 11.sp, color = Color.White.copy(0.7f))
            }
        }
        Spacer(Modifier.height(DSSpacing.sm))
        Text(summary, fontSize = 14.sp, color = Color.White.copy(0.95f), lineHeight = 20.sp)
    }
}

@Composable
private fun ProfileItemSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    section: String,
    items: List<V2ProfileItem>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val limit = V2GolfProfileData.sectionLimits[section] ?: 5
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text(
                "${items.size}/$limit",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.shadowGray,
                modifier = Modifier
                    .background(DSColors.mistGreen, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            if (items.size < limit) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.AddCircle, null, tint = DSColors.fairway, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(DSSpacing.sm))
        if (items.isEmpty()) {
            Text("暂无内容，点击 + 添加", fontSize = 12.sp, color = DSColors.shadowGray, modifier = Modifier.padding(vertical = 6.dp))
        } else {
            items.forEachIndexed { index, item ->
                ProfileItemRow(item, onRemove = { onRemove(index) })
                if (index < items.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileItemRow(item: V2ProfileItem, onRemove: () -> Unit) {
    val source = V2ProfileSource.fromWire(item.source)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.warmWhite)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(DSColors.mistGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                profileSourceIcon(source),
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(11.dp),
            )
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(item.content, fontSize = 14.sp, color = DSColors.inkGreen)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    source.displayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                    modifier = Modifier
                        .background(DSColors.mistGreen, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                V2ProfileDates.parseIsoOrNull(item.addedAt)?.let {
                    Text(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy/M/d")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(it),
                        fontSize = 10.sp,
                        color = DSColors.shadowGray,
                    )
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Cancel, null, tint = DSColors.shadowGray.copy(0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TrainingGoalsSection(goals: List<V2Goal>) {
    val activeGoals = remember(goals) {
        goals
            .filter { it.status == V2Goal.Status.ACTIVE }
            .sortedWith(
                compareByDescending<V2Goal> { it.todayFocus }
                    .thenBy { it.priority.ordinal }
                    .thenByDescending { it.updatedAt },
            )
    }
    val completedCount = remember(goals) { goals.count { it.status == V2Goal.Status.COMPLETED } }
    val visibleGoals = activeGoals.take(3)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DSColors.fairway.copy(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Star, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text("训练目标", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text(
                "${activeGoals.size} 进行中",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.fairway,
                modifier = Modifier
                    .background(DSColors.mistGreen, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(DSSpacing.sm))
        if (visibleGoals.isEmpty()) {
            Text(
                "还没有进行中的目标。创建目标后，AI 会在回答里结合你的当前训练方向。",
                fontSize = 12.sp,
                color = DSColors.shadowGray,
                lineHeight = 17.sp,
            )
        } else {
            visibleGoals.forEachIndexed { index, goal ->
                TrainingGoalRow(goal)
                if (index < visibleGoals.lastIndex) Spacer(Modifier.height(8.dp))
            }
            val hiddenCount = activeGoals.size - visibleGoals.size
            if (hiddenCount > 0 || completedCount > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    buildString {
                        if (hiddenCount > 0) append("还有 $hiddenCount 个进行中目标")
                        if (hiddenCount > 0 && completedCount > 0) append(" · ")
                        if (completedCount > 0) append("已完成 $completedCount 个")
                    },
                    fontSize = 11.sp,
                    color = DSColors.shadowGray,
                )
            }
        }
    }
}

@Composable
private fun TrainingGoalRow(goal: V2Goal) {
    val ratio = goal.ratio.coerceIn(0.0, 1.0).toFloat()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.warmWhite)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(
                    if (goal.todayFocus) DSColors.fairway else DSColors.shadowGray.copy(0.2f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Star,
                null,
                tint = if (goal.todayFocus) Color.White else DSColors.shadowGray,
                modifier = Modifier.size(11.dp),
            )
        }
        Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
                if (goal.todayFocus) {
                    Text(
                        "今日重点",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .background(DSColors.fairway, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(goal.typeLabel, fontSize = 10.sp, color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                goal.cadence?.displayText?.let {
                    Text(it, fontSize = 10.sp, color = DSColors.shadowGray)
                }
                if (goal.weekCheckInCount > 0) {
                    Text("本周 ${goal.weekCheckInCount} 次", fontSize = 10.sp, color = DSColors.shadowGray)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.borderLine.copy(0.6f)),
            ) {
                if (ratio > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DSColors.fairway),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.sessionProgressDisplay, fontSize = 11.sp, color = DSColors.shadowGray)
                Spacer(Modifier.weight(1f))
                Text("${(ratio * 100).toInt()}%", fontSize = 11.sp, color = DSColors.shadowGray, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileStringSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    section: String,
    items: List<String>,
    emptyText: String,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val limit = V2GolfProfileData.sectionLimits[section] ?: 5
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text(
                "${items.size}/$limit",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.shadowGray,
                modifier = Modifier
                    .background(DSColors.mistGreen, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            if (items.size < limit) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.AddCircle, null, tint = DSColors.fairway, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(DSSpacing.sm))
        if (items.isEmpty()) {
            Text(emptyText, fontSize = 12.sp, color = DSColors.shadowGray, lineHeight = 17.sp)
        } else {
            items.forEachIndexed { index, item ->
                ProfileStringRow(item = item, accent = accent, onRemove = { onRemove(index) })
                if (index < items.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileStringRow(
    item: String,
    accent: Color,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.warmWhite)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(accent.copy(0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Person, null, tint = accent, modifier = Modifier.size(11.dp))
        }
        Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item, fontSize = 14.sp, color = DSColors.inkGreen, lineHeight = 19.sp)
            Text(
                "教练建议",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .background(accent.copy(0.10f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Cancel, null, tint = DSColors.shadowGray.copy(0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddItemSheetContent(
    section: String,
    text: String,
    source: V2ProfileSource,
    onTextChange: (String) -> Unit,
    onSourceChange: (V2ProfileSource) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sectionName = V2GolfProfileData.sectionNames[section] ?: section
    val showSourcePicker = section in listOf("currentFocus", "painPoints", "strengths")
    Column(
        Modifier
            .imePadding()
            .v2NavigationBarPadding()
            .padding(DSSpacing.lg)
            .padding(bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("取消", color = DSColors.shadowGray) }
            TextButton(onClick = onConfirm, enabled = text.trim().isNotEmpty()) {
                Text("添加", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("添加到「$sectionName」", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DSColors.inkGreen)
        Spacer(Modifier.height(DSSpacing.lg))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入内容…", fontSize = 15.sp, color = DSColors.shadowGray) },
            minLines = 3,
            maxLines = 6,
            textStyle = TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DSColors.mistGreen,
                unfocusedContainerColor = DSColors.mistGreen,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        if (showSourcePicker) {
            Spacer(Modifier.height(DSSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("来源:", fontSize = 13.sp, color = DSColors.shadowGray)
                Spacer(Modifier.size(8.dp))
                listOf(V2ProfileSource.USER_MANUAL, V2ProfileSource.COACH_ADVICE).forEach { s ->
                    val selected = source == s
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) DSColors.fairway else DSColors.mistGreen)
                            .clickable { onSourceChange(s) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = profileSourceIcon(s),
                            contentDescription = null,
                            tint = if (selected) Color.White else DSColors.fairway,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            s.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else DSColors.fairway,
                        )
                    }
                }
            }
        }
    }
}

private fun profileSourceIcon(source: V2ProfileSource): ImageVector =
    when (source) {
        V2ProfileSource.AI_INFERRED -> Icons.Default.Memory
        V2ProfileSource.COACH_ADVICE -> Icons.Default.Person
        else -> Icons.Default.Edit
    }
