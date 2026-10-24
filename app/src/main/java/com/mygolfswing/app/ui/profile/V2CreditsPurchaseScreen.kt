package com.mygolfswing.app.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mygolfswing.app.data.billing.V2CreditsBillingManager
import com.mygolfswing.app.data.store.V2ProfileStore

internal enum class V2PurchaseAvailabilityCard {
    None,
    MissingActivity,
    NoProducts,
    PartialProducts,
}

internal fun purchaseAvailabilityCard(
    loading: Boolean,
    hasActivity: Boolean,
    totalProducts: Int,
    purchasableProducts: Int,
): V2PurchaseAvailabilityCard {
    if (loading) return V2PurchaseAvailabilityCard.None
    if (!hasActivity) return V2PurchaseAvailabilityCard.MissingActivity
    val safeTotal = totalProducts.coerceAtLeast(0)
    val safePurchasable = purchasableProducts.coerceIn(0, safeTotal)
    if (safePurchasable == 0) return V2PurchaseAvailabilityCard.NoProducts
    return if (safePurchasable < safeTotal) {
        V2PurchaseAvailabilityCard.PartialProducts
    } else {
        V2PurchaseAvailabilityCard.None
    }
}

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
        }
    }
    DisposableEffect(manager) {
        onDispose { manager.close() }
    }

    V2CreditsPurchaseContent(
        balance = profile.credits,
        isLoggedIn = profile.isLoggedIn,
        products = state.products,
        loadingProducts = state.loadingProducts,
        purchasingProductId = state.purchasingProductId,
        message = state.message,
        error = state.error,
        hasPurchaseActivity = activity != null,
        onBack = onBack,
        onRequestLogin = onRequestLogin,
        onReload = {
            manager.loadProducts(showEmptyRestoreMessage = true)
        },
        onPurchase = { productId ->
            activity?.let { manager.launchPurchase(it, productId) }
        },
        onDismissNotice = manager::clearNotice,
)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
