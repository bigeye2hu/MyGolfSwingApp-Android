package com.mygolfswing.app.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * V2 设计系统 - 间距 / 圆角 / 阴影 Token（对齐 iOS `DSSpacing.swift`）
 *
 * 一切 padding / spacing / cornerRadius 优先使用本文件，不写魔法数。
 * iOS 的 pt 在 Android 上 1:1 映射为 dp。
 */
object DSSpacing {
    /** 4dp - 极小（chip 内部图标与文字间距） */
    val xs: Dp = 4.dp
    /** 8dp - 小（图标与文字、紧凑间距） */
    val sm: Dp = 8.dp
    /** 12dp - 中小（卡片内部 padding） */
    val md: Dp = 12.dp
    /** 16dp - 中（页面横向 padding 默认值） */
    val lg: Dp = 16.dp
    /** 24dp - 大（区块之间） */
    val xl: Dp = 24.dp
    /** 32dp - 特大（章节之间） */
    val xxl: Dp = 32.dp
    /** 48dp - 巨大（页面顶部安全间距等） */
    val xxxl: Dp = 48.dp

    /** 圆角 */
    object Radius {
        /** 6dp - 极小（小 badge） */
        val xs: Dp = 6.dp
        /** 10dp - 小（chip / 小按钮） */
        val sm: Dp = 10.dp
        /** 14dp - 中（普通卡片） */
        val md: Dp = 14.dp
        /** 20dp - 大（Hero 卡 / 大按钮） */
        val lg: Dp = 20.dp
        /** 28dp - 特大（弹窗 / sheet 顶部） */
        val xl: Dp = 28.dp
        /** 完全圆形（pill / 头像） */
        val pill: Dp = 999.dp
    }

    /** 阴影预设（对应 iOS dsShadow） */
    object Shadow {
        /** 卡片轻阴影 */
        val card = ShadowSpec(elevation = 4.dp)
        /** 浮起元素（FAB / Toast） */
        val elevated = ShadowSpec(elevation = 12.dp)
        /** Hero 深色卡片用 */
        val hero = ShadowSpec(elevation = 16.dp)
    }

    data class ShadowSpec(val elevation: Dp)
}
