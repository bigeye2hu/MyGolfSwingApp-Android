package com.mygolfswing.app.data.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * V2 广场视频（对齐 iOS `V2PlazaVideo`）。
 *
 * 列表数据来自云端 `videoPlaza`；仅在网络失败时回退 mock。
 */
data class V2PlazaVideo(
    val id: UUID = UUID.randomUUID(),
    /** 云端原始 videoId（跳转/收藏） */
    val sourceVideoId: String? = null,
    val sourceCoachId: String? = null,
    val sourceCategoryId: String? = null,
    val sourceParentCategoryId: String? = null,
    val title: String,
    val durationSec: Double,
    val topic: V2Topic,
    val categoryDisplayName: String? = null,
    val coach: V2PlazaCoach,
    val viewCount: Int,
    val saveCount: Int,
    val thumbnailKind: ThumbnailKind,
    /** 真实封面；为空时用 [thumbnailKind] 渐变 */
    val thumbnailUrl: String? = null,
    val videoUrl: String,
    val publishedAtMs: Long = System.currentTimeMillis(),
    val isNew: Boolean = false,
    val isSaved: Boolean = false,
    val isFeaturedToday: Boolean = false,
) {
    /** 收藏 / 本地缓存用的云端 videoId */
    fun saveKey(): String = sourceVideoId?.takeIf { it.isNotBlank() } ?: id.toString()

    enum class ThumbnailKind {
        GREEN_SWING, SAND_WARM, MINT_TIP, EARTH_GEAR, FITNESS_ROSE, SHORT_AMBER;

        /** 缩略图渐变色（对齐 iOS 同名 case） */
        val gradient: List<Color>
            get() = when (this) {
                GREEN_SWING -> listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF3A7A5C))
                SAND_WARM -> listOf(Color(0xFF6B5A3A), Color(0xFF8A7455), Color(0xFF4A7A5A))
                MINT_TIP -> listOf(Color(0xFF4A7A65), Color(0xFF6FA08A), Color(0xFFE8F0EC))
                EARTH_GEAR -> listOf(Color(0xFF5A4A2A), Color(0xFF8A6A3A), Color(0xFF2D6A4F))
                FITNESS_ROSE -> listOf(Color(0xFF7A3F4A), Color(0xFFA0566B), Color(0xFF6B5A3A))
                SHORT_AMBER -> listOf(Color(0xFFB07A28), Color(0xFFD4A04A), Color(0xFF1B4332))
            }
    }
}

data class V2PlazaCoach(
    val displayName: String,
    val initialChar: String,
    val avatarTint: AvatarTint,
) {
    enum class AvatarTint {
        GREEN, GOLD, SAND, MIST;

        val gradient: List<Color>
            get() = when (this) {
                GREEN -> listOf(Color(0xFF2D6A4F), Color(0xFF4A8B6A))
                GOLD -> listOf(Color(0xFFB07A28), Color(0xFFD4A04A))
                SAND -> listOf(Color(0xFF5A4A2A), Color(0xFF8A6A3A))
                MIST -> listOf(Color(0xFF4A7A65), Color(0xFF6FA08A))
            }
    }
}

data class V2PlazaCategoryChip(
    val id: String,
    val name: String,
    val topic: V2Topic,
    val sortOrder: Int = 0,
    val parentId: String = "",
) {
    val isTopLevel: Boolean get() = parentId.isBlank()
}
