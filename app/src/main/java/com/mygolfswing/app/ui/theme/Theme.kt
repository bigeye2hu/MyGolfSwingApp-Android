package com.mygolfswing.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * V2 全局主题。
 *
 * 设计原则：
 * - 只用浅色模式（暂不做 dark theme，iOS V2 也是单色调）
 * - 不用 Material You 动态色（与 V2 米色绿色系冲突）
 * - 颜色 / 字体 / 间距统一走 `DSColors / DSTypography / DSSpacing`
 *
 * [MaterialTheme] 仍然提供给 Material3 组件作为兜底（Button、Switch 等），
 * 但业务页面应优先使用 DS Token。
 */
private val V2ColorScheme = lightColorScheme(
    primary = DSColors.fairway,
    onPrimary = DSColors.cardWhite,
    primaryContainer = DSColors.mistGreen,
    onPrimaryContainer = DSColors.deepGreen,

    secondary = DSColors.deepGreen,
    onSecondary = DSColors.cardWhite,
    secondaryContainer = DSColors.chipGreenBg,
    onSecondaryContainer = DSColors.deepGreen,

    tertiary = DSColors.trophyGold,
    onTertiary = DSColors.cardWhite,

    background = DSColors.warmWhite,
    onBackground = DSColors.inkGreen,

    surface = DSColors.cardWhite,
    onSurface = DSColors.inkGreen,
    surfaceVariant = DSColors.sandBeige,
    onSurfaceVariant = DSColors.shadowGray,

    error = DSColors.flagRed,
    onError = DSColors.cardWhite,

    outline = DSColors.listCardBorder,
    outlineVariant = DSColors.borderLine,
)

@Composable
fun MyGolfSwingAppAndroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = V2ColorScheme,
        typography = V2Typography,
        content = content,
    )
}
