package com.mygolfswing.app.ui.plaza

import com.mygolfswing.app.data.model.V2PlazaVideo
import java.util.Locale

const val V2_PLAZA_PUBLIC_VIEWS_THRESHOLD: Int = 50
const val V2_PLAZA_PUBLIC_FAVORITES_THRESHOLD: Int = 5

fun v2PlazaCompactCount(count: Int): String = when {
    count >= 10_000 -> String.format(Locale.US, "%.1f万", count / 10_000.0)
    count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
}

fun V2PlazaVideo.publicHomeMetaText(): String =
    if (viewCount >= V2_PLAZA_PUBLIC_VIEWS_THRESHOLD) {
        "${v2PlazaCompactCount(viewCount)} 次观看"
    } else {
        categoryDisplayName ?: "精选推荐"
    }

fun V2PlazaVideo.publicGridPrimaryMetaText(): String =
    if (viewCount >= V2_PLAZA_PUBLIC_VIEWS_THRESHOLD) {
        v2PlazaCompactCount(viewCount)
    } else if (isNew) {
        "新发布"
    } else {
        categoryDisplayName ?: "精选"
    }

fun V2PlazaVideo.publicFavoriteText(): String =
    if (saveCount >= V2_PLAZA_PUBLIC_FAVORITES_THRESHOLD) {
        v2PlazaCompactCount(saveCount)
    } else {
        "收藏"
    }
