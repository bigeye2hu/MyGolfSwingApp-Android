package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2PlazaWatchHistoryItem
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun V2PlazaWatchHistoryScreen(
    onBack: () -> Unit,
    onOpenVideo: (UUID) -> Unit,
    onRequestLogin: () -> Unit = {},
) {
    val items by V2PlazaStore.watchHistory.collectAsState()
    val profile by V2ProfileStore.profile.collectAsState()

    V2PlazaWatchHistoryContent(
        items = items,
        isLoggedIn = profile.isLoggedIn,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onRemove = V2PlazaStore::removeWatchHistory,
        onClear = V2PlazaStore::clearWatchHistory,
        onRequestLogin = onRequestLogin,
    )
}

@Composable
internal fun V2PlazaWatchHistoryContent(
    items: List<V2PlazaWatchHistoryItem>,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onOpenVideo: (UUID) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onRequestLogin: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HistoryTopBar(
            hasItems = isLoggedIn && items.isNotEmpty(),
            onBack = onBack,
            onClear = { showClearConfirm = true },
        )
        if (!isLoggedIn) {
            EmptyHistory(isLoggedIn = false, onLogin = onRequestLogin)
        } else if (items.isEmpty()) {
            EmptyHistory(isLoggedIn = true, onLogin = onRequestLogin)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .v2NavigationBarPadding(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(items, key = { it.saveKey }) { item ->
                    HistoryRow(
                        item = item,
                        onOpen = { onOpenVideo(item.videoId) },
                        onRemove = { onRemove(item.saveKey) },
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        WatchHistoryClearConfirmDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                onClear()
                showClearConfirm = false
            },
        )
    }
}

@Composable
internal fun WatchHistoryClearConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier.width(270.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F7).copy(alpha = 0.98f))
                    .testTag("watch-history-clear-dialog"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "清空观看历史",
                        color = Color(0xFF1C1C1E),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "清空后无法恢复，确定要清空吗？",
                        color = Color(0xFF3A3A3C),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD1D1D6)))
                Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize().clickable(onClick = onDismiss)
                            .testTag("watch-history-clear-cancel"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("取消", color = Color(0xFF007AFF), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.width(1.dp).fillMaxSize().background(Color(0xFFD1D1D6)))
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize().clickable(onClick = onConfirm)
                            .testTag("watch-history-clear-confirm"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("清空", color = Color(0xFFFF3B30), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(
    hasItems: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "观看历史",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "关闭观看历史",
                tint = Color(0xFFC084FC),
                modifier = Modifier.size(16.dp),
            )
        }
        if (hasItems) {
            Text(
                "清空",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun HistoryRow(
    item: V2PlazaWatchHistoryItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HistoryThumb(item)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.coachName.isNotBlank()) {
                    Text(item.coachName, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1)
                }
                Text("·", color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(formatWatchDate(item.watchedAtMs), color = Color(0xFF6B7280), fontSize = 11.sp)
            }
            WatchProgressText(item.progress)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "删除这条观看记录", tint = Color(0xFF6B7280), modifier = Modifier.size(12.dp))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 92.dp)
            .height(1.dp)
            .background(Color(0xFF1F2937)),
    )
}

@Composable
private fun HistoryThumb(item: V2PlazaWatchHistoryItem) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF374151)),
    ) {
        val thumbnailUrl = item.thumbnailUrl?.takeIf { it.isNotBlank() }
        if (thumbnailUrl == null) {
            HistoryThumbnailFallback()
        } else {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { HistoryThumbnailFallback() },
                error = { HistoryThumbnailFallback() },
                success = { SubcomposeAsyncImageContent() },
            )
        }
        if (item.progress > 0.0 && item.progress < 0.95) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progress.toFloat().coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(Color(0xFFC084FC)),
            )
        }
    }
}

@Composable
private fun HistoryThumbnailFallback() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.OndemandVideo,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun WatchProgressText(progress: Double) {
    when {
        progress >= 0.95 -> Text("已看完", color = Color(0xFF10B981), fontSize = 10.sp)
        progress > 0.0 -> Text(
            "已看 ${(progress * 100).toInt()}%",
            color = Color(0xFFC084FC).copy(alpha = 0.8f),
            fontSize = 10.sp,
        )
        else -> Unit
    }
}

@Composable
private fun EmptyHistory(
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Filled.History, null, tint = Color(0xFF374151), modifier = Modifier.size(40.dp))
        Text(
            if (isLoggedIn) "还没有观看记录" else "登录后保存观看历史",
            color = Color(0xFF9CA3AF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            if (isLoggedIn) {
                "在视频广场观看教学视频后，会自动记录在这里"
            } else {
                "游客可以继续播放公开视频，但不会写入观看历史；登录后才能保留复习进度。"
            },
            color = Color(0xFF6B7280),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (!isLoggedIn) {
            Text(
                "登录/注册后保存历史",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF9333EA), Color(0xFF2563EB)),
                        ),
                    )
                    .clickable(onClick = onLogin)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

private fun formatWatchDate(ms: Long): String {
    val diff = (System.currentTimeMillis() - ms).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute}分钟前"
        diff < day -> "${diff / hour}小时前"
        diff < 30 * day -> "${diff / day}天前"
        else -> SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date(ms))
    }
}
