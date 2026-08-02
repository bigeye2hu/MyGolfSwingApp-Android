package com.mygolfswing.app.ui.memo

import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.model.V2Topic
import org.junit.Assert.assertEquals
import org.junit.Test

class V2MemoSecondaryListFilterTest {

    @Test
    fun textListFiltersMatchIosSecondaryListChips() {
        val mind = V2Memo.textMemo(
            title = "推杆节奏",
            body = "节奏更稳定",
            topic = V2Topic.MIND,
            tags = listOf("果岭"),
        )
        val reflection = V2Memo.textMemo(
            title = "铁杆反思",
            body = "上杆太快",
            topic = V2Topic.SWING,
            accent = V2MemoAccent.REFLECTION,
            tags = listOf("铁杆"),
        )
        val breakthrough = V2Memo.textMemo(
            title = "突破记录",
            body = "今天找到释放节奏",
            topic = V2Topic.SWING,
            accent = V2MemoAccent.BREAKTHROUGH,
        )
        val ai = V2Memo.textMemo(
            title = "AI 洞察",
            body = "近期节奏稳定",
            topic = V2Topic.MIND,
            accent = V2MemoAccent.INSIGHT,
        )
        val pinned = V2Memo.textMemo(
            title = "收藏心得",
            body = "赛前例行流程",
            topic = V2Topic.SWING,
        ).copy(pinState = V2PinState.PINNED)
        val memos = listOf(mind, reflection, breakthrough, ai, pinned)

        assertEquals(memos, textMemoListVisibleMemos(memos, "all"))
        assertEquals(listOf(mind, ai), textMemoListVisibleMemos(memos, "mind"))
        assertEquals(listOf(reflection), textMemoListVisibleMemos(memos, "reflection"))
        assertEquals(listOf(breakthrough), textMemoListVisibleMemos(memos, "breakthrough"))
        assertEquals(listOf(ai), textMemoListVisibleMemos(memos, "ai"))
        assertEquals(listOf(pinned), textMemoListVisibleMemos(memos, "pinned"))
    }

    @Test
    fun videoListFiltersMatchIosSecondaryListChips() {
        val shoot = V2Memo.videoMemo(
            displayTitle = "正面挥杆",
            videoUrl = "file:///private/shoot.mp4",
            topic = V2Topic.SWING,
        ).copy(trainingMethod = "拍摄")
        val upload = V2Memo.videoMemo(
            displayTitle = "本地上传正面角度",
            videoUrl = "file:///private/upload.mp4",
            topic = V2Topic.SWING,
        ).copy(trainingMethod = "本地上传")
        val online = V2Memo.videoMemo(
            displayTitle = "球场下场复盘",
            videoUrl = "https://example.com/video.mp4",
            topic = V2Topic.SWING,
        ).copy(
            isOnlineVideo = true,
            platform = "在线视频",
            trainingMethod = "在线",
        )
        val plaza = V2Memo.videoMemo(
            displayTitle = "广场收藏复盘",
            videoUrl = "https://example.com/plaza.mp4",
            topic = V2Topic.SWING,
        ).copy(source = V2MemoSource.PLAZA_SAVE)
        val memos = listOf(shoot, upload, online, plaza)

        assertEquals(memos, videoMemoListVisibleMemos(memos, "all"))
        assertEquals(listOf(shoot), videoMemoListVisibleMemos(memos, "shoot"))
        assertEquals(listOf(upload), videoMemoListVisibleMemos(memos, "upload"))
        assertEquals(listOf(online), videoMemoListVisibleMemos(memos, "online"))
        assertEquals(listOf(plaza), videoMemoListVisibleMemos(memos, "plaza"))
    }

    @Test
    fun secondaryListFiltersIgnoreUnknownFilterIds() {
        val text = V2Memo.textMemo("训练心法", "放松握杆", topic = V2Topic.MIND)
        val video = V2Memo.videoMemo("挥杆回看")

        assertEquals(listOf(text), textMemoListVisibleMemos(listOf(text), "future"))
        assertEquals(listOf(video), videoMemoListVisibleMemos(listOf(video), "future"))
    }
}
