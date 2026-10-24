package com.mygolfswing.app.ui.common

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler

/**
 * 全屏子页自己避开状态栏。
 * 已经放在带顶栏 Insets 的底栏页里时不要再加，否则会重复避让。
 */
fun Modifier.v2StatusBarPadding(): Modifier = statusBarsPadding()

/**
 * 底部操作区避开系统导航。
 * App 底栏自己加这一层；底栏页内容不要再加。
 */
fun Modifier.v2NavigationBarPadding(): Modifier = navigationBarsPadding()

fun Modifier.v2FullScreenInsets(): Modifier = statusBarsPadding().navigationBarsPadding()

@Composable
fun V2BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
