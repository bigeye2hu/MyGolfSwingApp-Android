package com.mygolfswing.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.ui.designsystem.DSColors

internal fun isLoadableAvatarReference(avatar: String?): Boolean {
    val v = avatar?.trim().orEmpty()
    if (v.isEmpty()) return false
    return listOf("http://", "https://", "file://", "content://")
        .any { prefix -> v.startsWith(prefix, ignoreCase = true) && v.length > prefix.length }
}

internal fun avatarFallbackInitial(value: String?): String? =
    value?.trim()?.takeIf(String::isNotEmpty)?.take(1)?.uppercase()

/**
 * 用户头像：网络 URL / 本地 URI → Coil；预设 `Avatar_*` key → 内置头像；
 * 缺失/加载失败时优先显示姓名首字，没有姓名时再显示占位图标。
 */
@Composable
fun V2UserAvatar(
    avatar: String?,
    modifier: Modifier = Modifier,
    fallbackFontSize: TextUnit = 16.sp,
    placeholderIconSize: androidx.compose.ui.unit.Dp = 28.dp,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackText: String? = null,
    fallbackColor: Color = DSColors.mistGreen,
    shape: Shape = CircleShape,
) {
    val fallbackInitial = avatarFallbackInitial(fallbackText)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoadableAvatarReference(avatar) -> {
                // Keep the name fallback underneath the remote image so loading and
                // failures match iOS without adding subcomposition to long avatar lists.
                AvatarFallback(
                    initial = fallbackInitial,
                    fontSize = fallbackFontSize,
                    iconSize = placeholderIconSize,
                )
                AsyncImage(
                    model = avatar,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }
            else -> {
                val drawable = V2PresetAvatars.drawableFor(avatar)
                if (drawable != null) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                    )
                } else {
                    AvatarFallback(
                        initial = fallbackInitial,
                        fontSize = fallbackFontSize,
                        iconSize = placeholderIconSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarFallback(
    initial: String?,
    fontSize: TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    if (initial != null) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    } else {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "头像",
            tint = DSColors.shadowGray,
            modifier = Modifier.size(iconSize),
        )
    }
}
