package com.mygolfswing.app.ui.goals

import com.mygolfswing.app.data.model.V2Memo

internal fun V2Memo.goalMemoDisplayTitle(): String =
    displayTitle ?: title ?: text?.take(16) ?: "未命名 Memo"

internal fun V2Memo.goalMemoSourceLabel(): String? = when {
    isPlazaVideo -> "广场视频"
    isOnlineVideo -> listOfNotNull(
        platform?.takeIf { it.isNotBlank() },
        trainingMethod?.takeIf { it.isNotBlank() },
    ).firstOrNull() ?: "在线视频"
    !trainingMethod.isNullOrBlank() -> trainingMethod
    else -> null
}
