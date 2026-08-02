package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2MemoListNavHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .v2StatusBarPadding()
            .padding(horizontal = DSSpacing.lg)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "返回",
                    tint = DSColors.fairway,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            trailing()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.fairway,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
fun V2MemoListHeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = DSColors.fairway.copy(alpha = if (!enabled) 0.5f else if (selected) 1f else 0.78f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
fun V2MemoStatsBar(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = DSColors.fairway,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg)
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.listMutedBg)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun V2MemoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索标题 / 内容 / 话题",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 6.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DSColors.shadowGray) },
        trailingIcon = {
            if (query.isNotBlank()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "清除搜索",
                    tint = DSColors.shadowGray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onQueryChange("") }
                        .padding(4.dp),
                )
            }
        },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = DSColors.shadowGray) },
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun V2MemoFilterChips(
    filters: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DSSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = { it.first }) { (id, label) ->
            val selected = id == selectedId
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else DSColors.fairway,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) DSColors.fairway else DSColors.cardWhite)
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else DSColors.listCardBorder,
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp)
                    .height(32.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun V2MemoListSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.listMutedBg)
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 6.dp, bottom = 5.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DSColors.fairway)
    }
}

@Composable
fun V2MemoNoMoreFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(DSColors.listCardBorder),
        )
        Text(
            text = "没有更多了",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(DSColors.listCardBorder),
        )
    }
}

fun V2Memo.matchesListQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isBlank()) return true
    return listOfNotNull(
        displayTitle,
        title,
        text,
        topic.displayName,
        accentDisplayLabel(),
        metaText,
        trainingMethod,
        platform,
    )
        .plus(tags)
        .plus(searchSourceLabels())
        .any { it.lowercase().contains(q) }
}

private fun V2Memo.searchSourceLabels(): List<String> = buildList {
    if (isPlazaVideo) addAll(listOf("球场视频", "广场", "plaza"))
    if (isOnlineVideo) addAll(listOf("在线", "在线视频", "online"))
    platform?.takeIf { it.isNotBlank() }?.let { add(it) }
    trainingMethod?.takeIf { it.isNotBlank() }?.let { add(it) }
    if (videoUrl?.startsWith("file://") == true) add("本地")
}
