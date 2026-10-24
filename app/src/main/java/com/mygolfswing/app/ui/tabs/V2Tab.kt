package com.mygolfswing.app.ui.tabs

/**
 * V2 底部 5 Tab 枚举（对齐 iOS `V2BottomTabBar.Tab`）。
 *
 * 顺序 = 视觉顺序：home → memo → interaction (中间凸起) → tools → me。
 *
 * iOS 为兼容旧深链仍把工具项的内部枚举命名为 `plaza`，安卓同样保留 [PLAZA]，
 * 但它的用户可见页面和标签都是「工具」。
 */
enum class V2Tab {
    HOME, MEMO, INTERACTION, PLAZA, ME
}
