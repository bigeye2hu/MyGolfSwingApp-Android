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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HistoryTopBar(
            hasItems = profile.isLoggedIn && items.isNotEmpty(),
            onBack = onBack,
            onClear = { showClearConfirm = true },
        )
        if (!profile.isLoggedIn) {
            LoginRequiredHistory(onLogin = onRequestLogin)
        } else if (items.isEmpty()) {
            EmptyHistory()
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
                        onRemove = { V2PlazaStore.removeWatchHistory(item.saveKey) },
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空观看历史") },
            text = { Text("清空后无法恢复，确定要清空吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        V2PlazaStore.clearWatchHistory()
                        showClearConfirm = false
                    },
                ) { Text("清空", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消", color = DSColors.shadowGray) }
            },
        )
    }
}

@Composable
private fun LoginRequiredHistory(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color(0xFFC084FC), modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text("登录后查看观看历史", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "观看进度和历史列表会跟随账号保存，换设备也能继续找到看过的视频",
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "去登录",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFC084FC))
                .clickable(onClick = onLogin)
                .padding(horizontal = 28.dp, vertical = 10.dp),
        )
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
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color(0xFFC084FC), modifier = Modifier.size(22.dp))
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.coachName, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1)
                Text("·", color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(formatWatchDate(item.watchedAtMs), color = Color(0xFF6B7280), fontSize = 11.sp)
            }
            WatchProgressText(item.progress)
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 108.dp)
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
            .background(Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF111827)))),
    ) {
        item.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Icon(
            Icons.Filled.PlayArrow,
            null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp),
        )
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
private fun WatchProgressText(progress: Double) {
    when {
        progress >= 0.95 -> Text("已看完", color = Color(0xFF10B981), fontSize = 10.sp)
        progress > 0.0 -> Text("已看 ${(progress * 100).toInt()}%", color = Color(0xFFC084FC), fontSize = 10.sp)
        else -> Text("刚开始观看", color = Color(0xFF6B7280), fontSize = 10.sp)
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.History, null, tint = Color(0xFF374151), modifier = Modifier.size(42.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text("还没有观看记录", color = Color(0xFF9CA3AF), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Text("在视频广场观看教学视频后，会自动记录在这里", color = Color(0xFF6B7280), fontSize = 13.sp)
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
