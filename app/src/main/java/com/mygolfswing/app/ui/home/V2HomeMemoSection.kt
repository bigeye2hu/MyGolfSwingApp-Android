package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * 主页「最近 Memo」横滑段（对齐 iOS `V2HomeMemoSection`）。
 *
 * - 标题 + 「查看全部 ›」
 * - 横滑卡列表（每张 220×约 96）
 */
@Composable
fun V2HomeMemoSection(
    memos: List<V2Memo>,
    onSeeAll: () -> Unit = {},
    onMemoTap: (V2Memo) -> Unit = {},
    modifier: Modifier = Modifier,
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
        Text(title, style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Text(
            text = " $count",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
        Box(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clickable { onSeeAll() },
        ) {
            Text("查看全部", style = DSTypography.cardTitle, color = DSColors.deepGreen)
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
    Box(
        modifier = Modifier
            .width(220.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = DSColors.borderLine, shape = RoundedCornerShape(14.dp))
            .clickable { onTap() }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notes,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "  ${memo.topic.displayName}",
                    style = DSTypography.label.copy(fontSize = 11.sp),
                    color = DSColors.fairway,
                )
                Box(modifier = Modifier.weight(1f))
                memo.accent?.let { accent ->
                    AccentChip(accent = accent)
                }
            }
            Text(
                text = memo.displayTitle ?: memo.title ?: "无标题",
                style = DSTypography.bodyBold,
                color = DSColors.inkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            memo.metaText?.let { meta ->
                Text(
                    text = meta,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = DSColors.shadowGray,
                )
            }
        }
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
