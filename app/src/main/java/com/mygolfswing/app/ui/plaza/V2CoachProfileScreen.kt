package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaCoachProfile
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import java.util.UUID

@Composable
fun V2CoachProfileScreen(
    coachId: String,
    onBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenVideo: (UUID) -> Unit,
    onRequestLogin: () -> Unit = {},
) {
    val userProfile by V2ProfileStore.profile.collectAsState()
    var profile by remember(coachId) { mutableStateOf<V2PlazaCoachProfile?>(null) }
    var videos by remember(coachId) { mutableStateOf<List<V2PlazaVideo>>(emptyList()) }
    var loading by remember(coachId) { mutableStateOf(true) }
    var error by remember(coachId) { mutableStateOf<String?>(null) }
    var reloadNonce by remember(coachId) { mutableStateOf(0) }

    LaunchedEffect(coachId, reloadNonce) {
        loading = true
        error = null
        try {
            val local = (listOfNotNull(V2PlazaStore.heroVideo.value) + V2PlazaStore.videos.value)
                .filter { it.sourceCoachId == coachId }
            videos = local
            ServiceLocator.plazaRepo.fetchCoachProfile(coachId)
                .onSuccess { profile = it }
                .onFailure { error = coachProfileError("教练资料加载失败，请稍后重试", it.message) }
            ServiceLocator.plazaRepo.fetchCoachVideos(coachId)
                .onSuccess {
                    videos = it
                    V2PlazaStore.mergeTransientVideos(it)
                }
                .onFailure {
                    if (videos.isEmpty()) {
                        error = coachProfileError("教练视频加载失败，请稍后重试", it.message)
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (videos.isEmpty()) {
                error = coachProfileError("教练主页加载失败，请稍后重试", e.message)
            }
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBar(
            title = "教练主页",
            onBack = onBack,
            trailing = {
                CoachHomeButton(onClick = onOpenHome)
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CoachHeader(
                    profile = profile,
                    fallbackName = videos.firstOrNull()?.coach?.displayName ?: "教练",
                    videoCount = videos.size,
                    viewCount = videos.sumOf { it.viewCount },
                    saveCount = videos.sumOf { it.saveCount },
                )
            }
            if (loading && videos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { LoadingState() }
            } else if (videos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val hasError = !error.isNullOrBlank()
                    EmptyState(
                        title = if (hasError) "加载失败" else "暂无视频作品",
                        message = error ?: "这位教练暂时还没有公开视频",
                        isError = hasError,
                        onRetry = if (hasError) ({ reloadNonce += 1 }) else null,
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(title = "教学视频", subtitle = "${videos.size} 条")
                }
                items(videos, key = { it.id }) { video ->
                    V2PlazaGridCard(
                        video = video,
                        onTap = { onOpenVideo(video.id) },
                        onSaveTap = {
                            if (userProfile.isLoggedIn) V2PlazaStore.toggleSave(video.id) else onRequestLogin()
                        },
                    )
                }
            }
            if (!error.isNullOrBlank() && videos.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(error.orEmpty(), style = DSTypography.caption, color = DSColors.shadowGray)
                }
            }
        }
    }
}

@Composable
private fun CoachHomeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DSColors.cardWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "返回首页",
            tint = DSColors.inkGreen,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun CoachHeader(
    profile: V2PlazaCoachProfile?,
    fallbackName: String,
    videoCount: Int,
    viewCount: Int,
    saveCount: Int,
) {
    val name = profile?.name?.takeIf { it.isNotBlank() } ?: fallbackName
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))),
        ) {
            profile?.bannerUrl?.takeIf { it.isNotBlank() }?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)))),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CoachAvatar(profile = profile, fallbackName = name)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, style = DSTypography.titleLarge, color = DSColors.inkGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (profile?.tier == "official_partner" || profile?.badges?.contains("official") == true) {
                            Icon(Icons.Filled.Verified, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        listOfNotNull(profile?.certification, profile?.region?.let { "IP: $it" })
                            .joinToString(" · ")
                            .ifBlank { "高尔夫教学创作者" },
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!profile?.bio.isNullOrBlank()) {
                Text(profile?.bio.orEmpty(), style = DSTypography.body, color = DSColors.inkGreen.copy(alpha = 0.76f), maxLines = 4)
            }

            val badges = profile?.badgeLabels.orEmpty()
            if (badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    badges.take(3).forEach { BadgePill(it) }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CoachStat(value = videoCount.toString(), label = "视频")
                CoachStat(value = v2PlazaCompactCount(viewCount), label = "播放")
                CoachStat(value = v2PlazaCompactCount(saveCount), label = "收藏")
            }

            if (!profile?.externalLinks.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("平台账号", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                    profile?.externalLinks.orEmpty().take(4).forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DSColors.sandBeige.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Link, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                            Text(link.label, style = DSTypography.caption, color = DSColors.inkGreen, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(link.displayText, style = DSTypography.caption, color = DSColors.shadowGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachAvatar(profile: V2PlazaCoachProfile?, fallbackName: String) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(V2PlazaCoach.AvatarTint.GREEN.gradient))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } ?: Text(fallbackName.firstOrNull()?.toString() ?: "C", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BadgePill(text: String) {
    Text(
        text = text,
        color = DSColors.fairway,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.mistGreen)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun CoachStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = DSColors.shadowGray, fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.PlayArrow, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen, modifier = Modifier.padding(start = 6.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(subtitle, style = DSTypography.caption, color = DSColors.shadowGray)
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        Text(
            "正在加载教练主页…",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    isError: Boolean,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = if (isError) DSColors.flagRed.copy(alpha = 0.75f) else DSColors.shadowGray.copy(alpha = 0.75f),
            modifier = Modifier.size(30.dp),
        )
        Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            message,
            style = DSTypography.caption,
            color = DSColors.shadowGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (onRetry != null) {
            Text(
                "重试",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

private fun coachProfileError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}
