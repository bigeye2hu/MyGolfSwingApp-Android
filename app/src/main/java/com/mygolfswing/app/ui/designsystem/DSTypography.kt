package com.mygolfswing.app.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

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

    private fun style(
        size: TextUnit,
        lineHeight: TextUnit,
        weight: FontWeight,
        family: FontFamily = FontFamily.Default,
    ) = TextStyle(
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = weight,
        fontFamily = family,
    )

    /** 28sp Bold - 问候语 / 页面标语 */
    val display = style(28.sp, 34.sp, FontWeight.Bold)

    /** 20sp Bold - 页面 / 区块大标题 */
    val titleLarge = style(20.sp, 25.sp, FontWeight.Bold)

    /** 17sp SemiBold - 卡片大标题 */
    val titleMedium = style(17.sp, 22.sp, FontWeight.SemiBold)

    /** 15sp Regular - 正文 */
    val body = style(15.sp, 20.sp, FontWeight.Normal)

    /** 15sp SemiBold - 强调正文 */
    val bodyBold = style(15.sp, 20.sp, FontWeight.SemiBold)

    /** 17sp SemiBold - 主按钮 */
    val button = style(17.sp, 22.sp, FontWeight.SemiBold)

    /** 13sp SemiBold - 卡片标题（视频卡 / 文字卡） */
    val cardTitle = style(13.sp, 18.sp, FontWeight.SemiBold)

    /** 12sp Medium - 首页副标题 */
    val meta = style(12.sp, 16.sp, FontWeight.Medium)

    /**
     * 截图视觉档。上面的 token 仍按 iOS 点数 1:1。
     * 安卓系统中文在相同 sp 下比苹方更粗更高，对照截图后页面用这一档。
     * 不关闭系统字体缩放。行高贴近字号，避免行盒把字再撑高。
     */
    val visualTitle = style(18.sp, 22.sp, FontWeight.Bold)

    /** 首页问候。20 在真机中文上偏大，收到 16。 */
    val visualGreeting = style(16.sp, 20.sp, FontWeight.SemiBold)
    val visualHeroTitle = style(16.sp, 20.sp, FontWeight.SemiBold)
    val visualCardTitle = style(13.sp, 16.sp, FontWeight.Bold)
    val visualBody = style(13.sp, 16.sp, FontWeight.Medium)
    val visualMeta = style(11.sp, 14.sp, FontWeight.Medium)
    val visualChip = style(11.sp, 14.sp, FontWeight.SemiBold)

    /** 13sp Medium - 时间戳 / 副信息 */
    val caption = style(13.sp, 18.sp, FontWeight.Medium)

    /** 11sp Bold - chip 标签 */
    val label = style(11.sp, 14.sp, FontWeight.Bold)

    /** 10sp Medium - TabBar 文字 */
    val tab = style(10.sp, 12.sp, FontWeight.Medium)

    /** 38sp ExtraBold - Bento 大数字（iOS 用 SF Pro Rounded） */
    val bentoNumber = style(38.sp, 46.sp, FontWeight.ExtraBold, FontFamily.SansSerif)
}
