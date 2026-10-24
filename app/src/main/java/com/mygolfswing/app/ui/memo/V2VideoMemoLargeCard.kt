package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.coach.V2MemoDiscussionUpdate
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.Locale

enum class V2VideoSourceChipStyle { MUTED, GOLD, LEAF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2VideoMemoLargeCard(
    memo: V2Memo,
    chipText: String,
    chipStyle: V2VideoSourceChipStyle,
    metaText: String,
    modifier: Modifier = Modifier,
    guestPreview: Boolean = false,
    discussionUpdates: List<V2MemoDiscussionUpdate> = emptyList(),
    onOpen: () -> Unit = {},
    onOpenDiscussion: (connectionId: String, itemId: String) -> Unit = { _, _ -> },
) {
    val playable = remember(memo.videoUrl, guestPreview) { guestPreview || memoVideoPlayable(memo.videoUrl) }
    val needsRecovery = remember(memo.source, memo.isOnlineVideo, memo.isPlazaVideo, playable, guestPreview) {
        videoMemoNeedsRecovery(memo, guestPreview, playable)
    }
    var showDiscussions by remember { mutableStateOf(false) }
    val title = videoMemoCardTitle(memo)
    val note = videoMemoCardNote(memo, title, chipText)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(12.dp)
                .testTag("video-memo-card-body"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            VideoMemoThumbnail(memo = memo, playable = playable)
            Column(
                modifier = Modifier.weight(1f).heightIn(min = 100.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DSColors.inkGreen,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (memo.isFavorite == true) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "已收藏",
                            tint = DSColors.goldBright,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (memo.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "已置顶",
                            tint = DSColors.goldBright,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (memo.isLinkedToAnyGoal) {
                        Icon(
                            Icons.Filled.TrackChanges,
                            contentDescription = "已关联目标",
                            tint = DSColors.goldBright,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                note?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = DSColors.shadowGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (memo.tags.any(String::isNotBlank)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        memo.tags.asSequence().filter(String::isNotBlank).take(2).forEach { tag ->
                            Text(
                                tag,
                                color = DSColors.fairway,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(DSColors.mistGreen)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SourceChip(chipText, chipStyle)
                    Text(
                        "${metaText}保存",
                        fontSize = 10.sp,
                        color = DSColors.shadowGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        when {
            needsRecovery -> VideoMemoStatusRow(
                text = "视频待恢复",
                color = DSColors.goldBright,
                icon = { Icon(Icons.Filled.DownloadForOffline, null, modifier = Modifier.size(15.dp)) },
                onClick = onOpen,
                testTag = "video-memo-recovery",
            )

            discussionUpdates.isNotEmpty() -> VideoMemoStatusRow(
                text = if (discussionUpdates.size == 1) {
                    "${discussionUpdates.first().peerName}的讨论有更新"
                } else {
                    "${discussionUpdates.size} 个空间有更新"
                },
                color = DSColors.fairway,
                icon = { Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(14.dp)) },
                onClick = {
                    val only = discussionUpdates.singleOrNull()
                    if (only == null) showDiscussions = true
                    else onOpenDiscussion(only.connectionId, only.itemId)
                },
                testTag = "video-memo-discussion-update",
            )
        }
    }

    if (showDiscussions) {
        ModalBottomSheet(
            onDismissRequest = { showDiscussions = false },
            containerColor = DSColors.warmWhite,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "相关讨论",
                    color = DSColors.inkGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showDiscussions = false }) { Text("关闭") }
            }
            discussionUpdates.forEachIndexed { index, update ->
                if (index > 0) HorizontalDivider(color = DSColors.listCardBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDiscussions = false
                            onOpenDiscussion(update.connectionId, update.itemId)
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.ChatBubble, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                    Text(
                        "${update.peerName}的讨论",
                        color = DSColors.inkGreen,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.size(28.dp))
        }
    }
}

@Composable
private fun VideoMemoThumbnail(memo: V2Memo, playable: Boolean) {
    Box(
        modifier = Modifier
            .width(92.dp)
            .heightIn(min = 100.dp, max = 100.dp)
            .clip(RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier.size(35.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp).offset(x = 1.dp),
            )
        }
        memo.durationSec?.takeIf { playable && it > 0 }?.let { sec ->
            val value = sec.toInt()
            Text(
                String.format(Locale.ROOT, "%d:%02d", value / 60, value % 60),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun VideoMemoStatusRow(
    text: String,
    color: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ChevronRight, null, tint = color, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SourceChip(text: String, style: V2VideoSourceChipStyle) {
    val background = when (style) {
        V2VideoSourceChipStyle.LEAF -> DSColors.mistGreen
        else -> DSColors.listMutedBg
    }
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = if (style == V2VideoSourceChipStyle.GOLD) DSColors.goldBright else DSColors.fairway,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

internal fun videoMemoCardTitle(memo: V2Memo): String =
    memo.displayTitle?.trim()?.takeIf(String::isNotEmpty) ?: "视频笔记"

internal fun videoMemoCardNote(memo: V2Memo, title: String, sourceChipText: String): String? {
    val value = memo.text?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return value.takeUnless { it == title || it == sourceChipText || it == "—" || it in memo.tags }
}

internal fun videoMemoNeedsRecovery(memo: V2Memo, guestPreview: Boolean, playable: Boolean): Boolean =
    !guestPreview && !playable && memo.source == V2MemoSource.SELF_RECORD &&
        !memo.isOnlineVideo && !memo.isPlazaVideo

fun videoSourceChipFor(memo: V2Memo): Pair<String, V2VideoSourceChipStyle> {
    if (memo.isPlazaVideo) return "球场视频" to V2VideoSourceChipStyle.LEAF
    if (memo.isOnlineVideo) return "在线" to V2VideoSourceChipStyle.GOLD
    return when (memo.trainingMethod) {
        "拍摄" -> "拍摄" to V2VideoSourceChipStyle.MUTED
        "本地上传" -> "本地" to V2VideoSourceChipStyle.MUTED
        else -> (memo.trainingMethod?.takeIf { it.isNotBlank() } ?: "本地") to V2VideoSourceChipStyle.MUTED
    }
}

fun matchesVideoFilter(memo: V2Memo, filterId: String): Boolean = when (filterId) {
    "all" -> true
    "shoot" -> memo.trainingMethod == "拍摄"
    "upload" -> memo.trainingMethod == "本地上传"
    "online" -> memo.isOnlineVideo && !memo.isPlazaVideo
    "plaza" -> memo.isPlazaVideo
    else -> true
}
