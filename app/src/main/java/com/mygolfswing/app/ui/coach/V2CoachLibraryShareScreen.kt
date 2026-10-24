package com.mygolfswing.app.ui.coach

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2LibraryShareDraft
import com.mygolfswing.app.data.coach.V2LibraryShareDraftStore
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import java.text.Collator
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun V2CoachLibraryShareDialog(
    asset: V2CoachLibraryAsset,
    connections: List<V2CoachConnection>,
    pendingByConnection: Map<String, Int>,
    ownerUserId: String,
    draftStore: V2LibraryShareDraftStore,
    connectionsLoading: Boolean,
    connectionsError: String?,
    onDismiss: () -> Unit,
    onNeedStudent: () -> Unit,
    onReloadConnections: () -> Unit,
    onDraftChanged: () -> Unit,
    onAssetLoaded: (V2CoachLibraryAsset) -> Unit,
    onSent: (List<V2CoachWorkspaceItem>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val networkConnected = rememberCoachNetworkConnected()
    val activeConnections = remember(connections) {
        connections.filter { it.status == "active" && it.relationshipType != "friend" }
    }
    val initialDraft = remember(asset.assetId, ownerUserId) {
        if (ownerUserId.isBlank()) V2LibraryShareDraft() else draftStore.load(ownerUserId, asset.assetId)
    }
    var displayedAsset by remember(asset.assetId) { mutableStateOf(asset) }
    var selected by remember(asset.assetId) {
        mutableStateOf(
            if (activeConnections.isEmpty()) initialDraft.connectionIds.take(20).toCollection(linkedSetOf())
            else initialDraft.connectionIds.filterTo(linkedSetOf()) { id ->
                activeConnections.any { it.connectionId == id }
            },
        )
    }
    var note by remember(asset.assetId) { mutableStateOf(initialDraft.note.take(500)) }
    var batchId by remember(asset.assetId) {
        mutableStateOf(initialDraft.batchId.ifBlank { UUID.randomUUID().toString() })
    }
    var restoredDraftNotice by remember(asset.assetId) { mutableStateOf(initialDraft.hasContent) }
    var draftExists by remember(asset.assetId, ownerUserId) { mutableStateOf(initialDraft.hasContent) }
    var initializing by remember(asset.assetId) { mutableStateOf(true) }
    var previewExpanded by remember(asset.assetId) { mutableStateOf(false) }
    var previewLoading by remember(asset.assetId) { mutableStateOf(false) }
    var submitting by remember(asset.assetId) { mutableStateOf(false) }
    var error by remember(asset.assetId) { mutableStateOf<String?>(null) }
    val validSelected = remember(selected, activeConnections) {
        val activeIds = activeConnections.mapTo(hashSetOf(), V2CoachConnection::connectionId)
        selected.filterTo(linkedSetOf(), activeIds::contains)
    }

    LaunchedEffect(asset.assetId) {
        if (activeConnections.isEmpty()) onReloadConnections()
        if (selected.isEmpty() && activeConnections.size == 1) {
            selected = linkedSetOf(activeConnections.single().connectionId)
        }
        initializing = false
        if (restoredDraftNotice) {
            delay(3_000)
            restoredDraftNotice = false
        }
    }

    LaunchedEffect(activeConnections.map(V2CoachConnection::connectionId)) {
        if (activeConnections.isEmpty()) return@LaunchedEffect
        val validIds = activeConnections.mapTo(hashSetOf(), V2CoachConnection::connectionId)
        selected = selected.filterTo(linkedSetOf(), validIds::contains)
        if (selected.isEmpty() && activeConnections.size == 1) {
            selected = linkedSetOf(activeConnections.single().connectionId)
        }
    }

    LaunchedEffect(selected, note, batchId, initializing, ownerUserId) {
        if (initializing || ownerUserId.isBlank()) return@LaunchedEffect
        val shouldPersist = shouldPersistLibraryShareDraft(
            note = note,
            selectedConnectionIds = selected,
            connectionCount = activeConnections.size,
            draftExists = draftExists,
        )
        val hadDraft = draftExists
        draftStore.save(
            ownerUserId = ownerUserId,
            assetId = asset.assetId,
            draft = if (shouldPersist) {
                V2LibraryShareDraft(
                    connectionIds = selected.toList(),
                    note = note,
                    batchId = batchId,
                )
            } else {
                V2LibraryShareDraft()
            },
        )
        draftExists = shouldPersist
        if (hadDraft != shouldPersist) onDraftChanged()
    }

    fun prepareMutation() {
        if (error != null) {
            error = null
            batchId = UUID.randomUUID().toString()
        }
    }

    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2CoachLibraryShareContent(
            asset = displayedAsset,
            connections = activeConnections,
            pendingByConnection = pendingByConnection,
            selectedConnectionIds = validSelected,
            note = note,
            restoredDraftNotice = restoredDraftNotice,
            previewExpanded = previewExpanded,
            previewLoading = previewLoading,
            submitting = submitting,
            error = error,
            networkConnected = networkConnected,
            connectionsLoading = connectionsLoading,
            connectionsError = connectionsError,
            onClose = onDismiss,
            onPreviewToggle = {
                if (previewExpanded) {
                    previewExpanded = false
                } else if (!displayedAsset.videoUrl.isNullOrBlank()) {
                    previewExpanded = true
                } else if (!previewLoading) {
                    previewLoading = true
                    scope.launch {
                        try {
                            ServiceLocator.coachWorkspaceRepo.coachAsset(displayedAsset.assetId)
                                .onSuccess { loaded ->
                                    displayedAsset = loaded
                                    onAssetLoaded(loaded)
                                    previewExpanded = !loaded.videoUrl.isNullOrBlank()
                                }
                        } finally {
                            previewLoading = false
                        }
                    }
                }
            },
            onNoteChange = { value ->
                prepareMutation()
                note = value.take(500)
            },
            onSelectionChange = { updated ->
                prepareMutation()
                selected = updated.take(20).toCollection(linkedSetOf())
            },
            onNeedStudent = {
                onDismiss()
                scope.launch {
                    delay(320)
                    onNeedStudent()
                }
            },
            onReloadConnections = onReloadConnections,
            onSend = {
                if (!submitting && validSelected.isNotEmpty() && networkConnected) {
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            ServiceLocator.coachWorkspaceRepo.shareLibraryAsset(
                                assetId = displayedAsset.assetId,
                                connectionIds = validSelected.toList(),
                                title = displayedAsset.title,
                                text = note,
                                clientRequestId = "library_share_${displayedAsset.assetId}_$batchId",
                            ).onSuccess { sentItems ->
                                if (ownerUserId.isNotBlank()) {
                                    val hadDraft = draftExists
                                    draftStore.clear(ownerUserId, displayedAsset.assetId)
                                    draftExists = false
                                    if (hadDraft) onDraftChanged()
                                }
                                onSent(sentItems)
                                onDismiss()
                            }.onFailure {
                                if (ownerUserId.isNotBlank()) {
                                    val hadDraft = draftExists
                                    draftStore.save(
                                        ownerUserId = ownerUserId,
                                        assetId = displayedAsset.assetId,
                                        draft = V2LibraryShareDraft(
                                            connectionIds = validSelected.toList(),
                                            note = note,
                                            batchId = batchId,
                                        ),
                                    )
                                    draftExists = true
                                    if (!hadDraft) onDraftChanged()
                                }
                                error = "发送暂停，选择和留言已保留"
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            submitting = false
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun V2CoachLibraryShareContent(
    asset: V2CoachLibraryAsset,
    connections: List<V2CoachConnection>,
    pendingByConnection: Map<String, Int>,
    selectedConnectionIds: Set<String>,
    note: String,
    restoredDraftNotice: Boolean,
    previewExpanded: Boolean,
    previewLoading: Boolean,
    submitting: Boolean,
    error: String?,
    networkConnected: Boolean,
    connectionsLoading: Boolean,
    connectionsError: String?,
    onClose: () -> Unit,
    onPreviewToggle: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
    onNeedStudent: () -> Unit,
    onReloadConnections: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var previewStarted by remember(asset.assetId) { mutableStateOf(false) }
    val visibleConnections = remember(connections, pendingByConnection, query) {
        coachLibraryShareVisibleConnections(connections, pendingByConnection, query)
    }
    val visibleIds = visibleConnections.mapTo(linkedSetOf(), V2CoachConnection::connectionId)
    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all(selectedConnectionIds::contains)
    val selectedNames = connections.associateBy(V2CoachConnection::connectionId)
    val singleName = selectedConnectionIds.singleOrNull()?.let { selectedNames[it]?.student?.nickname }
    val sendTitle = when {
        submitting -> "正在发送…"
        selectedConnectionIds.isEmpty() -> "选择学员后发送"
        singleName != null -> "发送给 $singleName"
        else -> "发送给 ${selectedConnectionIds.size} 位学员"
    }
    val canSend = selectedConnectionIds.isNotEmpty() && !submitting && networkConnected

    LaunchedEffect(previewExpanded) {
        if (!previewExpanded) previewStarted = false
    }

    Box(modifier.fillMaxSize().background(DSColors.warmWhite).imePadding()) {
        V2RoomBackground(Modifier.fillMaxSize())
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 126.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(48.dp)) {
                    Text(
                        "发送给学员",
                        color = DSColors.inkGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).testTag("coach-library-share-title"),
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd).size(36.dp).clip(CircleShape)
                            .background(DSColors.cardWhite).clickable(enabled = !submitting, onClick = onClose)
                            .testTag("coach-library-share-close"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭", tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp)).padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (previewExpanded && !asset.videoUrl.isNullOrBlank()) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(coachLibraryShareDisplayAspectRatio(asset))
                                .clip(RoundedCornerShape(14.dp)).background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            CoachLibraryInlineVideo(
                                asset.videoUrl,
                                playWhenReady = previewStarted,
                                useController = previewStarted,
                            )
                            if (!previewStarted) {
                                V2CoachMediaCover(asset, Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f)))))
                                Box(
                                    Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.94f))
                                        .clickable { previewStarted = true }
                                        .testTag("coach-library-share-inline-play"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        "播放${asset.title.ifBlank { "教学素材" }}",
                                        tint = DSColors.deepGreen,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(
                            Modifier.size(width = 78.dp, height = 64.dp).clip(RoundedCornerShape(14.dp))
                                .background(DSColors.deepGreen).clickable(enabled = !previewLoading, onClick = onPreviewToggle)
                                .testTag("coach-library-share-preview"),
                            contentAlignment = Alignment.Center,
                        ) {
                            V2CoachMediaCover(asset, Modifier.fillMaxSize())
                            if (previewLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            else Icon(
                                if (previewExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.PlayArrow,
                                if (previewExpanded) "收起素材预览" else "预览要发送的素材",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                asset.title.ifBlank { "教学素材" },
                                color = DSColors.inkGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("时长 ${coachLibraryShareDuration(asset.durationSeconds)}", color = DSColors.shadowGray, fontSize = 10.sp)
                        }
                    }
                }
            }
            if (restoredDraftNotice) {
                item {
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(Icons.Filled.History, null, tint = DSColors.deepGreen, modifier = Modifier.size(15.dp))
                        Text("已恢复上次选择的学员和留言", color = DSColors.deepGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("可选附言", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    BasicTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        enabled = !submitting,
                        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 13.sp, lineHeight = 18.sp),
                        cursorBrush = SolidColor(DSColors.fairway),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(14.dp))
                                    .background(DSColors.cardWhite)
                                    .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp)).padding(14.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                if (note.isEmpty()) Text("给学员留句话（选填）", color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 13.sp)
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("coach-library-share-note"),
                    )
                }
            }
            item {
                CoachLibraryRecipientHeader(
                    connectionCount = connections.size,
                    selectedCount = selectedConnectionIds.size,
                    allVisibleSelected = allVisibleSelected,
                    onToggleAll = {
                        onSelectionChange(
                            if (allVisibleSelected) selectedConnectionIds - visibleIds
                            else (selectedConnectionIds + visibleIds).take(20).toSet(),
                        )
                    },
                )
            }
            if (connections.size > 5) {
                item {
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(DSColors.warmWhite).border(1.dp, DSColors.borderLine, RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(14.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 12.sp),
                            cursorBrush = SolidColor(DSColors.fairway),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (query.isEmpty()) Text("搜索学员", color = DSColors.shadowGray, fontSize = 12.sp)
                                    inner()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("coach-library-share-search"),
                        )
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Cancel,
                                "清除搜索",
                                tint = DSColors.shadowGray,
                                modifier = Modifier.size(16.dp).clickable { query = "" },
                            )
                        }
                    }
                }
            }
            when {
                connections.isEmpty() -> item {
                    CoachLibraryShareNoStudents(
                        loading = connectionsLoading,
                        error = connectionsError,
                        onRetry = onReloadConnections,
                        onNeedStudent = onNeedStudent,
                    )
                }
                visibleConnections.isEmpty() -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.PersonSearch, null, tint = DSColors.shadowGray, modifier = Modifier.size(21.dp))
                        Text("没有找到这位学员", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "清除搜索",
                            color = DSColors.fairway,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { query = "" }.padding(5.dp),
                        )
                    }
                }
                connections.size == 1 -> item {
                    CoachLibrarySingleStudentRow(connections.single())
                }
                else -> items(visibleConnections, key = V2CoachConnection::connectionId) { connection ->
                    CoachLibraryStudentRow(
                        connection = connection,
                        pendingCount = pendingByConnection[connection.connectionId] ?: 0,
                        selected = connection.connectionId in selectedConnectionIds,
                        enabled = !submitting,
                        onClick = {
                            val updated = if (connection.connectionId in selectedConnectionIds) {
                                selectedConnectionIds - connection.connectionId
                            } else if (selectedConnectionIds.size < 20) {
                                selectedConnectionIds + connection.connectionId
                            } else {
                                selectedConnectionIds
                            }
                            onSelectionChange(updated)
                        },
                    )
                }
            }
            if (error != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(V2CoachPalette.mutedGold.copy(alpha = 0.58f))
                            .border(1.dp, V2CoachPalette.gold.copy(alpha = 0.26f), RoundedCornerShape(14.dp))
                            .padding(11.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = V2CoachPalette.gold, modifier = Modifier.size(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("发送暂停，选择和留言已保留", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("素材还没有送达，对方暂时看不到；可以直接继续发送", color = DSColors.shadowGray, fontSize = 10.sp)
                        }
                        Text(
                            "继续发送",
                            color = DSColors.fairway,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.92f))
                                .clickable(enabled = canSend, onClick = onSend).padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("coach-library-share-retry"),
                        )
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White.copy(alpha = 0.94f))
                .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (!networkConnected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.WifiOff, null, tint = DSColors.shadowGray, modifier = Modifier.size(13.dp))
                    Text("当前离线，选择和留言已保留", color = DSColors.shadowGray, fontSize = 10.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth().height(50.dp).alpha(if (canSend) 1f else 0.5f)
                    .clip(RoundedCornerShape(14.dp)).background(DSColors.fairway)
                    .clickable(enabled = canSend, onClick = onSend).testTag("coach-library-share-send"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(sendTitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoachLibraryRecipientHeader(
    connectionCount: Int,
    selectedCount: Int,
    allVisibleSelected: Boolean,
    onToggleAll: () -> Unit,
) {
    if (connectionCount == 1) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("选择学员", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                if (selectedCount == 0) "最多可同时发给20位" else "已选 $selectedCount 位",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
            )
        }
        if (connectionCount > 0) {
            Text(
                if (allVisibleSelected) "取消这批" else "全选",
                color = DSColors.fairway,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onToggleAll).padding(8.dp).testTag("coach-library-share-select-all"),
            )
        }
    }
}

@Composable
private fun CoachLibraryShareNoStudents(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onNeedStudent: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            loading -> {
                CircularProgressIndicator(Modifier.size(22.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                Text("正在同步学员…", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            error != null -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Filled.WifiOff, null, tint = DSColors.inkGreen, modifier = Modifier.size(16.dp))
                    Text("学员列表暂时没有同步成功", color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(error, color = DSColors.shadowGray, fontSize = 10.sp, maxLines = 2)
                CoachLibrarySmallAction("重新同步", Icons.Filled.Refresh, onRetry)
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Filled.PersonOff, null, tint = DSColors.shadowGray, modifier = Modifier.size(16.dp))
                    Text("还没有可发送的学员", color = DSColors.shadowGray, fontSize = 12.sp)
                }
                CoachLibrarySmallAction("添加学员", Icons.Filled.Add, onNeedStudent)
            }
        }
    }
}

@Composable
private fun CoachLibrarySmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(DSColors.fairway).clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoachLibrarySingleStudentRow(connection: V2CoachConnection) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(15.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.64f))
            .border(1.dp, DSColors.fairway.copy(alpha = 0.32f), RoundedCornerShape(15.dp)).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        V2UserAvatar(
            avatar = connection.student?.avatar,
            fallbackText = connection.student?.nickname,
            modifier = Modifier.size(40.dp).border(1.dp, V2CoachPalette.gold, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(connection.student?.nickname ?: "挥呗学员", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("一对一私有教学空间", color = DSColors.shadowGray, fontSize = 9.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text("将发送", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CoachLibraryStudentRow(
    connection: V2CoachConnection,
    pendingCount: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(15.dp))
            .background(if (selected) DSColors.mistGreen.copy(alpha = 0.72f) else DSColors.warmWhite)
            .border(
                1.dp,
                if (selected) DSColors.fairway.copy(alpha = 0.45f) else DSColors.borderLine,
                RoundedCornerShape(15.dp),
            )
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp)
            .testTag("coach-library-share-student-${connection.connectionId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        V2UserAvatar(
            avatar = connection.student?.avatar,
            fallbackText = connection.student?.nickname,
            modifier = Modifier.size(38.dp).border(1.dp, V2CoachPalette.gold, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(connection.student?.nickname ?: "挥呗学员", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (pendingCount > 0) "还有 $pendingCount 条内容待回复" else "教学空间已连接",
                color = if (pendingCount > 0) V2CoachPalette.gold else DSColors.shadowGray,
                fontSize = 9.sp,
            )
        }
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .then(
                    if (selected) Modifier.background(DSColors.fairway)
                    else Modifier.border(1.5.dp, DSColors.shadowGray.copy(alpha = 0.5f), CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun coachLibraryShareDuration(seconds: Double?): String {
    val total = seconds?.takeIf { it.isFinite() && it > 0 }?.toInt() ?: 0
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
}

internal fun coachLibraryShareAspectRatio(asset: V2CoachLibraryAsset): Float {
    val width = asset.width?.takeIf { it > 0 } ?: return 16f / 9f
    val height = asset.height?.takeIf { it > 0 } ?: return 16f / 9f
    val ratio = width.toFloat() / height.toFloat()
    return when {
        ratio < 0.9f -> 4f / 5f
        ratio < 1.1f -> 1f
        else -> 16f / 9f
    }
}

internal fun coachLibraryShareDisplayAspectRatio(asset: V2CoachLibraryAsset): Float =
    maxOf(coachLibraryShareAspectRatio(asset), 1.25f)

internal fun coachLibraryShareVisibleConnections(
    connections: List<V2CoachConnection>,
    pendingByConnection: Map<String, Int>,
    query: String,
): List<V2CoachConnection> {
    val keyword = query.trim()
    val collator = Collator.getInstance(Locale.CHINA)
    return connections.asSequence()
        .filter { connection ->
            keyword.isEmpty() ||
                connection.student?.nickname.orEmpty().contains(keyword, ignoreCase = true) ||
                connection.student?.userId.orEmpty().contains(keyword, ignoreCase = true)
        }
        .sortedWith { left, right ->
            val pendingOrder = (pendingByConnection[right.connectionId] ?: 0)
                .compareTo(pendingByConnection[left.connectionId] ?: 0)
            if (pendingOrder != 0) pendingOrder
            else collator.compare(left.student?.nickname.orEmpty(), right.student?.nickname.orEmpty())
        }
        .toList()
}

internal fun shouldPersistLibraryShareDraft(
    note: String,
    selectedConnectionIds: Set<String>,
    connectionCount: Int,
    draftExists: Boolean,
): Boolean = note.isNotBlank() ||
    (selectedConnectionIds.isNotEmpty() && (connectionCount > 1 || draftExists))

@Composable
private fun rememberCoachNetworkConnected(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    var connected by remember(manager) { mutableStateOf(manager?.coachNetworkAvailable() ?: true) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connected = manager?.coachNetworkAvailable() ?: true
            }

            override fun onLost(network: Network) {
                connected = manager?.coachNetworkAvailable() ?: false
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val registered = manager?.let { runCatching { it.registerDefaultNetworkCallback(callback) }.isSuccess } == true
        onDispose {
            if (registered) runCatching { manager?.unregisterNetworkCallback(callback) }
        }
    }
    return connected
}

private fun ConnectivityManager.coachNetworkAvailable(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
