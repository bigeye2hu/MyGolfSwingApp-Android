package com.mygolfswing.app.ui.interaction

import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset
import com.mygolfswing.app.data.coach.V2CoachUserSummary
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.data.coach.V2_WORKSPACE_DRAFT_LIFETIME_MILLIS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class V2WorkspaceThreadChromeParityTest {
    @Test
    fun threadTopBarUsesTheIosInlineNavigationHeight() {
        val source = listOf(
            java.io.File("app/src/main/java/com/mygolfswing/app/ui/interaction/V2StudentInteractionScreen.kt"),
            java.io.File("../app/src/main/java/com/mygolfswing/app/ui/interaction/V2StudentInteractionScreen.kt"),
        ).first { it.isFile }.readText()
        val topBar = source.substringAfter("internal fun WorkspaceThreadTopBar(")
            .substringBefore("internal fun WorkspaceThreadTabBar(")

        assertTrue(topBar.contains("barHeight: androidx.compose.ui.unit.Dp = DSLayout.topBarHeight"))
        assertTrue(topBar.contains(".height(barHeight)"))
        assertTrue(topBar.contains(".testTag(\"workspace-thread-top-bar\")"))
        assertFalse(topBar.contains(".height(56.dp)"))
    }

    @Test
    fun titleFollowsIosPerspectiveAndRelationshipRules() {
        assertEquals("练习详情", workspaceThreadTitle("大眼二胡2", coachPerspective = false, friendSpace = false))
        assertEquals("大眼二胡2", workspaceThreadTitle("大眼二胡2", coachPerspective = true, friendSpace = false))
        assertEquals("球友", workspaceThreadTitle(" 球友 ", coachPerspective = false, friendSpace = true))
        assertEquals("好友交流", workspaceThreadTitle("", coachPerspective = true, friendSpace = true))
        assertEquals("教学交流", workspaceThreadTitle("", coachPerspective = true, friendSpace = false))
    }

    @Test
    fun subtitlePreservesModePrivacyAndReadOnlyState() {
        val practice = item(kind = "student_practice")
        val watching = item(kind = "coach_post")

        assertEquals(
            "刚刚",
            workspaceThreadSubtitle(friendSpace = false, connectionStatus = "active", source = practice),
        )
        assertEquals(
            "私密好友空间 · 刚刚",
            workspaceThreadSubtitle(friendSpace = true, connectionStatus = "active", source = watching),
        )
        assertEquals(
            "历史教学空间 · 只读",
            workspaceThreadSubtitle(friendSpace = false, connectionStatus = "disconnected", source = practice),
        )
        assertEquals(
            "历史好友空间 · 只读",
            workspaceThreadSubtitle(friendSpace = true, connectionStatus = "disconnected", source = watching),
        )
    }

    @Test
    fun sourceCardCopyTagsAndAspectRatioFollowIosContentTab() {
        val source = V2CoachWorkspaceItem(
            itemId = "practice",
            connectionId = "space",
            senderRole = "student",
            title = " ",
            createdAt = null,
            width = 720,
            height = 1280,
            sender = V2CoachUserSummary(userId = "student", nickname = "小林"),
            primaryAsset = V2CoachWorkspaceAsset(
                assetId = "video",
                tags = listOf(" 下杆 ", "节奏", "下杆", ""),
            ),
        )

        assertEquals("复练视频", workspaceThreadSourceTitle(source, friendSpace = false))
        assertEquals("练习记录", workspaceThreadSourceTitle(source, friendSpace = true))
        assertEquals("记录者 小林 · 刚刚", workspaceThreadSourceMetadata(source, friendSpace = false))
        assertEquals(listOf("下杆", "节奏"), workspaceThreadSourceTags(source))
        assertEquals(0.5625f, workspaceThreadVideoAspectRatio(source), 0.0001f)
        assertEquals(16f / 9f, workspaceThreadVideoAspectRatio(item(kind = "coach_post")), 0.0001f)
    }

    @Test
    fun resourceCardCopyDistinguishesReferenceAndRecordedAnalysis() {
        assertEquals(
            "引用素材",
            workspaceThreadResourceSourceLabel(V2CoachWorkspaceAsset(assetId = "library", sourceKind = "library")),
        )
        assertEquals(
            "视频讲解",
            workspaceThreadResourceSourceLabel(V2CoachWorkspaceAsset(assetId = "analysis", sourceKind = "analysis_recording")),
        )
        assertEquals(
            "交流视频",
            workspaceThreadResourceSourceLabel(V2CoachWorkspaceAsset(assetId = "recording", sourceKind = "coach_recording")),
        )
        assertEquals(
            "示范动作",
            workspaceThreadResourceTitle(V2CoachWorkspaceAsset(assetId = "named", title = " 示范动作 ")),
        )
        assertEquals(
            "交流视频",
            workspaceThreadResourceTitle(V2CoachWorkspaceAsset(assetId = "untitled")),
        )
    }

    @Test
    fun savedConversationVideoReportsTheSameResultAndRefreshesResourcesAsIos() {
        val source = listOf(
            java.io.File("app/src/main/java/com/mygolfswing/app/ui/interaction/V2StudentInteractionScreen.kt"),
            java.io.File("../app/src/main/java/com/mygolfswing/app/ui/interaction/V2StudentInteractionScreen.kt"),
        ).first { it.isFile }.readText()

        assertTrue(source.contains("var saveAssetNotice by remember(source.itemId)"))
        assertTrue(source.contains("\"已保存到私有素材，并同步到本页“资料”。\""))
        assertTrue(source.contains("coachThrowableError(\"暂时无法保存，请稍后重试。\""))
        assertTrue(source.contains("WorkspaceAssetSaveResultDialog("))
        assertTrue(source.contains("\"素材保存结果\""))
        assertTrue(source.contains("\"知道了\""))
        assertTrue(source.contains("workspaceThread(connectionId, source.itemId)"))
    }

    @Test
    fun primaryVideoReplacementProtectsTheExistingIosDraft() {
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.USE_DIRECTLY,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = null,
                replacementSourceKind = "album",
                coachPerspective = true,
                relatedCount = 0,
            ),
        )
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.DEMOTE_CURRENT_TO_RELATED,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = "coach_recording",
                replacementSourceKind = "analysis_recording",
                coachPerspective = true,
                relatedCount = 2,
            ),
        )
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = "coach_recording",
                replacementSourceKind = "album",
                coachPerspective = true,
                relatedCount = 0,
            ),
        )
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = "coach_recording",
                replacementSourceKind = "analysis_recording",
                coachPerspective = true,
                relatedCount = 3,
            ),
        )
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = "student_recording",
                replacementSourceKind = "analysis_recording",
                coachPerspective = false,
                relatedCount = 0,
            ),
        )
        assertEquals(
            WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING,
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = "voice_recording",
                replacementSourceKind = "analysis_recording",
                coachPerspective = true,
                relatedCount = 0,
            ),
        )
    }

    @Test
    fun discussionMediaAndReferencesFollowIosPresentationRules() {
        assertTrue(
            workspaceAssetUsesCompactPresentation(
                V2CoachWorkspaceAsset(assetId = "library", sourceKind = "library", savedToLibrary = true),
            ),
        )
        assertTrue(
            workspaceAssetUsesCompactPresentation(
                V2CoachWorkspaceAsset(assetId = "album", sourceKind = "album", savedToLibrary = false),
            ),
        )
        assertFalse(
            workspaceAssetUsesCompactPresentation(
                V2CoachWorkspaceAsset(assetId = "analysis", sourceKind = "analysis_recording", savedToLibrary = true),
            ),
        )

        val video = V2CoachWorkspaceItem(
            itemId = "video",
            connectionId = "space",
            hasVideo = true,
            text = "下杆时先稳住重心",
        )
        assertEquals(
            "原视频 · 00:12",
            workspaceReplyReferenceTitle(
                item = V2CoachWorkspaceItem("reply", "space", replyAtSeconds = 12.3),
                referencedItem = video,
            ),
        )
        assertEquals("下杆时先稳住重心", workspaceReplyReferencePreview(video))
        assertEquals("引用内容已失效", workspaceReplyReferenceTitle(item("coach_post"), null))
        assertEquals("原内容已不可用", workspaceReplyReferencePreview(null))
    }

    @Test
    fun discussionGroupingAndTimeDividersMatchIosThresholds() {
        val root = V2CoachWorkspaceItem("root", "space", createdAt = "2026-10-07T01:00:00Z")
        val replies = listOf(
            V2CoachWorkspaceItem("one", "space", senderRole = "student", createdAt = "2026-10-07T01:04:00Z"),
            V2CoachWorkspaceItem("two", "space", senderRole = "student", createdAt = "2026-10-07T01:08:59Z"),
            V2CoachWorkspaceItem("three", "space", senderRole = "student", createdAt = "2026-10-07T01:39:00Z"),
        )

        assertTrue(workspaceMessageStartsGroup(0, replies, friendSpace = false))
        assertFalse(workspaceMessageStartsGroup(1, replies, friendSpace = false))
        assertTrue(workspaceMessageStartsGroup(2, replies, friendSpace = false))
        assertFalse(workspaceShouldShowReplyTimeDivider(root, replies, 0))
        assertTrue(workspaceShouldShowReplyTimeDivider(root, replies, 2))
        assertEquals(
            "今天 09:39",
            workspaceConversationDateText(
                "2026-10-07T01:39:00Z",
                now = Instant.parse("2026-10-07T05:00:00Z"),
                zone = ZoneId.of("Asia/Shanghai"),
            ),
        )
    }

    @Test
    fun unreadBoundaryAndLatestReceiptStayOnTheConversationEdge() {
        val replies = listOf(
            V2CoachWorkspaceItem("read", "space", senderRole = "coach", isMine = false),
            V2CoachWorkspaceItem("mine", "space", senderRole = "student", isMine = true),
            V2CoachWorkspaceItem("unread", "space", senderRole = "coach", isMine = false),
            V2CoachWorkspaceItem("latest", "space", senderRole = "student", isMine = true),
        )
        val root = V2CoachWorkspaceItem(
            itemId = "root",
            connectionId = "space",
            senderRole = "student",
            studentHasUnread = true,
            studentLastViewedReplyId = "read",
            reviewState = "viewed",
        )

        assertEquals(2, workspaceFirstUnreadReplyIndex(root, replies, coachPerspective = false, friendSpace = false))
        assertEquals(1, workspaceUnreadReplyCount(replies, 2, coachPerspective = false, friendSpace = false))
        assertNull(workspaceReplyDeliveryStatus(root, replies[1], 1, replies, false, false))
        assertEquals("教练已查看", workspaceReplyDeliveryStatus(root, replies[3], 3, replies, false, false))
    }

    @Test
    fun composerDraftRetentionCopyMatchesIosTwentyFourHourPolicy() {
        val now = 1_800_000_000_000L

        assertEquals("正在保存草稿", workspaceDraftPersistenceText(0L, now))
        assertEquals(
            "草稿已超过保留时间",
            workspaceDraftPersistenceText(now - V2_WORKSPACE_DRAFT_LIFETIME_MILLIS, now),
        )
        assertEquals(
            "草稿即将到期",
            workspaceDraftPersistenceText(now - V2_WORKSPACE_DRAFT_LIFETIME_MILLIS + 30_000L, now),
        )
        assertEquals(
            "草稿30分钟内可恢复",
            workspaceDraftPersistenceText(now - V2_WORKSPACE_DRAFT_LIFETIME_MILLIS + 30L * 60L * 1_000L, now),
        )
        assertEquals(
            "草稿2小时内可恢复",
            workspaceDraftPersistenceText(now - V2_WORKSPACE_DRAFT_LIFETIME_MILLIS + 2L * 60L * 60L * 1_000L, now),
        )
    }

    @Test
    fun composerDeliveryCopyTracksTheRealQueuePhase() {
        assertEquals("正在发送", workspaceDeliveryStateText(null, 0.0))
        assertEquals("正在准备", workspaceDeliveryStateText(V2CoachDeliveryPhase.PREPARING, 0.0))
        assertEquals("上传中 42%", workspaceDeliveryStateText(V2CoachDeliveryPhase.UPLOADING, 0.429))
        assertEquals("正在确认", workspaceDeliveryStateText(V2CoachDeliveryPhase.PUBLISHING, 1.0))
    }

    private fun item(kind: String) = V2CoachWorkspaceItem(
        itemId = "item",
        connectionId = "space",
        kind = kind,
    )
}
