package com.mygolfswing.app.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 从当前 iOS 页面抽出的共用尺寸。
 *
 * pt 按 1:1 写成 dp。按钮和间距用这里的固定值；文字按内容撑开；
 * 图片和视频用约定比例；日历按周数和行高计算。不要按整页高度等比拉伸。
 *
 * 「在看」空态英雄卡是页面版本差异，不在这里改成间距问题。
 */
object DSLayout {
    val pageHorizontal: Dp = DSSpacing.lg
    val homeSectionSpacing: Dp = 12.dp
    val homeHeaderSpacing: Dp = 8.dp
    val practiceSectionSpacing: Dp = 14.dp
    val practicePageTop: Dp = 10.dp
    val practicePageBottom: Dp = DSSpacing.lg

    val primaryButtonHeight: Dp = 40.dp
    val compactButtonHeight: Dp = 30.dp
    val topBarHeight: Dp = 40.dp
    val iconButton: Dp = 34.dp
    val smallIcon: Dp = 16.dp
    val tabIcon: Dp = 16.dp

    val headerContentTop: Dp = 6.dp
    val headerContentBottom: Dp = 2.dp
    val tabContentTop: Dp = 6.dp
    val tabContentBottom: Dp = 4.dp

    val cardPadding: Dp = DSSpacing.md
    val searchFieldHeight: Dp = 34.dp
    val avatar: Dp = 48.dp

    /** 首页截图视觉档。同号 dp 在安卓上比 iOS 显得更大，只收首页这几处。 */
    val homeAvatar: Dp = 40.dp
    val homeToolCircle: Dp = 40.dp
    val homeToolImage: Dp = 20.dp
    val homeToolIcon: Dp = 14.dp
    val homeToolWidth: Dp = 58.dp

    val dateStripHeight: Dp = 64.dp
    val dateCellWidth: Dp = 42.dp
    val dateCellHeight: Dp = 54.dp
    val monthCellHeight: Dp = 36.dp
    val monthCellSpacing: Dp = 4.dp

    /** 挥杆缩略图容器比例，对应 iOS 竖幅格子。 */
    const val swingThumbAspectWidth: Float = 9f
    const val swingThumbAspectHeight: Float = 16f

    /** 球场底图只铺页面下半，顶部渐隐到暖白。 */
    val roomBackgroundHeight: Dp = 360.dp
    val roomBackgroundFadeHeight: Dp = 90.dp
}
