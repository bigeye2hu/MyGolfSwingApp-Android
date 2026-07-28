package com.mygolfswing.app.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.billing.V2CreditProduct
import com.mygolfswing.app.data.billing.V2CreditsBillingManager
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

@Composable
fun V2CreditsPurchaseScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by V2ProfileStore.profile.collectAsState()
    val manager = remember(context) { V2CreditsBillingManager(context, scope) }
    val state by manager.state.collectAsState()
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(profile.isLoggedIn) {
        if (profile.isLoggedIn) {
            manager.loadProducts()
            manager.restorePendingPurchases(showEmptyMessage = false)
        }
    }
    DisposableEffect(manager) {
        onDispose { manager.close() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            V2NavTopBarCentered(title = "积分充值", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                PurchaseHeroCard(balance = profile.credits, isLoggedIn = profile.isLoggedIn)
                state.message?.let {
                    PurchaseNotice(
                        text = it,
                        isError = false,
                        actionText = if (it.startsWith("充值成功")) "返回继续" else null,
                        onAction = onBack,
                        onDismiss = manager::clearNotice,
                    )
                }
                state.error?.let {
                    PurchaseNotice(text = it, isError = true, onDismiss = manager::clearNotice)
                }
                if (!profile.isLoggedIn) {
                    LoginRequiredCard(onLogin = onRequestLogin)
                } else {
                    ProductsSection(
                        products = state.products,
                        loading = state.loadingProducts,
                        purchasingProductId = state.purchasingProductId,
                        activity = activity,
                        onReload = {
                            manager.loadProducts()
                            manager.restorePendingPurchases(showEmptyMessage = true)
                        },
                        onPurchase = manager::launchPurchase,
                    )
                    PurchaseNotesCard()
                }
            }
        }

        if (state.purchasingProductId != null) {
            PurchasingOverlay()
        }
    }
}

@Composable
private fun PurchaseHeroCard(balance: Int, isLoggedIn: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Stars, null, tint = Color.White, modifier = Modifier.size(29.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (isLoggedIn) "当前 $balance 积分" else "登录后充值",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("购买下载积分", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("积分可用于下载高清视频和训练素材", style = DSTypography.caption, color = Color.White.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun ProductsSection(
    products: List<V2CreditProduct>,
    loading: Boolean,
    purchasingProductId: String?,
    activity: Activity?,
    onReload: () -> Unit,
    onPurchase: (Activity, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("选择充值档位", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !loading, onClick = onReload)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
                }
                Text(if (loading) "加载中" else "刷新/恢复", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        val purchasableCount = products.count { it.productDetails != null }
        if (!loading && (activity == null || purchasableCount == 0)) {
            PurchaseUnavailableCard(
                title = if (activity == null) "购买页暂时无法打开" else "商品暂不可购买",
                body = if (activity == null) {
                    "当前页面没有可用于 Google Play 结算的 Activity，请返回后重新进入本页。"
                } else {
                    "没有从 Google Play 加载到可购买的积分商品。请确认网络、Google Play 登录状态，以及 Play Console 商品是否已生效。"
                },
                onReload = onReload,
            )
        } else if (!loading && purchasableCount in 1 until products.size) {
            PurchasePartialConfigCard(
                available = purchasableCount,
                total = products.size,
                onReload = onReload,
            )
        }
        products.forEach { product ->
            ProductCard(
                product = product,
                loading = loading,
                purchasing = purchasingProductId == product.productId,
                canPurchase = product.productDetails != null && activity != null && purchasingProductId == null,
                onClick = {
                    val a = activity ?: return@ProductCard
                    onPurchase(a, product.productId)
                },
            )
        }
    }
}

@Composable
private fun PurchaseUnavailableCard(
    title: String,
    body: String,
    onReload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF7ED))
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
            Text(title, style = DSTypography.bodyBold, color = Color(0xFF92400E))
        }
        Text(body, style = DSTypography.caption, color = Color(0xFF9A3412))
        PurchaseInlineAction(text = "刷新并恢复订单", icon = Icons.Filled.Refresh, onClick = onReload)
    }
}

@Composable
private fun PurchasePartialConfigCard(
    available: Int,
    total: Int,
    onReload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.mistGreen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = DSColors.fairway, modifier = Modifier.size(17.dp))
        Text(
            "$available/$total 个商品已可购买，其余档位等待 Google Play 生效。",
            style = DSTypography.caption,
            color = DSColors.fairway,
            modifier = Modifier.weight(1f),
        )
        Text(
            "刷新",
            color = DSColors.fairway,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onReload)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun PurchaseInlineAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.78f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Color(0xFFB45309), modifier = Modifier.size(15.dp))
        Text(text, color = Color(0xFFB45309), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProductCard(
    product: V2CreditProduct,
    loading: Boolean,
    purchasing: Boolean,
    canPurchase: Boolean,
    onClick: () -> Unit,
) {
    val border = when {
        product.popular -> DSColors.trophyGold
        product.productDetails != null -> DSColors.fairway.copy(alpha = 0.30f)
        else -> DSColors.borderLine
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (product.popular) Color(0xFFFFFBEB) else DSColors.listMutedBg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(enabled = canPurchase, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (product.popular) DSColors.trophyGold.copy(alpha = 0.16f) else DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (product.popular) Icons.Filled.Star else Icons.Filled.AccountBalanceWallet,
                null,
                tint = if (product.popular) DSColors.trophyGold else DSColors.fairway,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${product.credits} 积分", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                if (product.popular) {
                    Text(
                        "最划算",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DSColors.trophyGold)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                if (product.productDetails == null && !loading) {
                    "等待 Google Play 商品配置"
                } else {
                    "约${product.credits / 10}次高清下载"
                },
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.displayPrice, color = DSColors.deepGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
            PurchaseMiniButton(
                text = when {
                    purchasing -> "购买中"
                    product.productDetails == null -> "未配置"
                    else -> "立即购买"
                },
                icon = if (purchasing) Icons.Filled.Refresh else Icons.Filled.ShoppingCart,
                enabled = canPurchase,
            )
        }
    }
}

@Composable
private fun PurchaseMiniButton(text: String, icon: ImageVector, enabled: Boolean) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = if (enabled) Color.White else DSColors.shadowGray, modifier = Modifier.size(13.dp))
        Text(text, color = if (enabled) Color.White else DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginRequiredCard(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("登录后充值", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        Text("积分会发放到当前账号，购买前需要先完成登录。", style = DSTypography.caption, color = DSColors.shadowGray)
        PurchaseActionButton("去登录", Icons.Filled.AccountBalanceWallet, onLogin)
    }
}

@Composable
private fun PurchaseNotesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("充值说明", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        PurchaseNote(Icons.Filled.CheckCircle, "积分永久有效", "成功发放后会进入服务端余额")
        PurchaseNote(Icons.Filled.ShoppingCart, "Google Play 结算", "支付完成后自动消费一次性商品")
        PurchaseNote(Icons.Filled.Refresh, "自动恢复订单", "支付后断网或重进页面，会尝试恢复未完成积分订单")
        PurchaseNote(Icons.Filled.Info, "订单可追踪", "发放异常时会显示订单号或凭据尾号，便于客服核对")
        PurchaseNote(Icons.Filled.CheckCircle, "账号已绑定", "购买请求会携带非明文账号标识，降低错账风险")
    }
}

@Composable
private fun PurchasingOverlay() {
    val overlayInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.70f))
            .clickable(
                interactionSource = overlayInteraction,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp), strokeWidth = 3.dp)
            Text("处理中...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PurchaseNotice(
    text: String,
    isError: Boolean,
    actionText: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isError) Color(0xFFFCEDEB) else DSColors.mistGreen)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            null,
            tint = if (isError) DSColors.flagRed else DSColors.fairway,
            modifier = Modifier.size(17.dp),
        )
        Text(text, style = DSTypography.caption, color = if (isError) DSColors.flagRed else DSColors.fairway, modifier = Modifier.weight(1f))
        actionText?.let {
            Text(
                it,
                color = DSColors.fairway,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.78f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PurchaseNote(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(subtitle, style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun PurchaseActionButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.fairway)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
