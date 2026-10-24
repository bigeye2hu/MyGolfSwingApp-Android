package com.mygolfswing.app.ui.common

import com.mygolfswing.app.data.store.V2UserProfile

/** 首页问候区展示名（对齐 iOS `V2HomeHeader.displayName`：nickname → username → 兜底） */
fun V2UserProfile.headerDisplayName(): String {
    if (!isLoggedIn) return "挥呗游客"
    nickname.trim()
        .takeIf { it.isNotEmpty() && it != "未登录" }
        ?.let { return it }
    username.removePrefix("@").trim()
        .takeIf { it.isNotEmpty() && it != "guest" }
        ?.let { return it }
    return "高尔夫球友"
}
