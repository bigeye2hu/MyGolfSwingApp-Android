package com.mygolfswing.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.common.V2UserAvatar
import androidx.compose.ui.platform.testTag
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * V2 主页头部（对齐 iOS `V2HomeHeader`）。
 *
 * - 左：48dp 头像（金色 2.5dp 边）
 * - 中：「问候，用户名」+ 首页行动文案
 * - 右：绿色「记录」按钮
 * - 下：教学视频搜索框
 */
@Composable
fun V2HomeHeader(
    displayName: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    isGuest: Boolean = false,
    onRecordTap: () -> Unit = {},
    onSearchSubmit: () -> Unit = {},
) {
    val ringYellow = Color(0xFFE0A82E)
    val titleInk = Color(0xFF1B2A21)
    val mutedGray = Color(0xFF6C757D)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DSLayout.pageHorizontal, vertical = DSLayout.homeHeaderSpacing)
            .testTag("home-header"),
        verticalArrangement = Arrangement.spacedBy(DSLayout.homeHeaderSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(DSLayout.homeAvatar)
                    .testTag("home-avatar")
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = DSColors.fairway.copy(alpha = 0.18f),
                        spotColor = DSColors.fairway.copy(alpha = 0.18f),
                    )
                    .border(
                        width = 2.5.dp,
                        color = if (isGuest) DSColors.fairway else ringYellow,
                        shape = CircleShape,
                    ),
            ) {
                if (isGuest) {
                    Image(
                        painter = painterResource(R.drawable.avatar_xiaoxiong),
                        contentDescription = "游客头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    V2UserAvatar(
                        avatar = avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        fallbackFontSize = 18.sp,
                        placeholderIconSize = 28.dp,
                        fallbackText = displayName,
                        fallbackColor = DSColors.sandBeige,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "${V2Format.greeting()}，$displayName",
                    style = DSTypography.visualGreeting,
                    color = titleInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "更好的一杆，从这里开始",
                    style = DSTypography.visualMeta,
                    color = mutedGray,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier
                    .height(DSLayout.compactButtonHeight)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onRecordTap)
                    .padding(horizontal = 10.dp)
                    .testTag("home-record-button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("记录", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DSLayout.searchFieldHeight)
                .testTag("home-search-field")
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(16.dp))
            BasicTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = DSTypography.visualBody.copy(color = DSColors.inkGreen),
                cursorBrush = SolidColor(DSColors.fairway),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                decorationBox = { innerField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchText.isEmpty()) {
                            Text("搜索视频、技巧或关键词", style = DSTypography.visualBody, color = DSColors.shadowGray)
                        }
                        innerField()
                    }
                },
            )
            if (searchText.isNotEmpty()) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "清除搜索",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(19.dp)
                        .clickable {
                            onSearchTextChange("")
                        },
                )
            }
        }
    }
}
