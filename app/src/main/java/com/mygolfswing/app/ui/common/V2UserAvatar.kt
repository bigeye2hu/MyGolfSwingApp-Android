package com.mygolfswing.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors

/** 注册页预设头像 key → drawable（与 iOS `DefaultAvatars` 一致） */
private val presetAvatarDrawable = mapOf(
    "xiaoqie" to R.drawable.avatar_xiaoqie,
    "koala" to R.drawable.avatar_xiaokaola,
    "fox" to R.drawable.avatar_xiaohuli,
    "bear" to R.drawable.avatar_xiaoxiong,
    "dino" to R.drawable.avatar_konglong,
)

private fun isRemoteAvatarUrl(avatar: String?): Boolean {
    val v = avatar?.trim().orEmpty()
    return v.startsWith("http://", ignoreCase = true) || v.startsWith("https://", ignoreCase = true)
}

/**
 * 用户头像：远程 URL → Coil；预设 key → 本地头像；否则占位图标（对齐 iOS 头像 3 级回退）。
 */
@Composable
fun V2UserAvatar(
    avatar: String?,
    modifier: Modifier = Modifier,
    emojiFontSize: TextUnit = 28.sp,
    placeholderIconSize: androidx.compose.ui.unit.Dp = 28.dp,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(DSColors.mistGreen),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isRemoteAvatarUrl(avatar) -> {
                AsyncImage(
                    model = avatar,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                val drawable = avatar?.trim()?.let { presetAvatarDrawable[it] }
                if (drawable != null) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "头像",
                        tint = DSColors.shadowGray,
                        modifier = Modifier.size(placeholderIconSize),
                    )
                }
            }
        }
    }
}
