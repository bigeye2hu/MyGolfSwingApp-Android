package com.mygolfswing.app.data.model

/**
 * V2 数据模型层共享小类型（对齐 iOS `V2SharedTypes.swift`）。
 */

enum class V2PinState { NONE, PINNED, CANCELLING }

enum class V2MemoSource { SELF_RECORD, PLAZA_SAVE, TEXT_INPUT, SHARE }

enum class V2MemoKind { VIDEO, TEXT, MIXED }

enum class V2MemoThumbnailKind { FAIRWAY, SAND, GREEN_GRASS, SUNSET, MIST, FOREST }

enum class V2MemoAccent(val displayName: String) {
    BREAKTHROUGH("突破 ⚡"),
    INSIGHT("心法 ✨"),
    REFLECTION("反思"),
    COACH_NOTE("教练点评"),
}

/** 训练话题（含 ALL 共 9 个） */
enum class V2Topic(val displayName: String) {
    ALL("全部"),
    SWING("挥杆"),
    PUTTING("推杆"),
    SHORT_GAME("短杆"),
    SAND("沙坑"),
    MIND("心法"),
    FITNESS("体能"),
    GEAR("装备");

    companion object {
        /** 可用于业务归类（不含 ALL） */
        val assignable: List<V2Topic> get() = entries.filter { it != ALL }
    }
}
