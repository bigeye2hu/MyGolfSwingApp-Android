package com.mygolfswing.app.data.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * V2 Memo 数据模型（对齐 iOS `V2Memo.swift`）。
 *
 * - 三态形态：[V2MemoKind.VIDEO] / [V2MemoKind.TEXT] / [V2MemoKind.MIXED]
 * - 通过 [V2MemoSource] 区分来源
 * - 通过 [linkedGoalIds] 与 [V2Goal] 形成多对多关系
 */
data class V2Memo(
    val id: UUID = UUID.randomUUID(),
    var kind: V2MemoKind,
    var source: V2MemoSource,

    var videoUrl: String? = null,
    var thumbnailUrl: String? = null,
    var durationSec: Double? = null,

    var text: String? = null,
    var title: String? = null,

    var topic: V2Topic,
    var tagIds: List<String> = emptyList(),
    var tags: List<String> = emptyList(),

    var pinState: V2PinState = V2PinState.NONE,
    /** 视频收藏与 Memo 置顶独立；null 兼容旧的本地 V2 快照。 */
    var isFavorite: Boolean? = null,
    var linkedGoalIds: List<UUID> = emptyList(),

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    /** 是否在卡片右上角显示金色 NEW 圆点 */
    var isNew: Boolean = false,
    /** 米黄色 chip：突破 / 心法 / 反思 / 教练点评 */
    var accent: V2MemoAccent? = null,
    /** mixed 卡上方加粗的小标题；也用作 video 卡的标题 */
    var displayTitle: String? = null,
    /** 卡片底部 meta 行（例："今天 14:32 · 7号铁 · 50球"） */
    var metaText: String? = null,
    /** 文字卡的副引言段 */
    var subText: String? = null,
    /** 缩略图占位色调 */
    var thumbnailKind: V2MemoThumbnailKind = V2MemoThumbnailKind.FAIRWAY,
    /** 视频来源文案：拍摄 / 本地上传 / 在线 等（对齐 iOS `VideoMemo.trainingMethod`） */
    var trainingMethod: String? = null,
    var isOnlineVideo: Boolean = false,
    var platform: String? = null,
    var plazaVideoId: UUID? = null,
    /** 文字 Memo 优先级 raw（low/medium/high） */
    var priorityRaw: String = "medium",
    /** iOS InsightCard 旧附件关系；详情页只读展示数量，编辑时原样保留。 */
    var attachedVideoIds: List<UUID> = emptyList(),
    var attachedImageIds: List<UUID> = emptyList(),
) {
    val isPlazaVideo: Boolean get() = plazaVideoId != null || source == V2MemoSource.PLAZA_SAVE
    val wordCount: Int get() = text?.length ?: 0
    val hasVideo: Boolean get() = kind == V2MemoKind.VIDEO || kind == V2MemoKind.MIXED
    val hasText: Boolean get() = kind == V2MemoKind.TEXT || kind == V2MemoKind.MIXED
    val isPinned: Boolean get() = pinState == V2PinState.PINNED || pinState == V2PinState.CANCELLING
    val isLinkedToAnyGoal: Boolean get() = linkedGoalIds.isNotEmpty()

    companion object {
        fun textMemo(
            title: String,
            body: String,
            topic: V2Topic = V2Topic.MIND,
            tags: List<String> = emptyList(),
            linkedGoalIds: List<UUID> = emptyList(),
            accent: V2MemoAccent? = null,
            subText: String? = null,
        ) = V2Memo(
            kind = V2MemoKind.TEXT,
            source = V2MemoSource.TEXT_INPUT,
            title = title,
            text = body,
            topic = topic,
            tags = tags,
            linkedGoalIds = linkedGoalIds,
            displayTitle = title,
            accent = accent,
            subText = subText,
        )

        fun videoMemo(
            displayTitle: String,
            videoUrl: String? = null,
            durationSec: Double? = null,
            topic: V2Topic = V2Topic.SWING,
            tags: List<String> = emptyList(),
            metaText: String? = null,
            thumbnailKind: V2MemoThumbnailKind = V2MemoThumbnailKind.FAIRWAY,
        ) = V2Memo(
            kind = V2MemoKind.VIDEO,
            source = V2MemoSource.SELF_RECORD,
            displayTitle = displayTitle,
            videoUrl = videoUrl,
            durationSec = durationSec,
            topic = topic,
            tags = tags,
            metaText = metaText,
            thumbnailKind = thumbnailKind,
        )

        fun mixedMemo(
            displayTitle: String,
            body: String,
            videoUrl: String? = null,
            durationSec: Double? = null,
            topic: V2Topic = V2Topic.SWING,
            tags: List<String> = emptyList(),
            metaText: String? = null,
            thumbnailKind: V2MemoThumbnailKind = V2MemoThumbnailKind.FAIRWAY,
        ) = V2Memo(
            kind = V2MemoKind.MIXED,
            source = V2MemoSource.SELF_RECORD,
            displayTitle = displayTitle,
            text = body,
            videoUrl = videoUrl,
            durationSec = durationSec,
            topic = topic,
            tags = tags,
            metaText = metaText,
            thumbnailKind = thumbnailKind,
        )
    }
}
