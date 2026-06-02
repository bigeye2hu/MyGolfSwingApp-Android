package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.model.V2CategoryItem
import com.mygolfswing.app.data.model.V2UserCategoryData
import com.mygolfswing.app.data.model.V2UserNode
import com.mygolfswing.app.data.repo.V2CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 分类 / 标签 Store（对齐 iOS `UnifiedCategoryBridge`）。
 */
object V2CategoryStore {
    private lateinit var repo: V2CategoryRepository

    private val _groups = MutableStateFlow<List<V2CategoryGroup>>(emptyList())
    val groups: StateFlow<List<V2CategoryGroup>> = _groups.asStateFlow()

    private var userData: V2UserCategoryData? = null
    /** 当前内存中加载的分类所属 userId；登出 / 切换账号时置 null。 */
    private var activeUserId: String? = null

    fun init(repository: V2CategoryRepository) {
        if (::repo.isInitialized) return
        repo = repository
        _groups.value = repo.loadDefaultGroups()
    }

    fun loadDefaultGuest() {
        userData = null
        activeUserId = null
        _groups.value = repo.loadDefaultGroups()
    }

    suspend fun loadForUser(userId: String) {
        val cloud = repo.fetchUserCategory(userId)
        val data = cloud ?: repo.createUserDataFromDefault(userId).also {
            repo.saveUserCategory(it)
        }
        userData = data
        activeUserId = userId
        _groups.value = repo.groupsFromUserData(data)
    }

    fun clearUserData() {
        userData = null
        activeUserId = null
        _groups.value = repo.loadDefaultGroups()
    }

    fun isLoadedFor(userId: String): Boolean = activeUserId == userId

    fun getAllGroups(): List<V2CategoryGroup> = _groups.value

    fun getAllItems(): List<V2CategoryItem> = _groups.value.flatMap { it.items }

    fun tagNamesFor(ids: Set<String>): List<String> =
        getAllItems().filter { it.id in ids }.map { it.name }

    fun groupForItemId(itemId: String): V2CategoryGroup? =
        _groups.value.firstOrNull { group -> group.items.any { it.id == itemId } }

    fun addCustomNode(name: String, groupId: String, color: String = "10B981") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val data = userData
        if (data == null) {
            // 访客模式：仅内存追加，不落云
            appendGuestNode(trimmed, groupId, color)
            return
        }
        val now = System.currentTimeMillis()
        val nodeId = "custom_${UUID.randomUUID().toString().take(8)}"
        val node = V2UserNode(
            id = nodeId,
            name = trimmed,
            color = color,
            source = "user",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        val idx = data.nodeGroups.indexOfFirst { it.id == groupId }
        if (idx < 0) return
        val updatedGroups = data.nodeGroups.toMutableList()
        updatedGroups[idx] = updatedGroups[idx].copy(
            nodes = updatedGroups[idx].nodes + node,
            updatedAt = now,
        )
        val updated = data.copy(nodeGroups = updatedGroups, lastSyncedAt = now)
        userData = updated
        _groups.value = repo.groupsFromUserData(updated)
        ServiceLocator.appScope.launch {
            repo.saveUserCategory(updated)
        }
    }

    private fun appendGuestNode(name: String, groupId: String, color: String) {
        val nodeId = "custom_${UUID.randomUUID().toString().take(8)}"
        _groups.value = _groups.value.map { group ->
            if (group.id != groupId) group
            else group.copy(
                items = group.items + V2CategoryItem(
                    id = nodeId,
                    name = name,
                    color = color,
                    groupId = groupId,
                    source = "user",
                ),
            )
        }
    }
}
