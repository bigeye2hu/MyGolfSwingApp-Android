package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.store.V2UserProfile
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class V2ProfileLoginTarget {
    EDIT_PROFILE,
    ALL_GOALS,
    VIDEO_MEMOS,
    SWING_CALENDAR,
    AI_ARCHIVE,
    CREDITS,
    ACCOUNT_SECURITY,
}

/**
 * V2「我」页面（对齐 iOS `V2ProfileView`）。
 *
 * 接入真实数据：
 * - 启动 + 进入页面时刷新 [V2ProfileStore.refresh]
 * - 未登录态：顶部显示醒目的「立即登录 / 注册」banner，UserCard 也可以点
 * - 登录态：头像 / 编辑按钮 → 编辑资料；账号与安全 → 修改 / 设置密码
 * - 退出登录：弹确认对话框；确认后清 session 并显示 Snackbar 反馈
 */
@Composable
fun V2ProfileScreen(
    onRequestLogin: () -> Unit = {},
    onRequestLoginForTarget: (V2ProfileLoginTarget) -> Unit = { onRequestLogin() },
    onOpenEditProfile: () -> Unit = {},
    onOpenAccountSecurity: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenResetCredits: () -> Unit = {},
    onOpenAIConfig: () -> Unit = {},
    onOpenAIArchive: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAllGoals: () -> Unit = {},
    onOpenVideoMemoList: () -> Unit = {},
    onOpenSwingCalendar: () -> Unit = {},
    onBrowsePlaza: () -> Unit = {},
    onBrowseMemo: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
    showRoleSwitcher: Boolean = false,
    roleSwitchBlocked: Boolean = false,
    onSwitchRole: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
    val swingSessions by V2SwingSessionStore.sessions.collectAsState()
    val isRefreshing by V2ProfileStore.isRefreshing.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    var logoutError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 记住上一次的登录态，用于检测「刚退出」事件
    val previousLoggedIn = remember { mutableStateOf(profile.isLoggedIn) }

    LaunchedEffect(profile.isLoggedIn) {
        val was = previousLoggedIn.value
        previousLoggedIn.value = profile.isLoggedIn
        if (profile.isLoggedIn) {
            V2ProfileStore.refresh()
        } else if (was && !profile.isLoggedIn) {
            val result = snackbarHostState.showSnackbar(
                message = "已退出登录",
                actionLabel = "立即登录",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onRequestLogin()
        }
    }

    val displayGoals = profileDisplayGoals(profile.isLoggedIn, goals)
    val displayMemos = profileDisplayMemos(profile.isLoggedIn, memos)
    val activeGoals = displayGoals.count { it.status.name == "ACTIVE" }
    val completedGoals = displayGoals.count { it.status.name == "COMPLETED" }
    val videoMemoCount = displayMemos.count {
        it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED
    }
    val swingCount = if (profile.isLoggedIn) swingSessions.sumOf { it.clips.size } else 0

    Box(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { V2ProfileStore.refresh(force = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg)
                    .padding(top = DSSpacing.sm, bottom = DSSpacing.xxl + DSSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                ProfileHeader(
                    isLoggedIn = profile.isLoggedIn,
                    onLogin = onRequestLogin,
                )
                if (!profile.isLoggedIn) {
                    V2ProfileGuestContent(
                        onLogin = onRequestLogin,
                        onBrowsePlaza = onBrowsePlaza,
                        onBrowseMemo = onBrowseMemo,
                        onFeedback = onRequestLogin,
                        onTerms = onOpenTerms,
                        onPrivacy = onOpenPrivacy,
                    )
                } else {
                    V2ProfileUserCard(
                        profile = profile,
                        onAvatarTap = onOpenEditProfile,
                        onEditTap = onOpenEditProfile,
                    )
                    V2ProfilePersonalInfoCard(
                        birthday = profileBirthdayText(profile.birthday),
                        location = profileOptionalText(profile.location),
                        bio = profile.bio?.trim().orEmpty(),
                        onEditTap = onOpenEditProfile,
                    )
                    V2ProfileStatsCard(
                        activeGoals = activeGoals,
                        completedGoals = completedGoals,
                        videoMemoCount = videoMemoCount,
                        swingCount = swingCount,
                        onTapActive = onOpenAllGoals,
                        onTapCompleted = onOpenAllGoals,
                        onTapVideoMemo = onOpenVideoMemoList,
                        onTapSwing = onOpenSwingCalendar,
                    )
                    GolfRecordCard(profile = profile, onEditTap = onOpenEditProfile)
                    AIArchiveCard(subtitle = "AI 已了解你的练习习惯与短板", onTap = onOpenAIArchive)
                    V2ProfileMoreList(
                        isLoggedIn = true,
                        accountSecuritySubtitle = profileAccountSecurityText(profile.phone, profile.hasPassword),
                        onStorageTap = onOpenStorage,
                        onResetCreditsTap = onOpenResetCredits,
                        onAIConfigTap = onOpenAIConfig,
                        onFeedbackTap = onOpenFeedback,
                        onUserAgreementTap = onOpenTerms,
                        onPrivacyPolicyTap = onOpenPrivacy,
                        onAccountSecurityTap = onOpenAccountSecurity,
                        onLogoutTap = { confirmLogout = true },
                        showsRoleSwitcher = showRoleSwitcher,
                        accountActionsDisabled = roleSwitchBlocked,
                        onRoleSwitchTap = onSwitchRole,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).v2NavigationBarPadding().padding(bottom = 16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = DSColors.deepGreen,
                contentColor = DSColors.warmWhite,
                actionColor = DSColors.goldBright,
            )
        }
    }

    if (confirmLogout) {
        ProfileLogoutDialog(
            onDismiss = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                scope.launch {
                    try {
                        V2ProfileStore.logoutAndWait()
                        onLogoutComplete()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logoutError = profileLogoutThrowableError(e)
                    }
                }
            },
        )
    }

    logoutError?.let { message ->
        AlertDialog(
            onDismissRequest = { logoutError = null },
            title = { Text("退出失败", style = DSTypography.titleMedium) },
            text = { Text(message, style = DSTypography.body) },
            confirmButton = {
                TextButton(onClick = { logoutError = null }) {
                    Text("好", color = DSColors.fairway)
                }
            },
        )
    }

}

internal fun profileDisplayGoals(isLoggedIn: Boolean, goals: List<V2Goal>): List<V2Goal> =
    if (isLoggedIn) goals else emptyList()

internal fun profileDisplayMemos(isLoggedIn: Boolean, memos: List<V2Memo>): List<V2Memo> =
    if (isLoggedIn) memos else emptyList()

internal fun profileLogoutThrowableError(error: Throwable): String {
    if (error is CancellationException) throw error
    return "退出登录失败，请稍后重试"
}

internal fun profileOptionalText(raw: String?): String = raw?.trim()?.takeIf(String::isNotEmpty) ?: "未设置"

internal fun profileBirthdayText(raw: String?): String {
    val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return "未设置"
    if ('月' in value && '日' in value) return value
    return runCatching {
        LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("MM月dd日"))
    }.getOrDefault(value)
}

internal fun profileAccountSecurityText(phone: String?, hasPassword: Boolean): String {
    val phoneStatus = if (phone.isNullOrBlank()) "手机号未绑定" else "手机号已绑定"
    val passwordStatus = if (hasPassword) "密码已设置" else "密码未设置"
    return "$phoneStatus · $passwordStatus"
}

@Composable
internal fun ProfileHeader(
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "我的", style = DSTypography.titleLarge, color = DSColors.inkGreen)
            Text(
                text = if (isLoggedIn) "My 高尔夫之路" else "游客只读体验 · 保存需登录",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
            )
        }
        if (!isLoggedIn) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = DSColors.fairway.copy(alpha = 0.18f),
                        spotColor = DSColors.fairway.copy(alpha = 0.18f),
                    )
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.deepGreen)
                    .clickable(onClick = onLogin)
                    .testTag("profile-header-guest-login")
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    "登录/注册",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LoginPromptBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(DSColors.deepGreen, DSColors.fairway)),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = DSSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DSColors.warmWhite.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = DSColors.warmWhite,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "未登录",
                color = DSColors.warmWhite,
                style = DSTypography.bodyBold.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            )
            Text(
                "点击立即登录 / 注册，同步目标 · 视频 · AI 训练档案",
                color = DSColors.warmWhite.copy(alpha = 0.85f),
                style = DSTypography.caption,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = DSColors.warmWhite,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun GolfRecordCard(profile: V2UserProfile, onEditTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.08f),
                spotColor = DSColors.fairway.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("⛳", fontSize = 16.sp)
                Text("高尔夫档案", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            }
            Text(
                text = "编辑",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.fairway,
                modifier = Modifier.clickable(onClick = onEditTap),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm + 2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm + 2.dp)) {
                RecordChip(label = "差点 Handicap", value = profile.handicap, primary = true, modifier = Modifier.weight(1f))
                RecordChip(label = "球龄 Years", value = profile.golfYears, primary = false, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm + 2.dp)) {
                RecordChip(label = "惯用手 Hand", value = profile.dominantHandDisplay(), primary = false, modifier = Modifier.weight(1f))
                RecordChip(label = "球具品牌 Brand", value = profile.clubBrand, primary = false, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecordChip(
    label: String,
    value: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    var valueFontSize by remember(value) { mutableStateOf(16.sp) }
    Column(
        modifier = modifier
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = DSSpacing.md, vertical = DSSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontSize = 10.sp, color = DSColors.shadowGray)
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            color = if (primary) DSColors.fairway else DSColors.inkGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && valueFontSize.value > 12.6f) {
                    valueFontSize = (valueFontSize.value - 0.5f).sp
                }
            },
        )
    }
}

private fun V2UserProfile.dominantHandDisplay(): String =
    when (dominantHand) {
        "右手" -> "右手 ✋"
        "左手" -> "左手 🤚"
        else -> dominantHand
    }

@Composable
private fun AIArchiveCard(
    subtitle: String,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to DSColors.fairway,
                        0.6f to Color(0xFF40916C),
                        1.0f to DSColors.leafGreen,
                    ),
                ),
                RoundedCornerShape(20.dp),
            )
            .heightIn(min = 96.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "🧠",
            fontSize = 80.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .alpha(0.08f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🧠", fontSize = 18.sp)
                    Text("AI 训练档案", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DSColors.warmWhite)
                }
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = DSColors.warmWhite.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(DSColors.warmWhite.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = DSColors.warmWhite,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}
