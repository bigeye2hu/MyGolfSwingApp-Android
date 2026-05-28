package com.mygolfswing.app.ui.common

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler

/** 全屏子页顶栏/内容需避开状态栏（[enableEdgeToEdge] 后否则返回按钮点不到）。 */
fun Modifier.v2StatusBarPadding(): Modifier = statusBarsPadding()

/** 底部操作区避开手势导航条。 */
fun Modifier.v2NavigationBarPadding(): Modifier = navigationBarsPadding()

fun Modifier.v2FullScreenInsets(): Modifier = statusBarsPadding().navigationBarsPadding()

@Composable
fun V2BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
