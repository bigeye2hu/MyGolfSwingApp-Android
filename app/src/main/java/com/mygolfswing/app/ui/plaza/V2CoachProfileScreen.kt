@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.mygolfswing.app.ui.plaza

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaCoachLink
import com.mygolfswing.app.data.model.V2PlazaCoachProfile
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.plazaThrowableError
import com.mygolfswing.app.ui.common.copyPlainTextToClipboard
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.UUID

private val CoachPurple = Color(0xFF9333EA)
private val CoachPurpleLight = Color(0xFFC084FC)
private val CoachBlue = Color(0xFF2563EB)
private val CoachSurface = Color(0xFF1A1A2E)
private val CoachCard = Color(0x801A1A1A)
private val CoachText = Color(0xFFD1D5DB)
private val CoachMuted = Color(0xFF9CA3AF)
private val CoachDim = Color(0xFF6B7280)
private val CoachBorder = Color(0xFF374151)

@Composable
fun V2CoachProfileScreen(
    coachId: String,
    onBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenVideo: (UUID) -> Unit,
) {
    var profile by remember(coachId) { mutableStateOf<V2PlazaCoachProfile?>(null) }
    var videos by remember(coachId) { mutableStateOf<List<V2PlazaVideo>>(emptyList()) }
    var loading by remember(coachId) { mutableStateOf(true) }
    var error by remember(coachId) { mutableStateOf<String?>(null) }
    var reloadNonce by remember(coachId) { mutableIntStateOf(0) }

    LaunchedEffect(coachId, reloadNonce) {
        loading = true
        error = null
        try {
            val local = (listOfNotNull(V2PlazaStore.heroVideo.value) + V2PlazaStore.videos.value)
                .filter { it.sourceCoachId == coachId }
            videos = local
            ServiceLocator.plazaRepo.fetchCoachProfile(coachId, forceRefresh = true)
                .onSuccess { profile = it }
                .onFailure { error = plazaThrowableError("教练资料加载失败，请稍后重试", it) }
            ServiceLocator.plazaRepo.fetchCoachVideos(coachId)
                .onSuccess {
                    videos = it
                    V2PlazaStore.mergeTransientVideos(it)
                }
                .onFailure {
                    if (videos.isEmpty()) error = plazaThrowableError("教练视频加载失败，请稍后重试", it)
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (videos.isEmpty()) error = plazaThrowableError("教练主页加载失败，请稍后重试", e)
        } finally {
            loading = false
        }
    }

    V2CoachProfileContent(
        profile = profile,
        videos = videos,
        loading = loading,
        error = error,
        onRetry = { reloadNonce += 1 },
        onBack = onBack,
        onOpenHome = onOpenHome,
        onOpenVideo = onOpenVideo,
    )
}

@Composable
internal fun V2CoachProfileContent(
    profile: V2PlazaCoachProfile?,
    videos: List<V2PlazaVideo>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenVideo: (UUID) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val fallbackName = videos.firstOrNull()?.coach?.displayName ?: "教练"
    val titleVisible = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 110

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { CoachHero(profile = profile, fallbackName = fallbackName) }
            item(span = { GridItemSpan(maxLineSpan) }) { CoachDetails(profile = profile) }
            item(span = { GridItemSpan(maxLineSpan) }) { CoachStats(videos = videos) }
            item(span = { GridItemSpan(maxLineSpan) }) { WorksHeader(count = videos.size) }
            when {
                loading && videos.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { CoachLoadingState() }
                videos.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    CoachEmptyState(error = error, onRetry = if (error.isNullOrBlank()) null else onRetry)
                }
                else -> items(videos, key = { it.id }) { video ->
                    val index = videos.indexOf(video)
                    CoachVideoCard(
                        video = video,
                        profile = profile,
                        onClick = { onOpenVideo(video.id) },
                        modifier = Modifier.padding(
                            start = if (index % 2 == 0) 16.dp else 0.dp,
                            end = if (index % 2 == 1) 16.dp else 0.dp,
                        ),
                    )
                }
            }
            if (!error.isNullOrBlank() && videos.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(error, color = CoachDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        CoachFloatingTopBar(
            title = profile?.name ?: fallbackName,
            titleVisible = titleVisible,
            onBack = onBack,
            onOpenHome = onOpenHome,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun CoachFloatingTopBar(
    title: String,
    titleVisible: Boolean,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoachGlassButton(onClick = onBack, description = "返回") {
            Icon(Icons.Filled.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (titleVisible) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        CoachGlassButton(onClick = onOpenHome, description = "返回首页") {
            Icon(Icons.Filled.Home, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CoachGlassButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(Color(0xB3262730))
            .semantics { contentDescription = description }
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CoachHero(profile: V2PlazaCoachProfile?, fallbackName: String) {
    val name = profile?.name?.takeIf(String::isNotBlank) ?: fallbackName
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF1A0533), Color(0xFF0D1B2A), Color(0xFF1B2838)))),
    ) {
        Box(Modifier.size(300.dp).offset(x = (-80).dp, y = (-60).dp).clip(CircleShape).background(CoachPurple.copy(alpha = 0.08f)))
        Box(Modifier.size(250.dp).offset(x = 210.dp, y = 40.dp).clip(CircleShape).background(CoachBlue.copy(alpha = 0.06f)))
        profile?.bannerUrl?.takeIf(String::isNotBlank)?.let { url ->
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.6f), Color.Black),
                ),
            ),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CoachHeroAvatar(profile = profile, fallbackName = name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (!profile?.badges.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile?.badges.orEmpty().take(3).forEach { badge -> CoachBadge(badge) }
                    }
                }
                profile?.certification?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = CoachMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CoachHeroAvatar(profile: V2PlazaCoachProfile?, fallbackName: String) {
    Box(
        modifier = Modifier.size(77.dp).clip(CircleShape).background(Brush.linearGradient(listOf(CoachPurple, CoachBlue))).padding(2.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(V2PlazaCoach.AvatarTint.GREEN.gradient)),
            contentAlignment = Alignment.Center,
        ) {
            profile?.avatarUrl?.takeIf(String::isNotBlank)?.let {
                AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: Text(
                fallbackName.firstOrNull()?.toString() ?: "C",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CoachBadge(raw: String) {
    val (text, color) = when (raw) {
        "official" -> "官方合作" to Color(0xFF3B82F6)
        "certified" -> "认证教练" to Color(0xFF10B981)
        "vip" -> "VIP" to Color(0xFFF59E0B)
        else -> raw to CoachDim
    }
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun CoachDetails(profile: V2PlazaCoachProfile?) {
    if (profile == null) return
    var expanded by remember(profile.id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        profile.bio?.takeIf(String::isNotBlank)?.let { bio ->
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    bio,
                    color = CoachText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (bio.length > 100) {
                    Text(
                        if (expanded) "收起" else "展开",
                        color = CoachPurpleLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.combinedClickable(onClick = { expanded = !expanded }),
                    )
                }
            }
        }
        if (profile.externalLinks.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.externalLinks.forEach { CoachPlatformLink(it) }
            }
        }
        profile.region?.takeIf(String::isNotBlank)?.let {
            Text("●  IP: $it", color = CoachDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun CoachPlatformLink(link: V2PlazaCoachLink) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val color = platformColor(link.platform)
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { menuOpen = true }, onLongClick = { menuOpen = true }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Link, null, tint = color, modifier = Modifier.size(18.dp))
            Text(platformName(link.platform), color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(link.displayText, color = CoachText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = CoachSurface) {
            DropdownMenuItem(
                text = { Text("复制账号", color = Color.White) },
                onClick = {
                    copyCoachText(context, link.displayName.ifBlank { link.url }, "账号已复制")
                    menuOpen = false
                },
            )
            if (link.url.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("复制主页链接", color = Color.White) },
                    onClick = {
                        copyCoachText(context, link.url, "主页链接已复制")
                        menuOpen = false
                    },
                )
            }
        }
    }
}

private fun copyCoachText(context: Context, value: String, message: String) {
    context.copyPlainTextToClipboard("教练平台", value, successMessage = message)
}

private fun platformName(platform: String): String = when (platform.lowercase()) {
    "douyin" -> "抖音"
    "bilibili" -> "B站"
    "youtube" -> "YouTube"
    "wechat", "weixin" -> "微信"
    "xiaohongshu" -> "小红书"
    else -> platform
}

private fun platformColor(platform: String): Color = when (platform.lowercase()) {
    "douyin" -> Color(0xFF25F4EE)
    "bilibili" -> Color(0xFFFB7299)
    "youtube" -> Color(0xFFFF4444)
    "wechat", "weixin" -> Color(0xFF2DC100)
    "xiaohongshu" -> Color(0xFFFF4F6D)
    else -> CoachMuted
}

@Composable
private fun CoachStats(videos: List<V2PlazaVideo>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CoachSurface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoachStat(videos.size.toString(), "视频", Modifier.weight(1f))
        CoachStatDivider()
        CoachStat(coachVisibleTotal(videos.sumOf { it.likeCount }, 5), "获赞", Modifier.weight(1f))
        CoachStatDivider()
        CoachStat(coachVisibleTotal(videos.sumOf { it.saveCount }, 5), "收藏", Modifier.weight(1f))
        CoachStatDivider()
        CoachStat(coachVisibleTotal(videos.sumOf { it.viewCount }, 50), "播放", Modifier.weight(1f))
    }
}

@Composable
private fun CoachStat(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = CoachMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CoachStatDivider() {
    Box(Modifier.size(width = 1.dp, height = 28.dp).background(Color.White.copy(alpha = 0.08f)))
}

internal fun coachVisibleTotal(value: Int, threshold: Int): String =
    if (value >= threshold) coachCompactCount(value) else "积累中"

internal fun coachCompactCount(value: Int): String = when {
    value >= 10_000 -> String.format(Locale.US, "%.1fw", value / 10_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
    else -> value.toString()
}

@Composable
private fun WorksHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("作品", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("$count", color = CoachPurpleLight, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CoachVideoCard(
    video: V2PlazaVideo,
    profile: V2PlazaCoachProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CoachCard)
            .border(1.dp, CoachBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(10.dp))) {
            V2PlazaThumbnailBackground(video = video, modifier = Modifier.fillMaxSize())
            video.categoryDisplayName?.takeIf { it != "待分类子分类" }?.let {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(50)).background(CoachPurple.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Filled.Folder, null, tint = Color.White, modifier = Modifier.size(7.dp))
                    Text(it, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            if (profile?.tier == "official_partner") {
                Text(
                    "✓  官方合作",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0xD93B82F6)).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(9.dp))
                }
                Text(
                    coachDuration(video.durationSec),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(video.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoachMiniAvatar(profile = profile, video = video)
                Text(video.coach.displayName, color = CoachMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(
                    if (video.likeCount >= 5) Icons.Filled.Favorite else Icons.Filled.AutoAwesome,
                    null,
                    tint = CoachMuted,
                    modifier = Modifier.size(9.dp),
                )
                Text(
                    if (video.likeCount >= 5) coachCompactCount(video.likeCount) else (video.categoryDisplayName ?: if (video.isNew) "上新" else "精选"),
                    color = CoachMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CoachMiniAvatar(profile: V2PlazaCoachProfile?, video: V2PlazaVideo) {
    Box(
        modifier = Modifier.size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF6B21A8), Color(0xFF1D4ED8)))),
        contentAlignment = Alignment.Center,
    ) {
        val avatar = profile?.avatarUrl?.takeIf(String::isNotBlank) ?: video.coach.avatarUrl?.takeIf(String::isNotBlank)
        if (avatar != null) AsyncImage(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(video.coach.initialChar.take(1), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun coachDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun CoachLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = CoachPurpleLight, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        Text("加载中...", color = CoachMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun CoachEmptyState(error: String?, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Icon(
            if (error.isNullOrBlank()) Icons.Filled.VideoLibrary else Icons.Filled.ErrorOutline,
            null,
            tint = if (error.isNullOrBlank()) Color(0xFF4B5563) else Color(0xFFEF4444).copy(alpha = 0.75f),
            modifier = Modifier.size(28.dp),
        )
        Text(if (error.isNullOrBlank()) "暂无视频作品" else "加载失败", color = CoachDim, fontSize = 14.sp)
        if (!error.isNullOrBlank()) {
            Text(error, color = CoachDim, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 24.dp))
        }
        if (onRetry != null) {
            Text(
                "重试",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(CoachPurple).combinedClickable(onClick = onRetry).padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}
