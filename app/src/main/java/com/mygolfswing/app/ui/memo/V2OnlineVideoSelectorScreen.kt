package com.mygolfswing.app.ui.memo

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.launch

data class V2OnlineVideoSelection(
    val url: String,
    val platform: String,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val durationSec: Double? = null,
)

/** 在线视频 URL 选择（对齐 iOS `V2OnlineVideoSelectorView`） */
@Composable
fun V2OnlineVideoSelectorScreen(
    onBack: () -> Unit,
    onConfirm: (V2OnlineVideoSelection) -> Unit,
    initialUrl: String = "",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlInput by rememberSaveable { mutableStateOf(initialUrl) }
    var platform by remember { mutableStateOf("") }
    var videoTitle by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var durationSec by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(false) }
    var fetchFailed by remember { mutableStateOf(false) }
    var pasteMessage by remember { mutableStateOf<String?>(null) }
    var fetchToken by remember { mutableIntStateOf(0) }

    val hasRecognized = platform.isNotEmpty() && platform != "其他" &&
        (videoTitle.isNotBlank() || thumbnailUrl != null || durationSec != null)
    val hasInvalid = urlInput.isNotBlank() && !loading &&
        (platform == "其他" || fetchFailed)

    fun detectAndFetch(raw: String) {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(raw)
        if (cleaned != raw) {
            urlInput = cleaned
        }
        videoTitle = ""
        thumbnailUrl = null
        durationSec = null
        fetchFailed = false
        pasteMessage = null
        if (cleaned.isBlank()) {
            platform = ""
            return
        }
        platform = V2OnlineVideoMetadataFetcher.detectPlatform(cleaned)
        if (platform == "其他") {
            loading = false
            return
        }
        fetchToken += 1
        val token = fetchToken
        scope.launch {
            loading = true
            val meta = V2OnlineVideoMetadataFetcher.fetch(cleaned, platform)
            if (token != fetchToken) return@launch
            loading = false
            meta.title?.let { videoTitle = it }
            thumbnailUrl = meta.thumbnailUrl
            durationSec = meta.durationSec
            fetchFailed = meta.title.isNullOrBlank() && meta.thumbnailUrl == null && meta.durationSec == null
        }
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            urlInput = initialUrl
            detectAndFetch(initialUrl)
        }
    }

    fun confirm() {
        if (!hasRecognized) return
        val trimmed = urlInput.trim()
        onConfirm(
            V2OnlineVideoSelection(
                url = trimmed,
                platform = platform,
                title = videoTitle.ifBlank { trimmed },
                thumbnailUrl = thumbnailUrl,
                durationSec = durationSec,
            ),
        )
        onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .v2StatusBarPadding()
                .background(DSColors.warmWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.widthIn(min = 60.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onBack),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("返回", color = DSColors.fairway, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("添加在线视频", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.widthIn(min = 60.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "确认",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasRecognized) Color.White else DSColors.shadowGray,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (hasRecognized) Brush.linearGradient(listOf(Color(0xFF3D9A6A), DSColors.fairway))
                            else Brush.linearGradient(listOf(DSColors.sandBeige, DSColors.sandBeige)),
                        )
                        .clickable(enabled = hasRecognized, onClick = ::confirm)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .v2NavigationBarPadding()
                .padding(bottom = 24.dp),
        ) {
            if (!hasRecognized) {
                EmptyHero(modifier = Modifier.padding(top = 24.dp))
            }

            UrlInputCard(
                url = urlInput,
                onUrlChange = {
                    urlInput = it
                    detectAndFetch(it)
                },
                loading = loading,
                hasRecognized = hasRecognized,
                hasInvalid = hasInvalid,
                platform = platform,
                pasteMessage = pasteMessage,
                onPaste = {
                    val pasted = context.readClipboardText()
                    if (pasted == null) {
                        pasteMessage = "剪贴板没有可用链接，请手动粘贴"
                    } else {
                        urlInput = pasted
                        detectAndFetch(pasted)
                    }
                },
                onClear = {
                    urlInput = ""
                    platform = ""
                    videoTitle = ""
                    thumbnailUrl = null
                    durationSec = null
                    fetchFailed = false
                    pasteMessage = null
                },
            )

            when {
                hasRecognized -> {
                    VideoPreviewCard(
                        title = videoTitle,
                        platform = platform,
                        thumbnailUrl = thumbnailUrl,
                        durationSec = durationSec,
                    )
                    ConfirmButton(onClick = ::confirm)
                }
                hasInvalid -> InvalidLinkCard()
                else -> {
                    SupportedPlatformsCard()
                    HelperCard()
                }
            }

            DecorativeFooter(modifier = Modifier.padding(top = if (hasRecognized) 16.dp else 48.dp))
        }
    }
}

@Composable
private fun EmptyHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = DSColors.fairway.copy(alpha = 0.25f),
                    spotColor = DSColors.fairway.copy(alpha = 0.25f),
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF3D9A6A), DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Link, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("粘贴视频链接", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        Text(
            "支持 B站、YouTube、小红书、抖音等平台",
            fontSize = 13.sp,
            color = DSColors.shadowGray,
            modifier = Modifier.padding(horizontal = 32.dp).padding(top = 6.dp),
        )
    }
}

@Composable
private fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    loading: Boolean,
    hasRecognized: Boolean,
    hasInvalid: Boolean,
    platform: String,
    pasteMessage: String?,
    onPaste: () -> Unit,
    onClear: () -> Unit,
) {
    val borderColor = when {
        hasRecognized -> DSColors.fairway
        hasInvalid -> DSColors.flagRed
        else -> DSColors.borderLine
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = DSColors.fairway)
            hasRecognized -> Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            hasInvalid -> Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(18.dp))
            else -> Icon(Icons.Filled.Link, null, tint = DSColors.shadowGray, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = DSColors.inkGreen),
                decorationBox = { inner ->
                    if (url.isEmpty()) {
                        Text("粘贴或输入视频链接…", fontSize = 14.sp, color = DSColors.shadowGray)
                    }
                    inner()
                },
            )
            when {
                pasteMessage != null -> Text(pasteMessage, fontSize = 10.sp, color = DSColors.flagRed)
                loading -> Text("正在获取视频信息…", fontSize = 10.sp, color = DSColors.shadowGray)
                hasRecognized -> Text("链接识别成功 · $platform", fontSize = 10.sp, color = DSColors.fairway)
                hasInvalid -> Text("无法识别该视频链接", fontSize = 10.sp, color = DSColors.flagRed)
            }
        }
        if (url.isEmpty()) {
            Text(
                "粘贴",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3D9A6A), DSColors.fairway)))
                    .clickable(onClick = onPaste)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Text(
                "清除",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DSColors.warmWhite)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun VideoPreviewCard(
    title: String,
    platform: String,
    thumbnailUrl: String?,
    durationSec: Double?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.55f), Color.Transparent))),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(platformColor(platform).copy(0.9f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformBadgeText(platform = platform, compact = true)
                Spacer(Modifier.width(5.dp))
                Text(
                    platform,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            durationSec?.takeIf { it > 0 }?.let { d ->
                Text(
                    formatDuration(d),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(0.7f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .offset(x = 1.5.dp),
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(title.ifBlank { "正在获取标题…" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(platformColor(platform)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlatformBadgeText(platform = platform, compact = true)
                }
                Spacer(Modifier.width(8.dp))
                Text(platform, fontSize = 12.sp, color = DSColors.shadowGray)
            }
        }
    }
}

@Composable
private fun ConfirmButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.35f),
                spotColor = DSColors.fairway.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3D9A6A), DSColors.fairway)))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text("添加到 Memo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun InvalidLinkCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.flagRed.copy(0.08f))
            .border(1.dp, DSColors.flagRed.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("视频链接无效", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.flagRed)
            Text(
                "当前只支持 B站 / YouTube / 小红书 / 抖音 的视频链接。请检查是否复制完整，或换一条试试。",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SupportedPlatformsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text("支持平台", fontSize = 11.sp, color = DSColors.shadowGray)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            supportedPlatformItems.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .shadow(
                                elevation = 1.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = Color.Black.copy(alpha = 0.05f),
                                spotColor = Color.Black.copy(alpha = 0.05f),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.badge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                    Text(item.name, fontSize = 10.sp, color = DSColors.shadowGray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun HelperCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.fairway.copy(0.06f))
            .padding(16.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = DSColors.fairway, modifier = Modifier.padding(top = 2.dp).size(15.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("如何获取视频链接？", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            Text(
                "在视频平台点击「分享」→「复制链接」，然后粘贴到上方输入框即可自动识别。",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DecorativeFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(24.dp).height(1.dp).background(DSColors.goldBright.copy(0.4f)))
        Text("— 挥呗Memo —", fontSize = 10.sp, color = DSColors.shadowGray.copy(0.6f), modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.width(24.dp).height(1.dp).background(DSColors.goldBright.copy(0.4f)))
    }
}

private fun platformColor(p: String): Color = when (p) {
    "哔哩哔哩" -> Color(0xFFFB7299)
    "YouTube" -> Color(0xFFFF0000)
    "小红书" -> Color(0xFFFF2442)
    "抖音" -> Color(0xFF1B2A21)
    else -> DSColors.fairway
}

private fun Context.readClipboardText(): String? = runCatching {
    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clip.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(this)
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}.getOrNull()

private data class SupportedPlatformItem(
    val name: String,
    val platform: String,
    val color: Color,
    val badge: String,
)

private val supportedPlatformItems = listOf(
    SupportedPlatformItem("B站", "哔哩哔哩", Color(0xFFFB7299), "TV"),
    SupportedPlatformItem("YouTube", "YouTube", Color(0xFFFF0000), "YT"),
    SupportedPlatformItem("小红书", "小红书", Color(0xFFFF2442), "RED"),
    SupportedPlatformItem("抖音", "抖音", Color(0xFF1B2A21), "DY"),
)

@Composable
private fun PlatformBadgeText(platform: String, compact: Boolean = false) {
    Text(
        supportedPlatformItems.firstOrNull { it.platform == platform }?.badge ?: "URL",
        color = Color.White,
        fontSize = if (compact) 7.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
