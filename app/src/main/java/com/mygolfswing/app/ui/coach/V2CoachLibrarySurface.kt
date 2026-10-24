package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlin.math.roundToInt

@Composable
internal fun V2CoachLibraryHero(
    assetCount: Int,
    categoryCount: Int,
    reuseCount: Int,
    addEnabled: Boolean,
    dimmed: Boolean,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    V2PageArtworkBackground(
        artwork = V2PageArtwork.GOLF_BAG,
        tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
        modifier = modifier.fillMaxWidth().height(186.dp).testTag("coach-library-hero"),
    ) {
        Column(Modifier.fillMaxWidth()) {
            V2CoachLibraryFixedHeader(
                addEnabled = addEnabled,
                onSearch = onSearch,
                onAdd = onAdd,
            )
            V2CoachLibraryOverview(
                assetCount = assetCount,
                categoryCount = categoryCount,
                reuseCount = reuseCount,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    .alpha(if (dimmed) 0.76f else 1f),
            )
        }
    }
}

@Composable
internal fun V2CoachLibraryFixedHeader(
    addEnabled: Boolean,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp)
            .testTag("coach-library-fixed-header"),
    ) {
        Text(
            "私有素材库",
            color = DSColors.inkGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryHeaderAction(
                icon = Icons.Filled.Search,
                description = "搜索素材",
                tag = "coach-library-header-search",
                onClick = onSearch,
            )
            LibraryHeaderAction(
                icon = Icons.Filled.Add,
                description = "添加素材",
                tag = "coach-library-header-add",
                enabled = addEnabled,
                emphasized = true,
                onClick = onAdd,
            )
        }
    }
}

@Composable
private fun LibraryHeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(34.dp)
            .then(
                if (emphasized) {
                    Modifier.shadow(6.dp, CircleShape, ambientColor = DSColors.deepGreen.copy(alpha = 0.18f))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                },
            )
            .clip(CircleShape)
            .background(if (emphasized) DSColors.fairway else Color.White.copy(alpha = 0.78f))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = if (emphasized) Color.White else DSColors.deepGreen,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
internal fun V2CoachLibraryOverview(
    assetCount: Int,
    categoryCount: Int,
    reuseCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(18.dp))
            .testTag("coach-library-overview"),
    ) {
        Image(
            painter = painterResource(R.drawable.page_back_2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DSColors.deepGreen.copy(alpha = 0.88f),
                        DSColors.deepGreen.copy(alpha = 0.72f),
                    ),
                ),
            ),
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("我的教学素材库", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("沉淀经验 · 更好地帮助每一位学员", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth()) {
                LibraryMetric(assetCount.toString(), "全部素材", Modifier.weight(1f))
                LibraryMetric(categoryCount.toString(), "素材分类", Modifier.weight(1f))
                LibraryMetric(reuseCount.toString(), "被复用次数", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun V2CoachLibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(60)) },
        singleLine = true,
        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        modifier = modifier.fillMaxWidth().height(44.dp).focusRequester(focusRequester)
            .clip(RoundedCornerShape(15.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(15.dp))
            .testTag("coach-library-search"),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxSize().padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(17.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("搜索素材名称、标签或内容", color = DSColors.shadowGray, fontSize = 13.sp)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "清空搜索", tint = DSColors.shadowGray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        },
    )
}

@Composable
internal fun V2CoachLibraryCategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("coach-library-categories"),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        categories.forEach { category ->
            val selected = category == selectedCategory
            Box(
                Modifier.height(38.dp).clip(CircleShape)
                    .background(if (selected) DSColors.deepGreen else DSColors.cardWhite)
                    .border(1.dp, if (selected) Color.Transparent else DSColors.listCardBorder, CircleShape)
                    .clickable(role = Role.Button) { onSelected(category) }
                    .padding(horizontal = 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    category,
                    color = if (selected) Color.White else DSColors.shadowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun V2CoachLibraryEmptyState(
    searchActive: Boolean,
    actionsEnabled: Boolean,
    onAlbum: () -> Unit,
    onCamera: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
            .padding(22.dp).testTag("coach-library-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                Icon(
                    if (searchActive) Icons.Filled.Search else Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                if (searchActive) "没有找到匹配素材" else "先放进一条常用讲解",
                color = DSColors.inkGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (searchActive) "试试更换标题或标签关键词" else "下次回复学员时，两步就能直接发出",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (searchActive) {
            Text(
                "清空搜索",
                color = DSColors.fairway,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onReset)
                    .padding(horizontal = 18.dp, vertical = 10.dp).testTag("coach-library-empty-reset"),
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LibraryEmptyAction(
                    title = "从相册添加",
                    icon = Icons.Filled.PhotoLibrary,
                    emphasized = true,
                    enabled = actionsEnabled,
                    onClick = onAlbum,
                    modifier = Modifier.weight(1f),
                )
                LibraryEmptyAction(
                    title = "现在拍一条",
                    icon = Icons.Filled.CameraAlt,
                    emphasized = false,
                    enabled = actionsEnabled,
                    onClick = onCamera,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun V2CoachLibraryNoResultsState(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("coach-library-filter-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(22.dp))
        Text("这个分类里还没有素材", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            "查看全部",
            color = DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onReset)
                .padding(horizontal = 14.dp, vertical = 7.dp).testTag("coach-library-filter-reset"),
        )
    }
}

@Composable
private fun LibraryEmptyAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.height(42.dp).clip(RoundedCornerShape(14.dp))
            .background(
                (if (emphasized) DSColors.fairway else DSColors.mistGreen.copy(alpha = 0.74f))
                    .copy(alpha = if (enabled) 1f else 0.45f),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (emphasized) Color.White else DSColors.fairway, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            color = if (emphasized) Color.White else DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun V2CoachLibraryGridCard(
    asset: V2CoachLibraryAsset,
    hasDraft: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCoverNeedsRefresh: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(asset.assetId, asset.videoUrl) {
        if (coachLibraryCoverNeedsRefresh(asset)) onCoverNeedsRefresh()
    }
    Column(
        modifier.fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(16.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onOpen).testTag("coach-library-card-${asset.assetId}"),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            V2CoachMediaCover(
                asset = asset,
                modifier = Modifier.fillMaxSize().testTag("coach-library-cover-${asset.assetId}"),
                contentScale = ContentScale.Fit,
            )
            asset.durationSeconds?.takeIf { it > 0.0 }?.let { duration ->
                Text(
                    formatLibraryDuration(duration),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                asset.title.takeIf(String::isNotBlank) ?: "教学素材",
                color = DSColors.inkGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(18.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    asset.tags.firstOrNull()?.takeIf(String::isNotBlank) ?: "未分类",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onShare)
                        .testTag("coach-library-share-${asset.assetId}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (hasDraft) Icons.Filled.Schedule else Icons.AutoMirrored.Filled.Send,
                        if (hasDraft) "继续发送" else "发送给学员",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

internal fun coachLibraryCoverNeedsRefresh(asset: V2CoachLibraryAsset): Boolean =
    asset.videoUrl.isNullOrBlank()

@Composable
internal fun V2CoachLibrarySyncWarning(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFEED7).copy(alpha = 0.72f))
            .border(1.dp, DSColors.goldBright.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(12.dp).testTag("coach-library-sync-warning"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.Refresh, null, tint = DSColors.goldBright, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("暂时显示上次的素材", color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "已有内容仍可预览和发送，重新同步不会重复上传",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "重新加载",
            color = DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry).padding(vertical = 6.dp),
        )
        Box(
            Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "关闭同步提示", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
internal fun V2CoachLibraryUnavailableState(
    actionsEnabled: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
            .padding(24.dp).testTag("coach-library-unavailable"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CloudSync, null, tint = DSColors.fairway, modifier = Modifier.size(27.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("素材暂时没有同步下来", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "已经上传的内容不会丢失，重新连接后会回到这里。",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                "点击重新同步即可继续；如果刚上传过视频，也会自动找回。",
                color = DSColors.shadowGray.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryRecoveryAction(
                title = "重新同步",
                icon = Icons.Filled.Refresh,
                emphasized = true,
                enabled = true,
                onClick = onRetry,
            )
            LibraryRecoveryAction(
                title = "添加新素材",
                icon = Icons.Filled.Add,
                emphasized = false,
                enabled = actionsEnabled,
                onClick = onAdd,
            )
        }
    }
}

@Composable
private fun LibraryRecoveryAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.height(42.dp).clip(CircleShape)
            .background(if (emphasized) DSColors.fairway else DSColors.mistGreen.copy(alpha = 0.78f))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (emphasized) 18.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val color = if (emphasized) Color.White else DSColors.fairway
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

internal fun formatLibraryDuration(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

internal fun shouldRequestNextCoachLibraryPage(
    visibleIndex: Int,
    itemCount: Int,
    nextCursor: String?,
): Boolean = itemCount > 0 && visibleIndex == itemCount - 1 && !nextCursor.isNullOrBlank()

internal fun shouldRefreshCoachLibraryAfterReconnect(previous: Boolean?, current: Boolean): Boolean =
    previous == false && current
