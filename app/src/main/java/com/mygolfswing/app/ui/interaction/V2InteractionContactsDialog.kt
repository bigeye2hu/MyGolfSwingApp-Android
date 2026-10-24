package com.mygolfswing.app.ui.interaction

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.friend.V2FriendRequestOutcome
import com.mygolfswing.app.data.friend.V2FriendSearchUser
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.friend.V2IncomingFriendRequest
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.charset.Charset

@Composable
internal fun V2InteractionContactsDialog(
    currentUserId: String?,
    onDismiss: () -> Unit,
    onOpenChat: (V2IMConversation) -> Unit,
    onChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var contacts by remember(currentUserId) { mutableStateOf<List<V2FriendSearchUser>>(emptyList()) }
    var conversationByUserId by remember(currentUserId) { mutableStateOf<Map<String, V2IMConversation>>(emptyMap()) }
    var friendRequestCount by remember(currentUserId) { mutableIntStateOf(0) }
    var loading by remember(currentUserId) { mutableStateOf(true) }
    var error by remember(currentUserId) { mutableStateOf<String?>(null) }
    var showAddFriend by rememberSaveable { mutableStateOf(false) }
    var showFriendRequests by rememberSaveable { mutableStateOf(false) }
    var showBlackList by rememberSaveable { mutableStateOf(false) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun reload() {
        val session = ServiceLocator.session.current()
        if (!session.isLoggedIn) {
            contacts = emptyList()
            friendRequestCount = 0
            loading = false
            return
        }
        loading = true
        error = null
        val conversations = ServiceLocator.tencentFriendRepo.conversations(
            session = session,
            renewExpiredSession = ::renewTencentSession,
        ).getOrElse { emptyList() }
        conversationByUserId = conversations.associateBy(V2IMConversation::userId)
        ServiceLocator.tencentFriendRepo.incomingFriendRequests(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { friendRequestCount = it.size }
        ServiceLocator.tencentFriendRepo.friendUserIds(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { friendIds ->
            contacts = coroutineScope {
                friendIds.distinct().map { userId ->
                    async {
                        ServiceLocator.authRepo.friendPublicInfo(userId).getOrElse {
                            val conversation = conversationByUserId[userId]
                            V2FriendSearchUser(
                                userId = userId,
                                username = userId,
                                nickname = conversation?.title,
                                avatar = conversation?.avatar,
                            )
                        }
                    }
                }.awaitAll()
            }.sortedBy { it.displayName.lowercase() }
        }.onFailure { error = coachThrowableError("联系人暂时无法加载", it) }
        loading = false
    }

    LaunchedEffect(currentUserId) { reload() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
            V2InteractionContactsTopBar(
                onClose = onDismiss,
                onAddFriend = { showAddFriend = true },
            )
            V2InteractionContactsContent(
                contacts = contacts,
                friendRequestCount = friendRequestCount,
                loading = loading,
                error = error,
                onNewFriends = { showFriendRequests = true },
                onBlackList = { showBlackList = true },
                onRetry = { scope.launch { reload() } },
                onOpen = { contact ->
                    val conversation = conversationByUserId[contact.userId] ?: V2IMConversation(
                        userId = contact.userId,
                        title = contact.displayName,
                        avatar = contact.avatar,
                        preview = "开始聊天",
                    )
                    onOpenChat(conversation)
                },
            )
        }
    }

    if (showFriendRequests) {
        V2FriendRequestsDialog(
            onDismiss = {
                showFriendRequests = false
                scope.launch { reload() }
            },
            onChanged = onChanged,
        )
    }
    if (showBlackList) {
        V2BlackListDialog(onDismiss = { showBlackList = false })
    }
    if (showAddFriend) {
        AddFriendDialog(
            currentUserId = currentUserId,
            onDismiss = {
                showAddFriend = false
                scope.launch { reload() }
            },
            onCompleted = { _: V2FriendRequestOutcome ->
                showAddFriend = false
                onChanged()
                scope.launch { reload() }
            },
            onOpenExistingFriend = { user ->
                showAddFriend = false
                val conversation = conversationByUserId[user.userId] ?: V2IMConversation(
                    userId = user.userId,
                    title = user.displayName,
                    avatar = user.avatar,
                    preview = "开始聊天",
                )
                onOpenChat(conversation)
            },
        )
    }
}

@Composable
internal fun V2InteractionContactsTopBar(
    onClose: () -> Unit,
    onAddFriend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().v2StatusBarPadding().height(68.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContactCircleAction(
            contentDescription = "关闭联系人",
            onClick = onClose,
        ) {
            Icon(Icons.Filled.Close, null, tint = DSColors.deepGreen, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("联系人", color = DSColors.deepGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("好友和群聊", color = DSColors.shadowGray, fontSize = 12.sp)
        }
        ContactCircleAction(
            contentDescription = "添加好友",
            onClick = onAddFriend,
            filled = false,
        ) {
            Icon(Icons.Filled.PersonAdd, null, tint = DSColors.deepGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ContactCircleAction(
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .then(
                if (filled) Modifier.background(Color.White).border(1.dp, DSColors.borderLine, CircleShape)
                else Modifier,
            )
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun V2InteractionContactsContent(
    contacts: List<V2FriendSearchUser>,
    friendRequestCount: Int,
    loading: Boolean,
    error: String?,
    onNewFriends: () -> Unit,
    onBlackList: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (V2FriendSearchUser) -> Unit,
) {
    val groups = remember(contacts) { contacts.groupBy(::contactSectionTitle).toSortedMap(contactSectionComparator) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().v2NavigationBarPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ContactNavigationRow(
                title = "新的联系人",
                badge = friendRequestCount.takeIf { it > 0 },
                onClick = onNewFriends,
            )
        }
        item { ContactDivider(start = 16.dp) }
        item { ContactNavigationRow(title = "黑名单", onClick = onBlackList) }
        item { ContactDivider() }

        error?.let { message ->
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message, color = DSColors.shadowGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        "重新加载",
                        color = DSColors.fairway,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onRetry).padding(8.dp),
                    )
                }
            }
        }
        when {
            loading -> item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                    Text("正在同步联系人…", color = DSColors.shadowGray, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
            contacts.isEmpty() -> item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(62.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.GroupAdd, null, tint = DSColors.fairway, modifier = Modifier.size(28.dp))
                    }
                    Text("暂无联系人", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("点击右上角添加好友", color = DSColors.shadowGray, fontSize = 13.sp)
                }
            }
            else -> groups.forEach { (section, sectionContacts) ->
                item(key = "section-$section") {
                    Text(
                        section,
                        color = DSColors.deepGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().background(DSColors.warmWhite)
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
                items(sectionContacts, key = V2FriendSearchUser::userId) { contact ->
                    ContactFriendRow(contact = contact, onClick = { onOpen(contact) })
                    ContactDivider(start = 68.dp)
                }
            }
        }
    }
}

private val contactSectionComparator = compareBy<String> { it == "#" }.thenBy { it }

private fun contactSectionTitle(contact: V2FriendSearchUser): String {
    val name = contact.displayName.trim()
    val initial = name.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    initial.takeIf { it.matches(Regex("[A-Z]")) }?.let { return it }
    return pinyinSectionTitle(name) ?: "#"
}

private val pinyinInitialBoundaries = intArrayOf(
    -20319, -20284, -19776, -19219, -18711, -18527, -18240, -17923,
    -17418, -16475, -16213, -15641, -15166, -14923, -14915, -14631,
    -14150, -14091, -13319, -12839, -12557, -11848, -11056,
)
private const val pinyinInitialLetters = "ABCDEFGHJKLMNOPQRSTWXYZ"

/** Mirrors iOS AZOrderedList's Chinese-name A–Z grouping without raising minSdk. */
private fun pinyinSectionTitle(name: String): String? = runCatching {
    val first = name.firstOrNull() ?: return null
    val bytes = first.toString().toByteArray(Charset.forName("GBK"))
    if (bytes.size < 2) return null
    val code = (bytes[0].toInt() and 0xFF) * 256 + (bytes[1].toInt() and 0xFF) - 65_536
    pinyinInitialBoundaries.indices.reversed().firstOrNull { code >= pinyinInitialBoundaries[it] }
        ?.let { pinyinInitialLetters[it].toString() }
}.getOrNull()

@Composable
private fun ContactNavigationRow(
    title: String,
    badge: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DSColors.inkGreen, fontSize = 16.sp, modifier = Modifier.weight(1f))
        badge?.let {
            Text(
                if (it > 99) "99+" else "$it",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(Color(0xFFE54848))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ContactFriendRow(contact: V2FriendSearchUser, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = contact.avatar,
            modifier = Modifier.size(44.dp),
            fallbackText = contact.displayName,
            fallbackColor = DSColors.sandBeige,
        )
        Text(
            contact.displayName,
            color = DSColors.inkGreen,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContactDivider(start: Dp = 0.dp) {
    Box(
        Modifier.fillMaxWidth().height(1.dp).padding(start = start)
            .background(DSColors.borderLine),
    )
}

@Composable
internal fun InteractionContactSubpageTopBar(title: String, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().v2StatusBarPadding().height(44.dp).background(DSColors.warmWhite)
            .padding(horizontal = 8.dp),
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).size(44.dp).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "关闭$title", tint = DSColors.inkGreen, modifier = Modifier.size(16.dp))
        }
        Text(
            title,
            color = DSColors.inkGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun V2FriendRequestsDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<V2IncomingFriendRequest>>(emptyList()) }
    val handled = remember { mutableStateMapOf<String, String>() }
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun load() {
        loading = true
        error = null
        ServiceLocator.tencentFriendRepo.incomingFriendRequests(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { requests = it }
            .onFailure { error = coachThrowableError("好友申请暂时无法加载", it) }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Dialog(
        onDismissRequest = { if (processing == null) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
            InteractionContactSubpageTopBar("新的联系人", onDismiss)
            V2FriendRequestsContent(
                requests = requests,
                handled = handled,
                loading = loading,
                processingUserId = processing,
                error = error,
                onRetry = { scope.launch { load() } },
                onAccept = { request ->
                    processing = request.userId
                    error = null
                    scope.launch {
                        ServiceLocator.tencentFriendRepo.acceptFriendRequest(
                            session = ServiceLocator.session.current(),
                            targetUserId = request.userId,
                            renewExpiredSession = ::renewTencentSession,
                        ).onSuccess {
                            handled[request.userId] = "已同意"
                            onChanged()
                        }.onFailure { error = coachThrowableError("接受好友申请失败，请稍后重试", it) }
                        processing = null
                    }
                },
                onReject = { request ->
                    processing = request.userId
                    error = null
                    scope.launch {
                        ServiceLocator.tencentFriendRepo.rejectFriendRequest(
                            session = ServiceLocator.session.current(),
                            targetUserId = request.userId,
                            renewExpiredSession = ::renewTencentSession,
                        ).onSuccess { handled[request.userId] = "已拒绝" }
                            .onFailure { error = coachThrowableError("拒绝好友申请失败，请稍后重试", it) }
                        processing = null
                    }
                },
            )
        }
    }
}

@Composable
internal fun V2FriendRequestsContent(
    requests: List<V2IncomingFriendRequest>,
    handled: Map<String, String>,
    loading: Boolean,
    processingUserId: String?,
    error: String?,
    onRetry: () -> Unit,
    onAccept: (V2IncomingFriendRequest) -> Unit,
    onReject: (V2IncomingFriendRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize().v2NavigationBarPadding(),
        contentPadding = PaddingValues(vertical = 10.dp),
    ) {
        error?.let { message ->
            item {
                ContactErrorRow(message = message, onRetry = onRetry)
            }
        }
        when {
            loading -> item { ContactLoading("正在读取好友申请…") }
            requests.isEmpty() -> Unit
            else -> items(requests, key = V2IncomingFriendRequest::userId) { request ->
                FriendRequestRow(
                    request = request,
                    status = handled[request.userId],
                    processing = processingUserId == request.userId,
                    enabled = processingUserId == null,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request) },
                )
                ContactDivider(start = 72.dp)
            }
        }
    }
}

@Composable
internal fun FriendRequestRow(
    request: V2IncomingFriendRequest,
    status: String?,
    processing: Boolean,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V2UserAvatar(
                request.avatar,
                Modifier.size(40.dp),
                fallbackFontSize = 15.sp,
                fallbackText = request.displayName,
                fallbackColor = DSColors.sandBeige,
            )
            Text(
                request.displayName,
                color = DSColors.inkGreen,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                processing -> CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                status != null -> Text(
                    status,
                    color = if (status == "已拒绝") DSColors.flagRed else Color(0xFF2E9D56),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept,
                        enabled = enabled,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    ) { Text("同意", fontSize = 14.sp) }
                    Button(
                        onClick = onReject,
                        enabled = enabled,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DSColors.mistGreen,
                            contentColor = DSColors.inkGreen,
                        ),
                    ) { Text("拒绝", fontSize = 14.sp) }
                }
            }
        }
        request.wording?.takeIf(String::isNotBlank)?.let { wording ->
            Text(
                wording,
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}

@Composable
private fun V2BlackListDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<V2FriendSearchUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun load() {
        loading = true
        error = null
        ServiceLocator.tencentFriendRepo.blockedUserIds(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { ids ->
            users = coroutineScope {
                ids.map { userId ->
                    async {
                        ServiceLocator.authRepo.friendPublicInfo(userId).getOrElse {
                            V2FriendSearchUser(userId, userId, null, null)
                        }
                    }
                }.awaitAll()
            }
        }.onFailure { error = coachThrowableError("黑名单暂时无法加载", it) }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
            InteractionContactSubpageTopBar("黑名单", onDismiss)
            V2BlackListContent(
                users = users,
                loading = loading,
                error = error,
                onRetry = { scope.launch { load() } },
            )
        }
    }
}

@Composable
internal fun V2BlackListContent(
    users: List<V2FriendSearchUser>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().v2NavigationBarPadding()) {
        when {
            error != null -> ContactErrorRow(
                message = error,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            loading -> ContactLoading(
                label = "正在读取黑名单…",
                modifier = Modifier.align(Alignment.TopCenter),
            )
            users.isEmpty() -> BlackListEmptyState(Modifier.align(Alignment.Center))
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                items(users, key = V2FriendSearchUser::userId) { user ->
                    BlackListUserRow(user)
                    ContactDivider(start = 60.dp)
                }
            }
        }
    }
}

@Composable
private fun BlackListEmptyState(modifier: Modifier = Modifier) {
    val secondaryGray = Color(0xFF8E8E93)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(secondaryGray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = secondaryGray,
                    modifier = Modifier.size(50.dp),
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = secondaryGray,
                    modifier = Modifier.size(22.dp).align(Alignment.BottomEnd),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("黑名单为空", color = DSColors.inkGreen, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("您还没有屏蔽任何用户", color = secondaryGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BlackListUserRow(user: V2FriendSearchUser) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        V2UserAvatar(
            avatar = user.avatar,
            modifier = Modifier.size(40.dp),
            fallbackText = user.displayName,
            fallbackColor = DSColors.sandBeige,
        )
        Text(
            user.displayName,
            color = DSColors.inkGreen,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContactErrorRow(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = DSColors.flagRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("重试", color = DSColors.fairway, modifier = Modifier.clickable(onClick = onRetry).padding(8.dp))
    }
}

@Composable
private fun ContactLoading(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(Modifier.size(19.dp), color = DSColors.fairway, strokeWidth = 2.dp)
        Text(label, color = DSColors.shadowGray, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
    }
}
