package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.store.V2UserProfile
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

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
    onOpenEditProfile: () -> Unit = {},
    onOpenAccountSecurity: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenAIConfig: () -> Unit = {},
    onOpenAIArchive: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenCloudAudit: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
    val swingSessions by V2SwingSessionStore.sessions.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
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

    val displayGoals = if (profile.isLoggedIn) goals.filterNot { it.title in demoGoalTitles } else emptyList()
    val displayMemos = if (profile.isLoggedIn) memos.filterNot { it.displayTitle in demoMemoTitles || it.title in demoMemoTitles } else emptyList()
    val activeGoals = displayGoals.count { it.status.name == "ACTIVE" }
    val completedGoals = displayGoals.count { it.status.name == "COMPLETED" }
    val videoMemoCount = displayMemos.count {
        it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED
    }
    val swingCount = if (profile.isLoggedIn) swingSessions.sumOf { it.clips.size } else 0
    val isTestUser = V2DeveloperToolsAccess.isCloudAuditEnabled(profile)

    Box(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DSSpacing.lg)
                .padding(vertical = DSSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            Header()
            if (!profile.isLoggedIn) {
                LoginPromptBanner(onClick = onRequestLogin)
            }
            V2ProfileUserCard(
                profile = profile,
                onAvatarTap = {
                    if (!profile.isLoggedIn) onRequestLogin() else onOpenEditProfile()
                },
                onEditTap = {
                    if (!profile.isLoggedIn) onRequestLogin() else onOpenEditProfile()
                },
            )
            V2ProfileStatsCard(
                activeGoals = activeGoals,
                completedGoals = completedGoals,
                videoMemoCount = videoMemoCount,
                swingCount = swingCount,
            )
            GolfRecordCard(profile = profile)
            AIArchiveCard(onTap = onOpenAIArchive)
            V2ProfileMoreList(
                isLoggedIn = profile.isLoggedIn,
                onStorageTap = onOpenStorage,
                onAIConfigTap = onOpenAIConfig,
                onFeedbackTap = onOpenFeedback,
                onUserAgreementTap = onOpenTerms,
                onPrivacyPolicyTap = onOpenPrivacy,
                onAccountSecurityTap = {
                    if (!profile.isLoggedIn) onRequestLogin() else onOpenAccountSecurity()
                },
                onCloudAuditTap = onOpenCloudAudit,
                onLogoutTap = {
                    if (profile.isLoggedIn) confirmLogout = true else onRequestLogin()
                },
                isTestUser = isTestUser,
            )
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
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
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("确认退出登录？", style = DSTypography.titleMedium) },
            text = { Text("退出后将清除本机登录态，下次进入需要重新登录。", style = DSTypography.body) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch {
                        V2ProfileStore.logoutAndWait()
                        onLogoutComplete()
                    }
                }) {
                    Text("退出登录", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("取消", color = DSColors.shadowGray)
                }
            },
        )
    }

}

private val demoGoalTitles = setOf(
    "用 7 号铁打 50 球",
    "练习 40 分钟挥杆基础",
    "推杆 5 米精度训练",
    "每日复盘心法",
)

private val demoMemoTitles = setOf(
    "握把压力的微调",
    "推杆距离感的练习",
    "教练点评：上杆速度",
    "下杆节奏练习 · 7号铁",
    "沙坑救球角度",
)

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DSSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "我的", style = DSTypography.titleLarge, color = DSColors.inkGreen)
        Text(text = "My 高尔夫之路", style = DSTypography.caption, color = DSColors.shadowGray)
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
private fun GolfRecordCard(profile: V2UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("高尔夫档案", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        RecordRow(label = "差点 Handicap", value = profile.handicap)
        RecordRow(label = "球龄 Years", value = profile.golfYears)
        RecordRow(label = "惯用手", value = profile.dominantHand)
        RecordRow(label = "球杆品牌", value = profile.clubBrand)
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DSTypography.caption, color = DSColors.shadowGray)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, style = DSTypography.bodyBold, color = DSColors.inkGreen)
    }
}

@Composable
private fun AIArchiveCard(onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("AI 训练档案", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Text(
            text = "AI 教练会根据你过去 30 天的练习自动总结你的强项 / 短板。点击查看 →",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
    }
}
