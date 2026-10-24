package com.mygolfswing.app.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mygolfswing.app.R
import com.mygolfswing.app.data.friend.v2FriendQrContent
import com.mygolfswing.app.data.role.V2CoachAuthorizationStatus
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.store.V2UserProfile
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.interaction.AddFriendDialog
import com.mygolfswing.app.ui.interaction.V2FriendQrScannerDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * V2「我」页右上角精简设置页，对齐 iOS `V2ProfileSettingsView`。
 */
@Composable
fun V2ProfileSettingsScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit,
    onRequestLoginForQr: () -> Unit = onRequestLogin,
    onOpenLearningFavorites: () -> Unit = onRequestLogin,
    coachAuthorizationStatus: V2CoachAuthorizationStatus = V2CoachAuthorizationStatus.NONE,
    coachPreviewEnabled: Boolean = false,
    isDebuggable: Boolean = false,
    hasActiveDelivery: Boolean = false,
    onSwitchToCoach: () -> Unit = {},
    onEnableCoachPreview: () -> Unit = {},
    qrRequestKey: Int = 0,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    var showQrDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationPrivacy by rememberSaveable { mutableStateOf(false) }
    var scannedFriendUserId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(qrRequestKey, profile.isLoggedIn) {
        if (qrRequestKey > 0 && profile.isLoggedIn) {
            showQrDialog = true
        }
    }

    if (showNotificationPrivacy) {
        V2NotificationPrivacyScreen(onBack = { showNotificationPrivacy = false })
        return
    }

    if (showQrDialog) {
        ProfileQrCodeScreen(
            profile = profile,
            onBack = { showQrDialog = false },
            onScanned = { userId ->
                showQrDialog = false
                scannedFriendUserId = userId
            },
        )
        return
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            V2NavTopBarCentered(title = "设置", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg)
                    .padding(top = DSSpacing.md, bottom = DSSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                if (coachAuthorizationStatus.canUseCoachExperience || (isDebuggable && coachPreviewEnabled)) {
                    SettingsCard(
                        icon = Icons.Filled.SupervisorAccount,
                        iconBg = DSColors.trophyGold,
                        iconSize = 17.dp,
                        title = "切换到教练版",
                        subtitle = if (hasActiveDelivery) "视频发送完成后即可切换" else "进入独立的学员与视频工作台",
                        enabled = !hasActiveDelivery,
                        showsChevron = true,
                        shadowElevation = 8.dp,
                        onClick = onSwitchToCoach,
                    )
                } else if (coachAuthorizationStatus != V2CoachAuthorizationStatus.NONE) {
                    SettingsCard(
                        icon = Icons.Filled.SupervisorAccount,
                        iconBg = DSColors.trophyGold,
                        iconSize = 17.dp,
                        title = "教练身份",
                        subtitle = coachAuthorizationStatus.displayText,
                        showsChevron = false,
                        shadowElevation = 0.dp,
                    )
                }
                if (isDebuggable && !coachAuthorizationStatus.canUseCoachExperience) {
                    SettingsCard(
                        icon = Icons.Filled.Gavel,
                        iconBg = Color(0xFF7A5AF8),
                        iconSize = 17.dp,
                        title = "预览教练端界面",
                        subtitle = "仅开发版可见，使用 Figma 示例数据",
                        enabled = !hasActiveDelivery,
                        showsChevron = true,
                        shadowElevation = 0.dp,
                        onClick = onEnableCoachPreview,
                    )
                }
                SettingsCard(
                    icon = Icons.Filled.QrCode,
                    iconBg = DSColors.fairway,
                    title = "我的二维码",
                    subtitle = if (profile.isLoggedIn) "分享给球友加好友" else "登录后生成好友二维码",
                    cardHeight = 64.dp,
                    showsChevron = true,
                    shadowElevation = 8.dp,
                    onClick = {
                        if (profile.isLoggedIn) showQrDialog = true else onRequestLoginForQr()
                    },
                )
                SettingsCard(
                    icon = Icons.Filled.Notifications,
                    iconBg = Color(0xFFF39C12),
                    iconSize = 16.dp,
                    title = "通知设置",
                    subtitle = "敬请期待",
                    cardHeight = 64.dp,
                    showsChevron = false,
                    surfaceAlpha = 0.6f,
                    shadowElevation = 0.dp,
                    onClick = { showNotificationPrivacy = true },
                )
                SettingsCard(
                    icon = Icons.Filled.Bookmark,
                    iconBg = DSColors.fairway,
                    iconSize = 17.dp,
                    title = "学习收藏",
                    subtitle = "个人收藏，不按关系空间分类",
                    showsChevron = true,
                    shadowElevation = 0.dp,
                    onClick = onOpenLearningFavorites,
                )
                AboutCard()
            }
        }
    }

    scannedFriendUserId?.let { userId ->
        AddFriendDialog(
            currentUserId = profile.userId,
            initialScannedUserId = userId,
            onDismiss = { scannedFriendUserId = null },
            onCompleted = { scannedFriendUserId = null },
        )
    }
}

@Composable
internal fun ProfileQrCodeScreen(
    profile: V2UserProfile,
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = profile.userId.orEmpty()
    val qrContent = remember(userId) { v2FriendQrContent(userId) }
    var styleIndex by remember { mutableIntStateOf(0) }
    val qrStyle = remember(styleIndex) { UserQrStyle.presets[styleIndex % UserQrStyle.presets.size] }
    val appLogo = remember(context) { BitmapFactory.decodeResource(context.resources, R.drawable.auth_logo) }
    val qrBitmap = remember(qrContent, qrStyle, appLogo) {
        generateProfileQrBitmap(qrContent, foregroundColor = qrStyle.qrColor, centerLogo = appLogo)
    }
    var saving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    BackHandler(enabled = !showScanner, onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F0F23)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .v2StatusBarPadding()
                .v2NavigationBarPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    "我的二维码",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .shadow(20.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    V2UserAvatar(
                        avatar = profile.avatar,
                        modifier = Modifier.size(56.dp),
                        fallbackFontSize = 21.sp,
                        placeholderIconSize = 26.dp,
                        fallbackText = profile.nickname.ifBlank { "用户" },
                        fallbackColor = DSColors.fairway,
                        shape = RoundedCornerShape(10.dp),
                    )
                    Text(
                        text = profile.nickname.ifBlank { "用户" },
                        color = Color(0xFF333333),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "我的二维码",
                            modifier = Modifier.fillMaxSize(),
                            filterQuality = FilterQuality.None,
                        )
                    } else {
                        Text("二维码生成失败", color = DSColors.flagRed, fontSize = 13.sp)
                    }
                }
                Text(
                    text = "扫一扫上面的二维码图案，加我为好友。",
                    color = Color(0xFF999999),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                )
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrActionButton(
                    icon = Icons.Filled.QrCodeScanner,
                    title = "扫一扫",
                    enabled = !saving,
                    onClick = { showScanner = true },
                    modifier = Modifier.weight(1f),
                )
                QrActionDivider()
                QrActionButton(
                    icon = Icons.Filled.Palette,
                    title = "换个样式",
                    enabled = !saving,
                    onClick = { styleIndex = (styleIndex + 1) % UserQrStyle.presets.size },
                    modifier = Modifier.weight(1f),
                )
                QrActionDivider()
                QrActionButton(
                    icon = Icons.Filled.SaveAlt,
                    title = if (saving) "保存中..." else "保存图片",
                    enabled = qrBitmap != null && !saving,
                    onClick = {
                        val bitmap = qrBitmap ?: return@QrActionButton
                        scope.launch {
                            saving = true
                            try {
                                saveMessage = saveQrShareImage(context, profile, bitmap).message
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                saveMessage = profileQrGalleryThrowableError("保存图片失败，请稍后重试", e)
                            } finally {
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showScanner) {
        V2FriendQrScannerDialog(
            onDismiss = { showScanner = false },
            onScanned = { userId ->
                showScanner = false
                onScanned(userId)
            },
        )
    }

    saveMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { saveMessage = null },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { saveMessage = null }) { Text("确定") }
            },
        )
    }
}

@Composable
private fun QrActionButton(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(title, color = Color.White.copy(alpha = if (enabled) 0.8f else 0.45f), fontSize = 12.sp)
    }
}

@Composable
private fun QrActionDivider() {
    Box(Modifier.size(width = 1.dp, height = 24.dp).background(Color.White.copy(alpha = 0.3f)))
}

private data class UserQrStyle(
    val name: String,
    val qrColor: Int,
) {
    companion object {
        val presets = listOf(
            UserQrStyle("经典", AndroidColor.BLACK),
            UserQrStyle("紫色", AndroidColor.rgb(139, 92, 246)),
            UserQrStyle("蓝色", AndroidColor.rgb(59, 130, 246)),
            UserQrStyle("绿色", AndroidColor.rgb(34, 197, 94)),
        )
    }
}

internal fun generateProfileQrBitmap(
    content: String,
    sizePx: Int = 720,
    foregroundColor: Int = AndroidColor.BLACK,
    centerLogo: Bitmap? = null,
): Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    createBitmap(sizePx, sizePx).also { bitmap ->
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                bitmap[x, y] = if (matrix[x, y]) foregroundColor else AndroidColor.WHITE
            }
        }
        centerLogo?.let { logo ->
            val canvas = Canvas(bitmap)
            val logoSize = sizePx * 0.22f
            val left = (sizePx - logoSize) / 2f
            val top = (sizePx - logoSize) / 2f
            val logoRect = RectF(left, top, left + logoSize, top + logoSize)
            val backgroundRect = RectF(left - 10f, top - 10f, left + logoSize + 10f, top + logoSize + 10f)
            canvas.drawRoundRect(
                backgroundRect,
                22f,
                22f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE },
            )
            canvas.drawBitmap(logo, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG))
        }
    }
}.getOrNull()

private suspend fun saveQrShareImage(
    context: Context,
    profile: V2UserProfile,
    qrBitmap: Bitmap,
): GalleryImageSaveResult = withContext(Dispatchers.IO) {
    runCatching {
        fun fail(message: String, uri: Uri? = null): GalleryImageSaveResult {
            uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            return GalleryImageSaveResult(saved = false, message = message)
        }

        val image = runCatching { generateQrShareBitmap(profile, qrBitmap) }
            .getOrElse { return@runCatching fail("二维码图片生成失败，请稍后重试") }
        val resolver = context.contentResolver
        val fileName = "huibei_qrcode_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Huibei")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching fail("系统相册暂时不可写，请检查存储空间后重试")
        val wrote = runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                image.compress(Bitmap.CompressFormat.PNG, 100, output)
            } == true
        }.getOrDefault(false)
        if (!wrote) return@runCatching fail("无法写入系统相册，请稍后重试", uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            val published = runCatching { resolver.update(uri, values, null, null) }.isSuccess
            if (!published) return@runCatching fail("相册保存未完成，请稍后重试", uri)
        }
        GalleryImageSaveResult(saved = true, message = "已保存到相册")
    }.getOrElse {
        GalleryImageSaveResult(
            saved = false,
            message = profileQrGalleryThrowableError("保存到相册失败，请稍后重试", it),
        )
    }
}

private data class GalleryImageSaveResult(
    val saved: Boolean,
    val message: String,
)

internal fun generateQrShareBitmap(
    profile: V2UserProfile,
    qrBitmap: Bitmap,
): Bitmap {
    val width = 1125
    val height = 1500
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(51, 51, 51)
        textSize = 51f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(153, 153, 153)
        textSize = 39f
        textAlign = Paint.Align.CENTER
    }
    val appPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(204, 204, 204)
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    val avatarRect = RectF(90f, 120f, 258f, 288f)
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(240, 240, 240)
        canvas.drawRoundRect(avatarRect, 30f, 30f, this)
    }

    canvas.drawText(
        profile.nickname.ifBlank { "高尔夫球友" }.take(18),
        300f,
        150f - titlePaint.fontMetrics.ascent,
        titlePaint,
    )

    val qrSize = 720
    val qrLeft = (width - qrSize) / 2f
    val qrTop = 390f
    canvas.drawBitmap(qrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

    canvas.drawText(
        "扫一扫上面的二维码图案，加我为好友。",
        width / 2f,
        1170f - mutedPaint.fontMetrics.ascent,
        mutedPaint,
    )
    canvas.drawText(
        "挥呗 - Where Every Swing Matters",
        width / 2f,
        1380f - appPaint.fontMetrics.ascent,
        appPaint,
    )
    return bitmap
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconBg: Color,
    iconSize: Dp = 18.dp,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    cardHeight: Dp = 68.dp,
    showsChevron: Boolean = onClick != null,
    surfaceAlpha: Float = 1f,
    shadowElevation: Dp = 0.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = if (shadowElevation > 0.dp) 0.08f else 0f),
                spotColor = DSColors.fairway.copy(alpha = if (shadowElevation > 0.dp) 0.08f else 0f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite.copy(alpha = surfaceAlpha))
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .alpha(if (enabled) 1f else 0.62f)
            .padding(horizontal = DSSpacing.lg)
            .height(cardHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DSColors.shadowGray,
            )
        }
        if (showsChevron) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionText = remember(packageInfo) {
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        "v$versionName ($versionCode)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.04f),
                spotColor = DSColors.fairway.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(vertical = DSSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "挥呗",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.fairway,
        )
        Text(
            text = "Where Every Swing Matters",
            fontSize = 11.sp,
            color = DSColors.shadowGray,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = versionText,
            fontSize = 11.sp,
            color = DSColors.shadowGray.copy(alpha = 0.7f),
        )
    }
}
