package com.mygolfswing.app.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
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
    qrRequestKey: Int = 0,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(qrRequestKey, profile.isLoggedIn) {
        if (qrRequestKey > 0 && profile.isLoggedIn) {
            showQrDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
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
            SettingsCard(
                icon = Icons.Filled.QrCode,
                iconBg = DSColors.fairway,
                title = "我的二维码",
                subtitle = if (profile.isLoggedIn) "分享给球友加好友" else "登录后生成好友二维码",
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
                enabled = false,
            )
            AboutCard()
        }
    }

    if (showQrDialog) {
        UserQrDialog(
            profile = profile,
            onCopy = { content ->
                runCatching {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("huibei-user-qrcode", content))
                }.fold(
                    onSuccess = { "链接已复制" },
                    onFailure = { "复制失败，请稍后重试" },
                )
            },
            onDismiss = { showQrDialog = false },
        )
    }
}

@Composable
private fun UserQrDialog(
    profile: com.mygolfswing.app.data.store.V2UserProfile,
    onCopy: (String) -> String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = profile.userId.orEmpty()
    val qrContent = remember(userId) { userQrContent(userId) }
    var styleIndex by remember { mutableIntStateOf(0) }
    val qrStyle = remember(styleIndex) { UserQrStyle.presets[styleIndex % UserQrStyle.presets.size] }
    val qrBitmap = remember(qrContent, qrStyle) { generateQrBitmap(qrContent, foregroundColor = qrStyle.qrColor) }
    var saving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.v2NavigationBarPadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "我的二维码",
                style = DSTypography.titleMedium,
                color = DSColors.inkGreen,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    V2UserAvatar(
                        avatar = profile.avatar,
                        modifier = Modifier.size(52.dp),
                        placeholderIconSize = 24.dp,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = profile.nickname.ifBlank { "高尔夫球友" },
                            style = DSTypography.bodyBold,
                            color = DSColors.inkGreen,
                        )
                        Text(
                            text = profile.location?.takeIf { it.isNotBlank() }
                                ?: profile.homeCourse.takeIf { it != "未设置" }
                                ?: profile.username,
                            style = DSTypography.caption,
                            color = DSColors.shadowGray,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(226.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "我的二维码",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text("二维码生成失败", style = DSTypography.caption, color = DSColors.flagRed)
                    }
                }

                Text(
                    text = "扫一扫上面的二维码图案，加我为好友。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = qrContent,
                    fontSize = 11.sp,
                    color = DSColors.shadowGray.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "当前样式：${qrStyle.name}",
                    style = DSTypography.caption,
                    color = DSColors.fairway,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                saveMessage?.let {
                    Text(
                        text = it,
                        style = DSTypography.caption,
                        color = if (it.contains("失败") || it.contains("无法")) DSColors.flagRed else DSColors.fairway,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { saveMessage = onCopy(qrContent) }) {
                    Text("复制链接", color = DSColors.fairway)
                }
                TextButton(
                    enabled = !saving,
                    onClick = {
                        styleIndex = (styleIndex + 1) % UserQrStyle.presets.size
                        saveMessage = null
                    },
                ) {
                    Text("换个样式", color = DSColors.fairway)
                }
                TextButton(
                    enabled = qrBitmap != null && !saving,
                    onClick = {
                        val bitmap = qrBitmap ?: return@TextButton
                        scope.launch {
                            saving = true
                            try {
                                saveMessage = saveQrShareImage(context, profile, qrContent, bitmap, qrStyle).message
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                saveMessage = profileQrGalleryThrowableError("保存图片失败，请稍后重试", e)
                            } finally {
                                saving = false
                            }
                        }
                    },
                ) {
                    Text(if (saving) "保存中..." else "保存图片", color = DSColors.fairway)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DSColors.fairway)
            }
        },
    )
}

private fun userQrContent(userId: String): String = "huibei://user/$userId"

private data class UserQrStyle(
    val name: String,
    val qrColor: Int,
    val avatarColor: Int,
) {
    companion object {
        val presets = listOf(
            UserQrStyle("经典黑", AndroidColor.BLACK, AndroidColor.rgb(18, 111, 76)),
            UserQrStyle("果岭绿", AndroidColor.rgb(18, 111, 76), AndroidColor.rgb(18, 111, 76)),
            UserQrStyle("奖杯金", AndroidColor.rgb(132, 94, 31), AndroidColor.rgb(176, 124, 38)),
        )
    }
}

private fun generateQrBitmap(
    content: String,
    sizePx: Int = 720,
    foregroundColor: Int = AndroidColor.BLACK,
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
    }
}.getOrNull()

private suspend fun saveQrShareImage(
    context: Context,
    profile: com.mygolfswing.app.data.store.V2UserProfile,
    qrContent: String,
    qrBitmap: Bitmap,
    qrStyle: UserQrStyle,
): GalleryImageSaveResult = withContext(Dispatchers.IO) {
    runCatching {
        fun fail(message: String, uri: Uri? = null): GalleryImageSaveResult {
            uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            return GalleryImageSaveResult(saved = false, message = message)
        }

        val image = runCatching { generateQrShareBitmap(profile, qrContent, qrBitmap, qrStyle) }
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

private fun generateQrShareBitmap(
    profile: com.mygolfswing.app.data.store.V2UserProfile,
    qrContent: String,
    qrBitmap: Bitmap,
    qrStyle: UserQrStyle,
): Bitmap {
    val width = 1125
    val height = 1500
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(27, 42, 33)
        textSize = 52f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(107, 114, 128)
        textSize = 38f
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(156, 163, 175)
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    val appPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(190, 190, 190)
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    val avatarRect = RectF(90f, 120f, 258f, 288f)
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(232, 242, 236)
        canvas.drawRoundRect(avatarRect, 30f, 30f, this)
        color = qrStyle.avatarColor
        textSize = 68f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val initial = profile.nickname.firstOrNull()?.toString() ?: profile.username.removePrefix("@").firstOrNull()?.toString() ?: "挥"
        canvas.drawText(initial, avatarRect.centerX(), avatarRect.centerY() + 24f, this)
    }

    canvas.drawText(profile.nickname.ifBlank { "高尔夫球友" }.take(18), 300f, 180f, titlePaint)
    val region = profile.location?.takeIf { it.isNotBlank() }
        ?: profile.homeCourse.takeIf { it != "未设置" }
        ?: profile.username
    canvas.drawText(region.take(24), 300f, 245f, secondaryPaint)

    val qrSize = 720
    val qrLeft = (width - qrSize) / 2f
    val qrTop = 390f
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        canvas.drawRoundRect(RectF(qrLeft - 36, qrTop - 36, qrLeft + qrSize + 36, qrTop + qrSize + 36), 42f, 42f, this)
        color = AndroidColor.rgb(228, 232, 229)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        canvas.drawRoundRect(RectF(qrLeft - 36, qrTop - 36, qrLeft + qrSize + 36, qrTop + qrSize + 36), 42f, 42f, this)
    }
    canvas.drawBitmap(qrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

    canvas.drawText("扫一扫上面的二维码图案，加我为好友。", width / 2f, 1225f, mutedPaint)
    canvas.drawText(qrContent.take(48), width / 2f, 1290f, mutedPaint.apply { textSize = 28f })
    canvas.drawText("挥呗 - Where Every Swing Matters", width / 2f, 1408f, appPaint)
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = if (enabled) 0.08f else 0f),
                spotColor = DSColors.fairway.copy(alpha = if (enabled) 0.08f else 0f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite.copy(alpha = if (enabled) 1f else 0.6f))
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(horizontal = DSSpacing.lg)
            .height(64.dp),
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
        if (enabled) {
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
