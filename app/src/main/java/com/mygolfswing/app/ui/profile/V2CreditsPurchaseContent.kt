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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.billing.V2CreditProduct
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding

private val PurchaseBlack = Color.Black
private val PurchasePanel = Color(0xFF1F2937)
private val PurchaseGold = Color(0xFFFBBF24)
private val PurchasePurple = Color(0xFFA855F7)
private val PurchaseBlue = Color(0xFF3B82F6)
private val PurchaseGray = Color(0xFF9CA3AF)
private val PurchaseGreen = Color(0xFF34D399)

/** Current iOS `CreditsPurchaseView` hierarchy with Android billing boundaries preserved. */
@Composable
internal fun V2CreditsPurchaseContent(
    balance: Int,
    isLoggedIn: Boolean,
    products: List<V2CreditProduct>,
    loadingProducts: Boolean,
    purchasingProductId: String?,
    message: String?,
    error: String?,
    hasPurchaseActivity: Boolean,
    onBack: () -> Unit,
    onRequestLogin: () -> Unit,
    onReload: () -> Unit,
    onPurchase: (String) -> Unit,
    onDismissNotice: () -> Unit,
) {
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleError = localError ?: error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurchaseBlack)
            .testTag("credits-purchase-page"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .v2NavigationBarPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 128.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PurchaseHeader()
            PurchaseBalanceCard(balance = balance, isLoggedIn = isLoggedIn)
            message?.let {
                val success = creditsPurchaseNoticeIsSuccess(it)
                PurchaseDarkNotice(
                    text = it,
                    success = success,
                    actionText = if (success) "返回继续" else null,
                    onAction = onBack,
                    onDismiss = onDismissNotice,
                )
            }
            if (isLoggedIn) {
                PurchaseOptionsSection(
                    products = products,
                    loading = loadingProducts,
                    purchasingProductId = purchasingProductId,
                    hasPurchaseActivity = hasPurchaseActivity,
                    onReload = onReload,
                    onPurchase = onPurchase,
                    onUnavailablePurchase = {
                        localError = "内购服务正在准备中，请稍后再试。\n\nGoogle Play purchases will be available soon."
                    },
                )
                PurchaseTipsCard()
            } else {
                PurchaseLoginRequiredCard(onLogin = onRequestLogin)
            }
        }

        PurchaseTopBar(onBack = onBack)

        if (purchasingProductId != null) {
            PurchaseProcessingOverlay()
        }

        visibleError?.let { text ->
            AlertDialog(
                onDismissRequest = {
                    if (localError != null) localError = null else onDismissNotice()
                },
                containerColor = PurchasePanel,
                titleContentColor = Color.White,
                textContentColor = PurchaseGray,
                title = { Text("购买失败", fontWeight = FontWeight.Bold) },
                text = { Text(text) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (localError != null) localError = null else onDismissNotice()
                        },
                    ) {
                        Text("确定", color = PurchaseGold, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }
    }
}

@Composable
private fun PurchaseTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(PurchasePanel)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text("充值积分", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PurchaseHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(PurchaseGold),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = PurchaseBlack, modifier = Modifier.size(34.dp))
        }
        Text("购买下载积分", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "积分可用于下载高清视频和训练素材",
            color = PurchaseGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PurchaseBalanceCard(balance: Int, isLoggedIn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PurchasePanel)
            .border(1.dp, PurchaseGold.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("当前积分", color = PurchaseGray, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isLoggedIn) balance.toString() else "--",
                    color = PurchaseGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("积分", color = PurchaseGray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Star, contentDescription = null, tint = PurchaseGold.copy(alpha = 0.30f), modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun PurchaseOptionsSection(
    products: List<V2CreditProduct>,
    loading: Boolean,
    purchasingProductId: String?,
    hasPurchaseActivity: Boolean,
    onReload: () -> Unit,
    onPurchase: (String) -> Unit,
    onUnavailablePurchase: () -> Unit,
) {
    val catalog = remember(products) { products.sortedBy { it.credits } }
    val catalogKey = remember(catalog) { catalog.joinToString(separator = ",") { it.credits.toString() } }
    var selectedCredits by rememberSaveable(catalogKey) {
        mutableIntStateOf(catalog.firstOrNull { it.credits == 300 }?.credits ?: catalog.firstOrNull()?.credits ?: 300)
    }
    val purchasableCount = catalog.count { it.productDetails != null }
    val availability = purchaseAvailabilityCard(
        loading = loading,
        hasActivity = hasPurchaseActivity,
        totalProducts = catalog.size,
        purchasableProducts = purchasableCount,
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("选择充值档位", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !loading, onClick = onReload)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(color = PurchaseGold, modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = PurchaseGold, modifier = Modifier.size(14.dp))
                }
                Text(if (loading) "加载中" else "刷新/恢复", color = PurchaseGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (purchasableCount == 0) {
            catalog.forEach { product ->
                SelectablePurchaseCard(
                    product = product,
                    selected = selectedCredits == product.credits,
                    onSelect = { selectedCredits = product.credits },
                )
            }
            PurchaseGradientButton(
                text = "立即购买 $selectedCredits 积分",
                icon = Icons.Filled.ShoppingCart,
                onClick = onUnavailablePurchase,
            )
            Text(
                if (loading) "正在连接 Google Play..." else "Google Play 商品暂未就绪，可刷新后重试",
                color = PurchaseGray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            catalog.forEach { product ->
                LoadedPurchaseCard(
                    product = product,
                    purchasing = purchasingProductId == product.productId,
                    enabled = product.productDetails != null && hasPurchaseActivity && purchasingProductId == null,
                    onClick = {
                        if (product.productDetails == null || !hasPurchaseActivity) onUnavailablePurchase()
                        else onPurchase(product.productId)
                    },
                )
            }
        }

        when (availability) {
            V2PurchaseAvailabilityCard.MissingActivity -> PurchasePlatformNotice(
                "当前页面无法打开 Google Play 结算，请返回后重新进入。",
                onReload,
            )
            V2PurchaseAvailabilityCard.NoProducts -> if (!loading) PurchasePlatformNotice(
                "尚未加载到可购买商品，请确认 Google Play 登录与网络状态。",
                onReload,
            )
            V2PurchaseAvailabilityCard.PartialProducts -> PurchasePlatformNotice(
                "$purchasableCount/${catalog.size} 个商品已可购买，其余档位等待 Google Play 生效。",
                onReload,
            )
            V2PurchaseAvailabilityCard.None -> Unit
        }
    }
}

@Composable
private fun SelectablePurchaseCard(
    product: V2CreditProduct,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PurchasePanel.copy(alpha = if (selected) 1f else 0.60f))
                .border(
                    if (selected) 2.dp else 1.dp,
                    when {
                        selected -> PurchasePurple
                        product.popular -> PurchaseGold.copy(alpha = 0.50f)
                        else -> PurchaseGray.copy(alpha = 0.30f)
                    },
                    RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onSelect)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected) PurchasePurple else PurchaseGray.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(14.dp).clip(CircleShape).background(PurchasePurple))
            }
            Column(
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.credits.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("下载积分", color = PurchaseGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
                Text("约${product.credits / 10}次高清下载", color = PurchaseGray, fontSize = 12.sp)
            }
            Text(product.displayPrice, color = PurchaseGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (product.popular) PurchasePopularBadge(Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-6).dp))
    }
}

@Composable
private fun LoadedPurchaseCard(
    product: V2CreditProduct,
    purchasing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PurchasePanel)
                .border(
                    if (product.popular) 2.dp else 1.dp,
                    if (product.popular) PurchaseGold else PurchaseGray.copy(alpha = 0.30f),
                    RoundedCornerShape(16.dp),
                )
                .clickable(enabled = enabled || product.productDetails == null, onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.credits.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("积分", color = PurchaseGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    if (product.productDetails == null) "等待 Google Play 商品配置" else "约${product.credits / 10}次高清下载",
                    color = PurchaseGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.displayPrice, color = PurchaseGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (enabled) Brush.horizontalGradient(listOf(PurchasePurple, PurchaseBlue)) else Brush.horizontalGradient(listOf(PurchaseGray.copy(alpha = 0.4f), PurchaseGray.copy(alpha = 0.28f))))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (purchasing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Text(
                        when {
                            purchasing -> "购买中"
                            product.productDetails == null -> "暂不可用"
                            else -> "立即购买"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        if (product.popular) PurchasePopularBadge(Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-8).dp))
    }
}

@Composable
private fun PurchasePopularBadge(modifier: Modifier = Modifier) {
    Text(
        "最划算",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(PurchasePurple, PurchaseBlue)))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun PurchaseGradientButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = PurchasePurple.copy(alpha = 0.40f), spotColor = PurchasePurple.copy(alpha = 0.40f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(PurchasePurple, PurchaseBlue)))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PurchasePlatformNotice(text: String, onReload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PurchasePanel.copy(alpha = 0.75f))
            .border(1.dp, PurchaseGold.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PurchaseGold, modifier = Modifier.size(16.dp))
        Text(text, color = PurchaseGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            "刷新",
            color = PurchaseGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onReload).padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun PurchaseTipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PurchasePanel.copy(alpha = 0.50f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("购买说明", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        PurchaseTipRow(Icons.Filled.CheckCircle, "购买完成后积分立即到账")
        PurchaseTipRow(Icons.Filled.Shield, "通过 Google Play 官方支付，安全可靠")
        PurchaseTipRow(Icons.Filled.Refresh, "积分永久有效，不会过期")
    }
}

@Composable
private fun PurchaseTipRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = PurchaseGold, modifier = Modifier.size(14.dp))
        Text(text, color = PurchaseGray, fontSize = 13.sp)
    }
}

@Composable
private fun PurchaseLoginRequiredCard(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PurchasePanel)
            .border(1.dp, PurchaseGray.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("登录后充值", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("积分会发放到当前账号，购买前需要先完成登录。", color = PurchaseGray, fontSize = 13.sp)
        PurchaseGradientButton("去登录", Icons.Filled.AccountBalanceWallet, onLogin)
    }
}

@Composable
private fun PurchaseDarkNotice(
    text: String,
    success: Boolean,
    actionText: String?,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = if (success) PurchaseGreen else PurchaseGold
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (success) "credits-purchase-success-notice" else "credits-purchase-recovery-notice")
            .clip(RoundedCornerShape(12.dp))
            .background(PurchasePanel)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp),
        )
        Text(text, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        actionText?.let {
            Text(
                it,
                color = PurchaseGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

internal fun creditsPurchaseNoticeIsSuccess(message: String): Boolean =
    message.startsWith("充值成功") || message.startsWith("已完成订单确认")

@Composable
private fun PurchaseProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("credits-purchase-processing-overlay")
            .background(Color.Black.copy(alpha = 0.70f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp), strokeWidth = 3.dp)
            Text("处理中...", color = Color.White, fontSize = 16.sp)
        }
    }
}
