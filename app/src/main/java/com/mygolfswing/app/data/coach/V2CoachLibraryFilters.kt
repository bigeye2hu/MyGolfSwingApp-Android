package com.mygolfswing.app.data.coach

enum class V2CoachLibrarySort(val title: String) {
    RECENT("最近使用"),
    NEWEST("最新优先"),
    MOST_USED("复用最多"),
    TITLE("标题"),
}

fun List<V2CoachLibraryAsset>.libraryCategories(): List<String> = buildList {
    val fixed = listOf("全部", "视频", "图片", "常用", "未分类")
    addAll(fixed)
    addAll(
        this@libraryCategories.flatMap { it.tags }.map { it.trim() }
            .filter { it.isNotEmpty() && it !in fixed }.distinct().sorted(),
    )
}

fun V2CoachLibraryAsset.isLibraryVideoAsset(): Boolean =
    sourceKind.lowercase() !in setOf("voice", "audio", "voice_recording") &&
        (!videoUrl.isNullOrBlank() || !coverUrl.isNullOrBlank() || (durationSeconds ?: 0.0) > 0.0)

fun List<V2CoachLibraryAsset>.filterAndSortLibrary(
    query: String,
    category: String,
    sort: V2CoachLibrarySort,
): List<V2CoachLibraryAsset> = filter { asset ->
    val matchesCategory = when (category) {
        "全部" -> true
        "常用" -> asset.useCount > 0
        "未分类" -> asset.tags.isEmpty()
        "视频" -> asset.isLibraryVideoAsset()
        "图片", "图文" -> !asset.isLibraryVideoAsset()
        else -> category in asset.tags
    }
    val needle = query.trim()
    matchesCategory && (needle.isEmpty() || asset.title.contains(needle, ignoreCase = true) ||
        asset.tags.any { it.contains(needle, ignoreCase = true) })
}.let { values ->
    when (sort) {
        V2CoachLibrarySort.RECENT -> values.sortedByDescending {
            it.lastUsedAt ?: it.updatedAt ?: it.createdAt.orEmpty()
        }
        V2CoachLibrarySort.NEWEST -> values.sortedByDescending {
            it.createdAt ?: it.updatedAt.orEmpty()
        }
        V2CoachLibrarySort.MOST_USED -> values.sortedWith(
            compareByDescending<V2CoachLibraryAsset> { it.useCount }.thenBy { it.title },
        )
        V2CoachLibrarySort.TITLE -> values.sortedBy { it.title.lowercase() }
    }
}

fun List<V2CoachLibraryAsset>.prioritizeLibraryShareDrafts(
    draftAssetIds: Set<String>,
): List<V2CoachLibraryAsset> = withIndex().sortedWith(
    compareByDescending<IndexedValue<V2CoachLibraryAsset>> { it.value.assetId in draftAssetIds }
        .thenBy { it.index },
).map(IndexedValue<V2CoachLibraryAsset>::value)
