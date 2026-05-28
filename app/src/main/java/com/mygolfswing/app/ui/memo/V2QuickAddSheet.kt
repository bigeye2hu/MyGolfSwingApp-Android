package com.mygolfswing.app.ui.memo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R

/**
 * V2 快速添加 sheet（对齐 iOS `V2MemoCreateSheet` + Figma 188:1821）。
 *
 * - 主页：2×2 共 4 张（新建目标 / 视频 Memo / 文字 Memo / 挥杆记录）
 * - Memo 页：上排 2 张 + 下排居中 1 张（挥杆）
 * - 卡片背景使用与 iOS 相同的 `v2_quickadd_*` 图片资源
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2QuickAddSheet(
    showCreateGoal: Boolean,
    onCreateGoal: () -> Unit = {},
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateSwingClip: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB)),
            )

            Text(
                text = "快速添加",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (showCreateGoal) {
                QuickAddHomeGrid(
                    onCreateGoal = { onDismiss(); onCreateGoal() },
                    onCreateVideoMemo = { onDismiss(); onCreateVideoMemo() },
                    onCreateTextMemo = { onDismiss(); onCreateTextMemo() },
                    onCreateSwingClip = { onDismiss(); onCreateSwingClip() },
                )
            } else {
                QuickAddMemoGrid(
                    onCreateVideoMemo = { onDismiss(); onCreateVideoMemo() },
                    onCreateTextMemo = { onDismiss(); onCreateTextMemo() },
                    onCreateSwingClip = { onDismiss(); onCreateSwingClip() },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CancelButton(onClick = onDismiss)
        }
    }
}

@Composable
private fun QuickAddHomeGrid(
    onCreateGoal: () -> Unit,
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateSwingClip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_goal,
                fallbackGradient = listOf(Color(0xFF1B4332), Color(0xFF40916C)),
                icon = Icons.Filled.TrackChanges,
                title = "新建目标",
                subtitle = "设定清晰练习方向",
                onClick = onCreateGoal,
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_video,
                fallbackGradient = listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
                icon = Icons.Filled.Videocam,
                title = "视频 Memo",
                subtitle = "上传或拍摄视频",
                onClick = onCreateVideoMemo,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_text,
                fallbackGradient = listOf(Color(0xFF0D2B1F), Color(0xFF2D6A4F)),
                icon = Icons.Filled.Create,
                title = "文字 Memo",
                subtitle = "记录感悟与心得",
                onClick = onCreateTextMemo,
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_swing,
                fallbackGradient = listOf(Color(0xFF1B4332), Color(0xFF52B788)),
                icon = Icons.Filled.SportsGolf,
                title = "挥杆记录",
                subtitle = "快速拍摄挥杆动作",
                onClick = onCreateSwingClip,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickAddMemoGrid(
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateSwingClip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_video,
                fallbackGradient = listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
                icon = Icons.Filled.Videocam,
                title = "视频 Memo",
                subtitle = "上传或拍摄视频",
                onClick = onCreateVideoMemo,
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_text,
                fallbackGradient = listOf(Color(0xFF0D2B1F), Color(0xFF2D6A4F)),
                icon = Icons.Filled.Create,
                title = "文字 Memo",
                subtitle = "记录感悟与心得",
                onClick = onCreateTextMemo,
                modifier = Modifier.weight(1f),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 12.dp) / 2
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QuickAddCard(
                    imageRes = R.drawable.v2_quickadd_swing,
                    fallbackGradient = listOf(Color(0xFF1B4332), Color(0xFF52B788)),
                    icon = Icons.Filled.SportsGolf,
                    title = "挥杆记录",
                    subtitle = "快速拍摄挥杆动作",
                    onClick = onCreateSwingClip,
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    @DrawableRes imageRes: Int,
    fallbackGradient: List<Color>,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.05f),
                        0.5f to Color.Black.copy(alpha = 0.20f),
                        1.0f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            IconBadge(icon = icon)
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF2EFE8))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "取消", color = Color(0xFF374151), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
