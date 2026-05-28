package com.mygolfswing.app.ui.theme

import androidx.compose.material3.Typography
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * Material3 [Typography] 的 V2 版映射。
 *
 * 业务代码应优先直接使用 `DSTypography.titleLarge` 等 Token，
 * 这里只是给 Material 组件（按钮、对话框等）一份合理的默认。
 */
val V2Typography = Typography(
    displayLarge = DSTypography.display,
    displayMedium = DSTypography.display,
    displaySmall = DSTypography.titleLarge,

    headlineLarge = DSTypography.titleLarge,
    headlineMedium = DSTypography.titleLarge,
    headlineSmall = DSTypography.titleMedium,

    titleLarge = DSTypography.titleLarge,
    titleMedium = DSTypography.titleMedium,
    titleSmall = DSTypography.cardTitle,

    bodyLarge = DSTypography.body,
    bodyMedium = DSTypography.body,
    bodySmall = DSTypography.caption,

    labelLarge = DSTypography.button,
    labelMedium = DSTypography.caption,
    labelSmall = DSTypography.label,
)
