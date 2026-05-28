package com.mygolfswing.app.ui.designsystem

import androidx.compose.ui.graphics.Color

/**
 * V2 设计系统 - 色彩 Token（对齐 iOS `DSColors.swift`）
 *
 * 来源：以首页色卡为全局基准。
 * 命名空间：所有颜色通过 `DSColors.xxx` 访问。
 */
object DSColors {

    // ============ 中性底色 ============
    /** 全局米白底 */
    val warmWhite = Color(0xFFFAF7F0)
    /** 卡片白 */
    val cardWhite = Color(0xFFFFFFFF)
    /** 米卡底（文字 Memo / 装饰区） */
    val sandBeige = Color(0xFFF4ECDA)
    /** 浅雾绿（chip 已选 / 浅描边） */
    val mistGreen = Color(0xFFE8F0EC)

    // ============ 主色 - 球场绿系 ============
    /** 主绿（按钮、进度、强调） */
    val fairway = Color(0xFF2D6A4F)
    /** 深绿（Hero 卡片底、TabBar 选中） */
    val deepGreen = Color(0xFF1B4332)
    /** 暗深绿（最深一层渐变 / 不可点击文字） */
    val inkGreen = Color(0xFF1A2E22)

    // ============ 辅助色 ============
    /** 副文字 / 灰绿 */
    val shadowGray = Color(0xFF8A9E91)
    /** 警示 / 强调红 */
    val flagRed = Color(0xFFC0392B)
    /** 庆祝金（目标达成、今日精选） */
    val trophyGold = Color(0xFFC8930B)
    /** 亮金（Memo 卡 NEW 角标 / 突破/反思 chip 文字） */
    val goldBright = Color(0xFFD4A017)

    // ============ 描边 / 分割 ============
    /** 极淡描边线 */
    val borderLine = Color(0x0D000000) // black 5% opacity
    /** Memo 列表页分组吸顶 / 统计行 / 日期块底色 */
    val listMutedBg = Color(0xFFF5F0E8)
    /** Memo 列表页卡片描边色 */
    val listCardBorder = Color(0xFFD8E2DC)
    /** Memo 列表页 chip 浅绿底 */
    val chipGreenBg = Color(0xFFE9F4EE)
    /** 球场视频 chip 文字色（绿松石） */
    val leafGreen = Color(0xFF52B788)

    // ============ 语义化别名 ============
    object Semantic {
        val background = warmWhite
        val surface = cardWhite
        val surfaceMuted = sandBeige
        val primary = fairway
        val primaryDark = deepGreen
        val textPrimary = inkGreen
        val textSecondary = shadowGray
        val danger = flagRed
        val success = fairway
        val highlight = trophyGold
        val separator = borderLine
    }
}
