package com.mygolfswing.app.ui.coach

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachDashboard
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.friend.V2FriendRequestOutcome
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.interaction.AddFriendDialog
import com.mygolfswing.app.ui.interaction.V2InteractionConversationSection
import com.mygolfswing.app.ui.interaction.V2InteractionContactsDialog
import com.mygolfswing.app.ui.interaction.V2ConnectionChoiceSheet
import com.mygolfswing.app.ui.interaction.InteractionCoachCardSection
import com.mygolfswing.app.ui.interaction.rememberQueueNetworkConnected
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val COACH_STUDENT_ORDER_PREFS = "v2_interaction_coach_order"

internal fun shouldRefreshCoachStudentsAfterReconnect(previous: Boolean?, current: Boolean): Boolean =
    previous == false && current

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2CoachStudentsInteractionScreen(
    connections: List<V2CoachConnection>,
    dashboard: V2CoachDashboard,
    loadingSpaces: Boolean,
    syncError: String?,
    currentUserId: String?,
    onReloadSpaces: suspend () -> Unit,
    onInviteStudent: () -> Unit,
    onOpenStudent: (V2CoachConnection) -> Unit,
    onOpenChat: (V2IMConversation) -> Unit,
    onCommunicationCountChanged: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var conversations by remember(currentUserId) { mutableStateOf<List<V2IMConversation>>(emptyList()) }
    var friendRequestCount by remember(currentUserId) { mutableIntStateOf(0) }
    var loadingIM by remember(currentUserId) { mutableStateOf(true) }
    var imError by remember(currentUserId) { mutableStateOf<String?>(null) }
    var showConnectionOptions by rememberSaveable { mutableStateOf(false) }
    var showAddFriend by rememberSaveable { mutableStateOf(false) }
    var showContacts by rememberSaveable { mutableStateOf(false) }
    var showStudentOrder by rememberSaveable { mutableStateOf(false) }
    var connectionNotice by remember { mutableStateOf<String?>(null) }
    var orderRevision by remember { mutableIntStateOf(0) }
    var manuallyRefreshing by remember { mutableStateOf(false) }
    val networkConnected = rememberQueueNetworkConnected()
    var observedNetworkConnected by remember { mutableStateOf<Boolean?>(null) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun reloadIM(holdEmptyResult: Boolean) {
        val startedAt = SystemClock.elapsedRealtime()
        if (conversations.isEmpty()) loadingIM = true
        imError = null
        ServiceLocator.tencentFriendRepo.conversations(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { conversations = it }
            .onFailure { imError = coachThrowableError("最近对话暂时无法加载", it) }
        ServiceLocator.tencentFriendRepo.incomingFriendRequests(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { friendRequestCount = it.size }
        if (holdEmptyResult && conversations.isEmpty()) {
            val remaining = 700L - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0L) delay(remaining)
        }
        loadingIM = false
    }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) reloadIM(holdEmptyResult = true)
        else loadingIM = false
    }

    val activeStudents = remember(connections, currentUserId, orderRevision) {
        applyCoachStudentOrder(
            connections = connections.filter { it.status == "active" && it.relationshipType != "friend" },
            savedOrder = loadCoachStudentOrder(context, currentUserId),
        )
    }
    fun refreshAll() {
        if (manuallyRefreshing) return
        scope.launch {
            manuallyRefreshing = true
            try {
                onReloadSpaces()
                reloadIM(holdEmptyResult = activeStudents.isEmpty())
            } finally {
                manuallyRefreshing = false
            }
        }
    }
    LaunchedEffect(networkConnected) {
        val wasConnected = observedNetworkConnected
        observedNetworkConnected = networkConnected
        if (shouldRefreshCoachStudentsAfterReconnect(wasConnected, networkConnected)) refreshAll()
    }
    val studentByUserId = activeStudents.mapNotNull { connection ->
        connection.student?.userId?.let { it to connection }
    }.toMap()
    val orderByUserId = activeStudents.mapIndexedNotNull { index, connection ->
        connection.student?.userId?.let { it to index }
    }.toMap()
    val chatRows = remember(conversations, activeStudents) {
        val rows = conversations.map { conversation ->
            val student = studentByUserId[conversation.userId]?.student
            conversation.copy(
                title = student?.nickname ?: conversation.title,
                avatar = conversation.avatar?.takeIf(String::isNotBlank) ?: student?.avatar,
                isCoach = student != null,
            )
        }.toMutableList()
        val existing = rows.mapTo(mutableSetOf(), V2IMConversation::userId)
        activeStudents.forEach { connection ->
            val student = connection.student ?: return@forEach
            if (student.userId !in existing) {
                rows += V2IMConversation(
                    userId = student.userId,
                    title = student.nickname,
                    avatar = student.avatar,
                    preview = "已连接，可以开始聊天",
                    isCoach = true,
                )
            }
        }
        rows.sortedWith(
            compareBy<V2IMConversation> { !it.isCoach }
                .thenBy { if (it.isCoach) orderByUserId[it.userId] ?: Int.MAX_VALUE else 0 }
                .thenByDescending { it.timeEpochSeconds ?: Long.MIN_VALUE },
        )
    }
    val activityByConnection = dashboard.connectionStates.associateBy { it.connectionId }
    val workspaceUnread = activeStudents.sumOf { connection ->
        activityByConnection[connection.connectionId]?.newCount
            ?: dashboard.newByConnection[connection.connectionId]
            ?: 0
    }
    val communicationCount = workspaceUnread + conversations.sumOf(V2IMConversation::unreadCount) + friendRequestCount
    LaunchedEffect(communicationCount) { onCommunicationCountChanged(communicationCount) }

    Box(Modifier.fillMaxSize().background(CoachPaper).testTag("coach-students-pull-refresh")) {
        CoachStudentDirectory(
            connections = connections.filter { it.relationshipType != "friend" },
            dashboard = dashboard,
            loading = loadingSpaces,
            error = syncError ?: imError,
            refreshing = manuallyRefreshing,
            onRefresh = ::refreshAll,
            onInvite = { showConnectionOptions = true },
            onOpen = onOpenStudent,
        )
        connectionNotice?.let { notice ->
            Text(
                notice,
                color = CoachGreen,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp),
            )
        }
    }

    if (showConnectionOptions) {
        V2ConnectionChoiceSheet(
            primaryActionTitle = "邀请学员",
            message = "邀请学员会进入教学空间。添加好友后可以在下方聊天。",
            onPrimaryAction = { showConnectionOptions = false; onInviteStudent() },
            onAddFriend = { showConnectionOptions = false; showAddFriend = true },
            onDismiss = { showConnectionOptions = false },
        )
    }
    if (showAddFriend) {
        AddFriendDialog(
            currentUserId = currentUserId,
            onDismiss = {
                showAddFriend = false
                scope.launch { reloadIM(holdEmptyResult = false) }
            },
            onCompleted = { outcome ->
                showAddFriend = false
                connectionNotice = when (outcome) {
                    V2FriendRequestOutcome.SENT -> "好友请求已发送；对方接受后就可以在下方聊天"
                    V2FriendRequestOutcome.ALREADY_FRIENDS -> "你们已经是好友，可以在下方开始聊天"
                    V2FriendRequestOutcome.ACCEPTED -> "好友申请已接受，可以开始聊天"
                }
                scope.launch { reloadIM(holdEmptyResult = false) }
            },
        )
    }
    if (showContacts) {
        V2InteractionContactsDialog(
            currentUserId = currentUserId,
            onDismiss = {
                showContacts = false
                scope.launch { reloadIM(holdEmptyResult = false) }
            },
            onOpenChat = { conversation ->
                showContacts = false
                onOpenChat(conversation)
            },
            onChanged = { scope.launch { reloadIM(holdEmptyResult = false) } },
        )
    }
    if (showStudentOrder) {
        CoachStudentOrderDialog(
            connections = activeStudents,
            onDismiss = { showStudentOrder = false },
            onSave = { ordered ->
                saveCoachStudentOrder(context, currentUserId, ordered.map(V2CoachConnection::connectionId))
                orderRevision += 1
                showStudentOrder = false
            },
        )
    }
}

@Composable
private fun CoachInteractionStatusCard(
    title: String,
    detail: String,
    actionTitle: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.96f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(86.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(
                if (actionTitle == "重新同步") Icons.Filled.WifiOff else Icons.Filled.Groups,
                null,
                tint = DSColors.fairway.copy(alpha = 0.72f),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        actionTitle?.let {
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                shape = CircleShape,
            ) { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
internal fun CoachStudentOrderDialog(
    connections: List<V2CoachConnection>,
    onDismiss: () -> Unit,
    onSave: (List<V2CoachConnection>) -> Unit,
    coachPerspective: Boolean = true,
) {
    var ordered by remember(connections) { mutableStateOf(connections) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val reorderThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }

    fun move(connectionId: String, direction: Int): Boolean {
        val fromIndex = ordered.indexOfFirst { it.connectionId == connectionId }
        val toIndex = (fromIndex + direction).coerceIn(0, ordered.lastIndex)
        if (fromIndex < 0 || fromIndex == toIndex) return false
        ordered = moveCoachStudentOrder(ordered, fromIndex, toIndex)
        return true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier.fillMaxSize().background(DSColors.warmWhite).statusBarsPadding().navigationBarsPadding(),
        ) {
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(Color.White),
            ) {
                Text(
                    if (coachPerspective) "学员顺序" else "教练顺序",
                    color = DSColors.deepGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    "完成",
                    color = DSColors.fairway,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(12.dp))
                        .clickable { onSave(ordered) }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            ) {
                items(ordered, key = V2CoachConnection::connectionId) { connection ->
                    val currentIndex = ordered.indexOfFirst { it.connectionId == connection.connectionId }
                    val person = if (coachPerspective) connection.student else connection.coach ?: connection.peer
                    val name = person?.nickname ?: if (coachPerspective) "学员" else "挥呗教练"
                    CoachStudentOrderRow(
                        connection = connection,
                        name = name,
                        avatar = person?.avatar,
                        position = currentIndex,
                        total = ordered.size,
                        dragging = draggingId == connection.connectionId,
                        dragOffsetY = if (draggingId == connection.connectionId) dragOffsetY else 0f,
                        onMoveUp = { move(connection.connectionId, -1) },
                        onMoveDown = { move(connection.connectionId, 1) },
                        onDragStart = {
                            draggingId = connection.connectionId
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY ->
                            dragOffsetY += deltaY
                            when {
                                dragOffsetY <= -reorderThresholdPx && move(connection.connectionId, -1) -> {
                                    dragOffsetY += reorderThresholdPx
                                }
                                dragOffsetY >= reorderThresholdPx && move(connection.connectionId, 1) -> {
                                    dragOffsetY -= reorderThresholdPx
                                }
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            dragOffsetY = 0f
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachStudentOrderRow(
    connection: V2CoachConnection,
    name: String,
    avatar: String?,
    position: Int,
    total: Int,
    dragging: Boolean,
    dragOffsetY: Float,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp)
            .graphicsLayer {
                translationY = dragOffsetY
                shadowElevation = if (dragging) 12.dp.toPx() else 0f
            }
            .zIndex(if (dragging) 1f else 0f)
            .background(Color.White)
            .semantics {
                stateDescription = "第 ${position + 1} 位，共 $total 位"
                customActions = buildList {
                    if (position > 0) add(CustomAccessibilityAction("上移") { onMoveUp() })
                    if (position < total - 1) add(CustomAccessibilityAction("下移") { onMoveDown() })
                }
            }
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = avatar,
            modifier = Modifier.size(40.dp),
            fallbackText = name,
            fallbackColor = DSColors.sandBeige,
        )
        Text(
            name,
            color = DSColors.deepGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.size(width = 52.dp, height = 64.dp)
                .pointerInput(connection.connectionId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.DragHandle,
                "调整${name}顺序",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(0.5.dp).background(DSColors.borderLine))
}

internal fun <T> moveCoachStudentOrder(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
    return items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

internal fun applyCoachStudentOrder(
    connections: List<V2CoachConnection>,
    savedOrder: List<String>,
): List<V2CoachConnection> {
    val order = savedOrder.withIndex().associate { it.value to it.index }
    return connections.withIndex().sortedWith(
        compareBy<IndexedValue<V2CoachConnection>> { order[it.value.connectionId] ?: Int.MAX_VALUE }
            .thenBy { it.index },
    ).map(IndexedValue<V2CoachConnection>::value)
}

private fun coachStudentOrderKey(userId: String?): String = "student:${userId?.takeIf(String::isNotBlank) ?: "guest"}"

private fun loadCoachStudentOrder(context: Context, userId: String?): List<String> = runCatching {
    val raw = context.getSharedPreferences(COACH_STUDENT_ORDER_PREFS, Context.MODE_PRIVATE)
        .getString(coachStudentOrderKey(userId), null) ?: return@runCatching emptyList()
    val json = JSONArray(raw)
    List(json.length()) { index -> json.getString(index) }
}.getOrDefault(emptyList())

private fun saveCoachStudentOrder(context: Context, userId: String?, connectionIds: List<String>) {
    val json = JSONArray().apply { connectionIds.forEach(::put) }
    context.getSharedPreferences(COACH_STUDENT_ORDER_PREFS, Context.MODE_PRIVATE)
        .edit().putString(coachStudentOrderKey(userId), json.toString()).apply()
}
