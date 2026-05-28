package com.mygolfswing.app.ui.memo

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * V2 快速添加 sheet（对齐 iOS `V2MemoCreateSheet`）。
 *
 * - 主页入口：4 张（新建目标 / 视频 Memo / 文字 Memo / 挥杆记录）
 * - Memo 页入口：3 张（视频 / 文字 / 挥杆）
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
            Text(
                text = "快速添加",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            if (showCreateGoal) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    GridCard(
                        icon = Icons.Filled.Adjust,
                        title = "新建目标",
                        subtitle = "设定清晰练习方向",
                        gradient = listOf(Color(0xFF1B4332), Color(0xFF40916C)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateGoal() },
                    )
                    GridCard(
                        icon = Icons.Filled.Videocam,
                        title = "视频 Memo",
                        subtitle = "上传或拍摄视频",
                        gradient = listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateVideoMemo() },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    GridCard(
                        icon = Icons.Filled.Description,
                        title = "文字 Memo",
                        subtitle = "记录感悟与心得",
                        gradient = listOf(Color(0xFF0D2B1F), Color(0xFF2D6A4F)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateTextMemo() },
                    )
                    GridCard(
                        icon = Icons.Filled.Videocam,
                        title = "挥杆记录",
                        subtitle = "快速拍摄挥杆动作",
                        gradient = listOf(Color(0xFF1B4332), Color(0xFF52B788)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateSwingClip() },
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    GridCard(
                        icon = Icons.Filled.Videocam,
                        title = "视频 Memo",
                        subtitle = "上传或拍摄视频",
                        gradient = listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateVideoMemo() },
                    )
                    GridCard(
                        icon = Icons.Filled.Description,
                        title = "文字 Memo",
                        subtitle = "记录感悟与心得",
                        gradient = listOf(Color(0xFF0D2B1F), Color(0xFF2D6A4F)),
                        modifier = Modifier.weight(1f),
                        onClick = { onDismiss(); onCreateTextMemo() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            CancelButton(onClick = onDismiss)
        }
    }
}

@Composable
private fun GridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() },
    ) {
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
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF2EFE8))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "取消", color = Color(0xFF374151), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
