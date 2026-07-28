package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun Modifier.creditsCardShadow(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp,
    alpha: Float,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = DSColors.fairway.copy(alpha = alpha),
    spotColor = DSColors.fairway.copy(alpha = alpha),
)

@Composable
fun V2CreditsResetScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit,
    onOpenPurchase: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdatedAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    fun refreshBalance(resetCache: Boolean = false) {
        if (!profile.isLoggedIn) {
            onRequestLogin()
            return
        }
        scope.launch {
            loading = true
            statusMessage = null
            errorMessage = null
            try {
                val balance = if (resetCache) {
                    ServiceLocator.creditsRepo.resetLocalCacheAndRefresh()
                } else {
                    ServiceLocator.creditsRepo.fetchBalance(force = true)
                }
                V2ProfileStore.refresh(force = true)
                if (balance == null) {
                    errorMessage = "服务端余额同步失败，请稍后重试"
                } else {
                    lastUpdatedAt = LocalDateTime.now()
                    statusMessage = if (resetCache) {
                        "本地积分缓存已清除，并完成余额校准"
                    } else {
                        "已同步服务端余额：$balance 积分"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                errorMessage = creditsResetError("服务端余额同步失败，请稍后重试", e.message)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(profile.isLoggedIn) {
        if (profile.isLoggedIn) refreshBalance(resetCache = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "积分重置", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .v2NavigationBarPadding()
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
        ) {
            CreditsHeroCard(isLoggedIn = profile.isLoggedIn)
            CreditsBalanceCard(
                balance = profile.credits,
                loading = loading,
                lastUpdatedAt = lastUpdatedAt,
                statusMessage = statusMessage,
                errorMessage = errorMessage,
            )
            CreditsActionCard(
                isLoggedIn = profile.isLoggedIn,
                loading = loading,
                onLogin = onRequestLogin,
                onPurchase = onOpenPurchase,
                onRefresh = { refreshBalance(resetCache = false) },
                onReset = { showResetConfirm = true },
            )
            CreditsAuditCard()
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("确认清除本地积分缓存？", style = DSTypography.titleMedium) },
            text = { Text("该操作不会修改服务器积分，只会清除本机缓存并重新拉取服务端余额。", style = DSTypography.body) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    refreshBalance(resetCache = true)
                }) {
                    Text("清除并校准", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消", color = DSColors.shadowGray)
                }
            },
        )
    }
}

@Composable
private fun CreditsHeroCard(isLoggedIn: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .creditsCardShadow(shape, 12.dp, 0.08f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(DSColors.cardWhite, DSColors.mistGreen.copy(alpha = 0.75f))))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            StatusPill(isLoggedIn = isLoggedIn)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("积分校准中心", fontSize = 25.sp, fontWeight = FontWeight.Black, color = DSColors.inkGreen)
            Text("同步服务端余额，处理本地缓存导致的显示偏差。", style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun StatusPill(isLoggedIn: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.82f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isLoggedIn) DSColors.fairway else DSColors.flagRed),
        )
        Text(
            if (isLoggedIn) "已登录" else "未登录",
            color = if (isLoggedIn) DSColors.fairway else DSColors.flagRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CreditsBalanceCard(
    balance: Int,
    loading: Boolean,
    lastUpdatedAt: LocalDateTime?,
    statusMessage: String?,
    errorMessage: String?,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm") }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .creditsCardShadow(shape, 10.dp, 0.08f)
            .clip(shape)
            .background(DSColors.cardWhite)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("当前余额", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(balance.toString(), fontSize = 44.sp, fontWeight = FontWeight.Black, color = DSColors.deepGreen)
            Text("积分", style = DSTypography.bodyBold, color = DSColors.shadowGray, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.AccessTime,
                null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(14.dp),
            )
            Text(
                lastUpdatedAt?.let { "上次校准 ${formatter.format(it)}" } ?: "尚未完成服务端校准",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        when {
            errorMessage != null -> CreditsStatusBanner(errorMessage, isError = true)
            statusMessage != null -> CreditsStatusBanner(statusMessage, isError = false)
        }
    }
}

@Composable
private fun CreditsStatusBanner(text: String, isError: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isError) Color(0xFFFCEDEB) else DSColors.mistGreen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            null,
            tint = if (isError) DSColors.flagRed else DSColors.fairway,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text,
            style = DSTypography.caption,
            color = if (isError) DSColors.flagRed else DSColors.fairway,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun creditsResetError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

@Composable
private fun CreditsActionCard(
    isLoggedIn: Boolean,
    loading: Boolean,
    onLogin: () -> Unit,
    onPurchase: () -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .creditsCardShadow(shape, 8.dp, 0.06f)
            .clip(shape)
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CreditsButton(
            text = if (isLoggedIn) "充值积分" else "去登录后充值",
            icon = Icons.Filled.ShoppingCart,
            enabled = !loading,
            backgroundBrush = Brush.linearGradient(listOf(DSColors.trophyGold, Color(0xFFE0A82E))),
            textColor = Color.White,
            onClick = if (isLoggedIn) onPurchase else onLogin,
        )
        CreditsButton(
            text = if (isLoggedIn) "刷新服务端余额" else "去登录后校准",
            icon = Icons.Filled.Refresh,
            enabled = !loading,
            backgroundBrush = Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen)),
            textColor = Color.White,
            height = 54.dp,
            onClick = if (isLoggedIn) onRefresh else onLogin,
        )
        CreditsButton(
            text = "清除本地缓存",
            icon = Icons.Filled.Delete,
            enabled = isLoggedIn && !loading,
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFFCEDEB), Color(0xFFFCEDEB))),
            textColor = DSColors.flagRed,
            height = 50.dp,
            onClick = onReset,
        )
    }
}

@Composable
private fun CreditsButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    backgroundBrush: Brush,
    textColor: Color,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val disabledBrush = Brush.linearGradient(
        listOf(
            DSColors.shadowGray.copy(alpha = 0.18f),
            DSColors.shadowGray.copy(alpha = 0.18f),
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) backgroundBrush else disabledBrush)
            .then(Modifier.clickable(enabled = enabled, onClick = onClick)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (enabled) textColor else DSColors.shadowGray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text, color = if (enabled) textColor else DSColors.shadowGray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreditsAuditCard() {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .creditsCardShadow(shape, 8.dp, 0.05f)
            .clip(shape)
            .background(DSColors.cardWhite)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("校准说明", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        AuditRow(Icons.Filled.Storage, "服务端余额优先", "刷新后以服务器返回值为准")
        AuditRow(Icons.Filled.PhoneAndroid, "本地缓存可重建", "清除缓存不会扣减或增加真实积分")
        AuditRow(Icons.Filled.Security, "安全确认", "重置类操作需要二次确认")
    }
}

@Composable
private fun AuditRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(subtitle, style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}
