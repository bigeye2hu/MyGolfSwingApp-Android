package com.mygolfswing.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2ProfileReflector
import com.mygolfswing.app.data.ai.profileReflectionFailureResult
import com.mygolfswing.app.data.model.V2GolfProfileData
import com.mygolfswing.app.data.model.V2ProfileDates
import com.mygolfswing.app.data.model.V2ProfileItem
import com.mygolfswing.app.data.model.V2ProfileSource
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    reflectProfile: suspend (java.util.UUID?) -> V2ProfileReflector.Result = V2ProfileReflector::reflectAll,
) {
    val accountProfile by V2ProfileStore.profile.collectAsState()
    val profile by V2GolfProfileStore.profile.collectAsState()
    val isEmpty = aiArchiveIsProfileEmpty(profile)
    var isSyncing by remember { mutableStateOf(false) }
    var syncToast by remember { mutableStateOf<String?>(null) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addingSection by rememberSaveable { mutableStateOf("") }
    var newItemText by rememberSaveable { mutableStateOf("") }
    var newItemSource by rememberSaveable { mutableStateOf(V2ProfileSource.USER_MANUAL) }
    val scope = rememberCoroutineScope()
    var manualSyncJob by remember { mutableStateOf<Job?>(null) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(accountProfile.isLoggedIn) {
        if (accountProfile.isLoggedIn) {
            V2GolfProfileStore.reloadFromLocal()
            V2GolfProfileStore.refreshFromCloud()
        } else {
            manualSyncJob?.cancel()
            manualSyncJob = null
            isSyncing = false
            syncToast = null
            showAddSheet = false
            addingSection = ""
            newItemText = ""
        }
    }

    DisposableEffect(Unit) {
        onDispose { manualSyncJob?.cancel() }
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
        V2RoomBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            AIArchiveTopBar(onDone = onBack)
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
                            if (isSyncing || !accountProfile.isLoggedIn) return@ManualSyncCard
                            manualSyncJob?.cancel()
                            manualSyncJob = scope.launch {
                                isSyncing = true
                                val cid = V2AIChatStore.currentSession.value.id
                                val result = try {
                                    reflectProfile(cid)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    profileReflectionFailureResult(e)
                                } finally {
                                    isSyncing = false
                                    manualSyncJob = null
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

                    ComingSoonSection(
                        title = "训练目标",
                        icon = Icons.Default.Flag,
                        accent = DSColors.fairway,
                        hint = "AI 训练目标功能待开放",
                    )

                    ComingSoonSection(
                        title = "教练指导",
                        icon = Icons.Default.Person,
                        accent = DSColors.deepGreen,
                        hint = "教练协作功能待开放",
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
                        .testTag("ai-archive-sync-toast")
                        .background(DSColors.inkGreen.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    if (showAddSheet && accountProfile.isLoggedIn) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
            containerColor = DSColors.warmWhite,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(DSColors.warmWhite)
                    .testTag("ai-archive-add-sheet"),
            ) {
                V2RoomBackground(Modifier.fillMaxSize())
                AddItemSheetContent(
                    section = addingSection,
                    text = newItemText,
                    source = newItemSource,
                    onTextChange = { newItemText = it },
                    onSourceChange = { newItemSource = it },
                    onCancel = { showAddSheet = false },
                    onConfirm = confirm@{
                        if (!accountProfile.isLoggedIn) {
                            showAddSheet = false
                            return@confirm
                        }
                        val trimmed = newItemText.trim()
                        if (trimmed.isNotEmpty()) {
                            V2GolfProfileStore.addItem(addingSection, trimmed, newItemSource)
                            showAddSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AIArchiveTopBar(onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .testTag("ai-archive-top-bar"),
    ) {
        Text(
            text = "AI 训练档案",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = DSSpacing.sm),
        ) {
            Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun lastSyncHintText(profile: V2GolfProfileData): String? {
    if (aiArchiveIsProfileEmpty(profile)) return null
    val lastSync = V2ProfileDates.parseIsoOrNull(profile.summaryUpdatedAt) ?: return null
    val conv = V2AIChatStore.currentSession.value
    val hasNew = aiArchiveConversationHasNewMessages(
        hasMessages = conv.messages.isNotEmpty(),
        conversationUpdatedAt = conv.updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant(),
        lastSync = lastSync,
    )
    if (hasNew) return null
    val relative = V2ProfileDates.relativeLabel(profile.summaryUpdatedAt) ?: return null
    return "上次同步：$relative · 暂无新对话"
}

internal fun aiArchiveConversationHasNewMessages(
    hasMessages: Boolean,
    conversationUpdatedAt: Instant,
    lastSync: Instant,
): Boolean = hasMessages && conversationUpdatedAt.isAfter(lastSync)

internal fun aiArchiveIsProfileEmpty(profile: V2GolfProfileData): Boolean =
    profile.currentFocus.isEmpty() &&
        profile.painPoints.isEmpty() &&
        profile.strengths.isEmpty() &&
        profile.trainingGoals.isEmpty() &&
        profile.coachNotes.isEmpty()

internal fun aiArchiveSyncError(base: String, debugDetail: String?): String =
    aiArchiveSyncError(base, debugDetail, ServiceLocator.isDebuggable)

internal fun aiArchiveSyncError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
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
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "添加$title",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(22.dp),
                    )
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
private fun ComingSoonSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    hint: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.6f))
            .drawBehind {
                drawRoundRect(
                    color = DSColors.shadowGray.copy(alpha = 0.15f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DSColors.inkGreen.copy(alpha = 0.75f),
                )
                Text(
                    text = "待开放",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.shadowGray,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DSColors.mistGreen)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                text = hint,
                fontSize = 11.sp,
                color = DSColors.shadowGray,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
internal fun AddItemSheetContent(
    section: String,
    text: String,
    source: V2ProfileSource,
    onTextChange: (String) -> Unit,
    onSourceChange: (V2ProfileSource) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionName = V2GolfProfileData.sectionNames[section] ?: section
    val showSourcePicker = section in listOf("currentFocus", "painPoints", "strengths")
    val canConfirm = text.trim().isNotEmpty()
    Column(
        modifier
            .imePadding()
            .v2NavigationBarPadding()
            .padding(DSSpacing.lg)
            .padding(bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("取消", color = DSColors.shadowGray) }
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(
                    "添加",
                    color = if (canConfirm) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
                    fontWeight = FontWeight.SemiBold,
                )
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
