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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.common.v2NavigationBarPadding

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
    isGuest: Boolean = false,
    onCreateGoal: () -> Unit = {},
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateSwingClip: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lockedAction by remember { mutableStateOf<QuickAddLockedAction?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .v2NavigationBarPadding()
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
                    isGuest = isGuest,
                    onLocked = { lockedAction = it },
                    onCreateGoal = { onDismiss(); onCreateGoal() },
                    onCreateVideoMemo = { onDismiss(); onCreateVideoMemo() },
                    onCreateTextMemo = { onDismiss(); onCreateTextMemo() },
                    onCreateSwingClip = { onDismiss(); onCreateSwingClip() },
                )
            } else {
                QuickAddMemoGrid(
                    isGuest = isGuest,
                    onLocked = { lockedAction = it },
                    onCreateVideoMemo = { onDismiss(); onCreateVideoMemo() },
                    onCreateTextMemo = { onDismiss(); onCreateTextMemo() },
                    onCreateSwingClip = { onDismiss(); onCreateSwingClip() },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CancelButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    lockedAction?.let { action ->
        QuickAddGuestLoginDialog(
            action = action,
            onBack = { lockedAction = null },
            onLogin = {
                lockedAction = null
                onDismiss()
                when (action) {
                    QuickAddLockedAction.GOAL -> onCreateGoal()
                    QuickAddLockedAction.VIDEO_MEMO -> onCreateVideoMemo()
                    QuickAddLockedAction.TEXT_MEMO -> onCreateTextMemo()
                    QuickAddLockedAction.SWING -> onCreateSwingClip()
                }
            },
        )
    }
}

@Composable
private fun QuickAddHomeGrid(
    isGuest: Boolean,
    onLocked: (QuickAddLockedAction) -> Unit,
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
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.GOAL) else onCreateGoal() },
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_video,
                fallbackGradient = listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
                icon = Icons.Filled.Videocam,
                title = "视频 Memo",
                subtitle = "上传或拍摄视频",
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.VIDEO_MEMO) else onCreateVideoMemo() },
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
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.TEXT_MEMO) else onCreateTextMemo() },
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_swing,
                fallbackGradient = listOf(Color(0xFF1B4332), Color(0xFF52B788)),
                icon = Icons.Filled.SportsGolf,
                title = "挥杆记录",
                subtitle = "快速拍摄挥杆动作",
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.SWING) else onCreateSwingClip() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickAddMemoGrid(
    isGuest: Boolean,
    onLocked: (QuickAddLockedAction) -> Unit,
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
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.VIDEO_MEMO) else onCreateVideoMemo() },
                modifier = Modifier.weight(1f),
            )
            QuickAddCard(
                imageRes = R.drawable.v2_quickadd_text,
                fallbackGradient = listOf(Color(0xFF0D2B1F), Color(0xFF2D6A4F)),
                icon = Icons.Filled.Create,
                title = "文字 Memo",
                subtitle = "记录感悟与心得",
                isLocked = isGuest,
                onClick = { if (isGuest) onLocked(QuickAddLockedAction.TEXT_MEMO) else onCreateTextMemo() },
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
                    isLocked = isGuest,
                    onClick = { if (isGuest) onLocked(QuickAddLockedAction.SWING) else onCreateSwingClip() },
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@Composable
@android.annotation.SuppressLint("ModifierParameter")
private fun QuickAddCard(
    @DrawableRes imageRes: Int,
    fallbackGradient: List<Color>,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = fallbackGradient)),
        )
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
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(icon = icon)
                Spacer(Modifier.weight(1f))
                if (isLocked) QuickAddLockedBadge()
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = QuickAddCardTextShadow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        style = TextStyle(shadow = QuickAddCardTextShadow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                QuickAddChevron()
            }
        }
    }
}

@Composable
private fun QuickAddLockedBadge() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.90f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = "需要登录", tint = Color(0xFF1B4332), modifier = Modifier.size(12.dp))
    }
}

private enum class QuickAddLockedAction(
    val promptTitle: String,
    val message: String,
) {
    GOAL("登录后新建目标", "创建目标和打卡进度会保存到账号里，需要先登录或注册。"),
    VIDEO_MEMO("登录后创建视频 Memo", "视频 Memo 会保存视频、笔记和回看记录，需要先登录或注册。"),
    TEXT_MEMO("登录后创建文字 Memo", "文字 Memo 会保存到你的训练记录里，需要先登录或注册。"),
    SWING("登录后记录挥杆", "挥杆记录会保存视频片段和练习日期，需要先登录或注册。"),
}

@Composable
private fun QuickAddGuestLoginDialog(
    action: QuickAddLockedAction,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quick-add-guest-overlay")
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable(onClick = onBack),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B4332)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(action.promptTitle, color = Color(0xFF1A2E22), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(action.message, color = Color(0xFF8A9E91), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2EFE8)).clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("返回", color = Color(0xFF374151), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1B4332)).clickable(onClick = onLogin),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("登录/注册", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddChevron() {
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .size(12.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier
                .size(12.dp)
                .padding(top = 1.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(12.dp),
        )
    }
}

private val QuickAddCardTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.45f),
    offset = Offset(0f, 1f),
    blurRadius = 2f,
)

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.36f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.22f)),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
