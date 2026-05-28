package com.mygolfswing.app.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * V2 设计系统 - 字体 Token（对齐 iOS `DSTypography.swift`）
 *
 * iOS 端：
 * - 中文：PingFang SC（系统中文，使用 `.system` 自动命中）
 * - 英文：SF Pro Display
 * - 数字：SF Pro Rounded（design: .rounded）
 *
 * Android 端：
 * - 中文：默认 sans-serif（系统会用 Source Han Sans / 厂商中文字体）
 * - 数字：sans-serif（Android 无 Rounded 系统字体，后续可换成 Inter / Nunito 等 ttf）
 */
object DSTypography {

    /** 28sp Bold - 问候语 / 页面标语 */
    val display = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
    )

    /** 20sp Bold - 页面 / 区块大标题 */
    val titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
    )

    /** 17sp SemiBold - 卡片大标题 */
    val titleMedium = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
    )

    /** 15sp Regular - 正文 */
    val body = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default,
    )

    /** 15sp SemiBold - 强调正文 */
    val bodyBold = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
    )

    /** 17sp SemiBold - 主按钮 */
    val button = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
    )

    /** 13sp SemiBold - 卡片标题（视频卡 / 文字卡） */
    val cardTitle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
    )

    /** 13sp Medium - 时间戳 / 副信息 */
    val caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default,
    )

    /** 11sp Bold - chip 标签 */
    val label = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
    )

    /** 10sp Medium - TabBar 文字 */
    val tab = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default,
    )

    /** 38sp ExtraBold - Bento 大数字（iOS 用 SF Pro Rounded） */
    val bentoNumber = TextStyle(
        fontSize = 38.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.SansSerif,
    )
}
