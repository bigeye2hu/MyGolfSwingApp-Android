package com.mygolfswing.app.ui.memo

import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent

fun V2Memo.isAiInsightMemo(): Boolean {
    val textBlob = listOfNotNull(displayTitle, title, metaText, subText)
        .plus(tags)
        .joinToString(" ")
    return textBlob.contains("AI", ignoreCase = true) || textBlob.contains("洞察")
}

fun V2Memo.accentDisplayLabel(): String = when (accent) {
    V2MemoAccent.INSIGHT -> if (isAiInsightMemo()) "AI 洞察" else "心法"
    else -> accent?.displayName.orEmpty()
}
