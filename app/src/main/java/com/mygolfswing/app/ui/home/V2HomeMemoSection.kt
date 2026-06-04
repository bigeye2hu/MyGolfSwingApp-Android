package com.mygolfswing.app.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.memo.V2MemoThumbnail

/**
 * 主页「最近 Memo」横滑段（对齐 iOS `V2HomeMemoSection`）。
 *
 * - 标题 + 「查看全部 ›」
 * - 横滑卡列表；按 video / text / mixed 三种形态展示
 */
@Composable
fun V2HomeMemoSection(
    memos: List<V2Memo>,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {},
    onMemoTap: (V2Memo) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        SectionTitleRow(
            title = "最近 Memo",
            count = memos.size,
            onSeeAll = onSeeAll,
            modifier = Modifier.padding(horizontal = DSSpacing.lg),
        )

        if (memos.isEmpty()) {
            EmptyHint(
                text = "暂无最近 Memo，去写一条试试 →",
                modifier = Modifier.padding(horizontal = DSSpacing.lg),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = DSSpacing.lg),
            ) {
                items(memos, key = { it.id }) { memo ->
                    MemoMiniCard(memo = memo, onTap = { onMemoTap(memo) })
                }
            }
        }
    }
}

@Composable
internal fun SectionTitleRow(
    title: String,
    count: Int,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Book, null, tint = DSColors.deepGreen, modifier = Modifier.size(14.dp))
            Text(title, style = DSTypography.titleMedium, color = DSColors.inkGreen)
            Text(
                text = " $count",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Box(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clickable { onSeeAll() },
        ) {
            Text("全部", style = DSTypography.cardTitle, color = DSColors.deepGreen)
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.deepGreen,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun MemoMiniCard(memo: V2Memo, onTap: () -> Unit) {
    when (memo.kind) {
        V2MemoKind.VIDEO -> VideoMemoMiniCard(memo = memo, chipText = "视频", onTap = onTap)
        V2MemoKind.MIXED -> VideoMemoMiniCard(memo = memo, chipText = "视频·笔记", onTap = onTap)
        V2MemoKind.TEXT -> TextMemoMiniCard(memo = memo, onTap = onTap)
    }
}

@Composable
private fun VideoMemoMiniCard(memo: V2Memo, chipText: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onTap() },
    ) {
        V2MemoThumbnail(
            memo = memo,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemoTypeChip(
                    text = chipText,
                    icon = if (memo.kind == V2MemoKind.MIXED) Icons.Filled.Notes else Icons.Filled.Videocam,
                    dark = true,
                )
                Spacer(modifier = Modifier.weight(1f))
                DurationBadge(memo.durationSec)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayTitle(memo),
                    style = DSTypography.cardTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = V2Format.timelineLabel(memo.createdAt),
                    style = DSTypography.caption,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = DSColors.inkGreen, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun TextMemoMiniCard(memo: V2Memo, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.sandBeige)
            .clickable { onTap() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemoTypeChip(text = "文字", icon = Icons.Filled.TextFields, dark = false)
            Spacer(modifier = Modifier.weight(1f))
            memo.accent?.let { AccentChip(accent = it) }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = memo.text?.takeIf { it.isNotBlank() } ?: displayTitle(memo),
            style = DSTypography.cardTitle,
            color = DSColors.inkGreen,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = V2Format.timelineLabel(memo.createdAt),
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${memo.wordCount}字",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
    }
}

@Composable
private fun MemoTypeChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dark: Boolean,
) {
    val bg = if (dark) Color.Black.copy(alpha = 0.45f) else DSColors.cardWhite.copy(alpha = 0.72f)
    val fg = if (dark) Color.White else DSColors.fairway
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

@Composable
private fun AccentChip(accent: V2MemoAccent) {
    Text(
        text = accent.displayName,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = DSColors.goldBright,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DSColors.goldBright.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun DurationBadge(durationSec: Double?) {
    durationSec?.takeIf { it > 0 } ?: return
    Text(
        text = formatDuration(durationSec),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun displayTitle(memo: V2Memo): String {
    memo.displayTitle?.takeIf { it.isNotBlank() }?.let { return it }
    memo.title?.takeIf { it.isNotBlank() }?.let { return it }
    memo.text?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull()?.let { return it.take(20) }
    return if (memo.kind == V2MemoKind.TEXT) "未命名笔记" else "未命名视频"
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
internal fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.listMutedBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
    }
}
