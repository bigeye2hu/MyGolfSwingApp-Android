package com.mygolfswing.app.data.model

/** 视图层分类项（对齐 iOS `CategoryItem`） */
data class V2CategoryItem(
    val id: String,
    val name: String,
    val color: String? = null,
    val groupId: String,
    val source: String? = null,
)

/** 视图层分类组（对齐 iOS `CategoryGroup`） */
data class V2CategoryGroup(
    val id: String,
    val name: String,
    val icon: String? = null,
    val items: List<V2CategoryItem>,
    val source: String? = null,
)

/** 云端用户分类数据（对齐 iOS `UserCategoryData`） */
data class V2UserCategoryData(
    val userId: String,
    var schemaVersion: String,
    var lastSyncedAt: Long,
    var nodeGroups: List<V2UserNodeGroup>,
    var trees: List<V2UserTree> = emptyList(),
    var customNodeGroups: List<V2UserNodeGroup> = emptyList(),
    var customTrees: List<V2UserTree> = emptyList(),
)

data class V2UserNodeGroup(
    val id: String,
    var name: String,
    var color: String,
    var icon: String? = null,
    var bindView: String? = null,
    var nodes: List<V2UserNode>,
    var source: String = "system",
    var isActive: Boolean = true,
    var isDeprecated: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)

data class V2UserNode(
    val id: String,
    var name: String,
    var color: String,
    var hotspotId: String? = null,
    var source: String = "system",
    var isActive: Boolean = true,
    var usageCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)

data class V2UserTree(
    val id: String,
    var name: String,
    var description: String? = null,
    var rootNode: String? = null,
    var childGroup: String? = null,
    var customChildren: List<String> = emptyList(),
    var viewType: String = "list",
    var bindToPages: List<String> = emptyList(),
    var source: String = "system",
    var isActive: Boolean = true,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)
