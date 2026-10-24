package com.mygolfswing.app.ui.goals

import com.mygolfswing.app.data.model.V2Memo

internal fun V2Memo.goalMemoDisplayTitle(): String =
    displayTitle ?: title ?: text?.take(16) ?: "未命名 Memo"
