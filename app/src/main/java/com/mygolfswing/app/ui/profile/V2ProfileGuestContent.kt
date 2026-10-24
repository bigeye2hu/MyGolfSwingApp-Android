package com.mygolfswing.app.ui.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.mygolfswing.app.ui.designsystem.DSColors

/** Logged-out profile experience aligned with iOS `V2ProfileGuest*`. */
@Composable
internal fun V2ProfileGuestContent(
    onLogin: () -> Unit,
    onBrowsePlaza: () -> Unit,
    onBrowseMemo: () -> Unit,
    onFeedback: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    V2ProfileGuestHero(onLogin)
    V2ProfileGuestJourney(onBrowsePlaza, onBrowseMemo, onLogin)
    V2ProfileGuestPermission(onLogin)
    V2ProfileGuestValue(onLogin)
    V2ProfileGuestScope()
    V2ProfileGuestSupport(onFeedback, onTerms, onPrivacy)
}

@Composable
private fun V2ProfileGuestHero(onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().guestSurface(22).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White)
                    .padding(6.dp).clip(CircleShape).background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Visibility, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("游客体验中", color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "你正在以只读身份浏览挥呗。公开视频和示例内容可以直接看，个人记录会在登录后保存。",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GuestImageTile(
                R.drawable.v2_quickadd_video,
                Icons.Filled.PlayCircle,
                "公开视频",
                "可播放",
                modifier = Modifier.weight(1f),
            )
            GuestImageTile(
                R.drawable.v2_quickadd_swing,
                Icons.AutoMirrored.Filled.MenuBook,
                "示例 Memo",
                "只读预览",
                modifier = Modifier.weight(1f),
            )
            GuestImageTile(
                R.drawable.homepage_analysis,
                Icons.Filled.Flag,
                "轨迹分析",
                "看流程",
                modifier = Modifier.weight(1f),
            )
        }
        GuestPrimaryButton("登录/注册，保存我的练习", Icons.Filled.Person, onLogin)
    }
}

@Composable
private fun V2ProfileGuestJourney(onBrowsePlaza: () -> Unit, onBrowseMemo: () -> Unit, onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().guestSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GuestSectionHeader("游客体验路线", "先看价值，再决定要不要保存自己的练习。", Icons.Filled.Visibility)
        GuestJourneyStep("1", Icons.Filled.PlayCircle, "先浏览公开视频", "看教练讲解、姿势案例和练习方法", "去广场", onBrowsePlaza)
        GuestJourneyStep("2", Icons.AutoMirrored.Filled.MenuBook, "再看示例 Memo", "理解视频、文字和标签如何帮你复盘", "看示例", onBrowseMemo)
        GuestJourneyStep("3", Icons.Filled.Shield, "开始保存时登录", "收藏、创建、录制和 AI 对话会先进入登录/注册", "登录", onLogin)
    }
}

@Composable
private fun GuestJourneyStep(
    index: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionTitle: String,
    action: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DSColors.warmWhite.copy(alpha = 0.76f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier.align(Alignment.Center).size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(DSColors.mistGreen, DSColors.sandBeige.copy(alpha = 0.75f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(DSColors.deepGreen),
                contentAlignment = Alignment.Center,
            ) { Text(index, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text(
            actionTitle,
            color = DSColors.fairway,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(DSColors.mistGreen)
                .clickable(onClick = action).padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun V2ProfileGuestPermission(onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().guestSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GuestSectionHeader(
            "游客能做什么",
            "浏览和预览保持轻松；一旦要把内容变成自己的记录，就先登录。",
            Icons.Filled.Checklist,
            prominent = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GuestPermissionColumn("现在可看", "只读", Icons.Filled.Visibility, DSColors.fairway, listOf("公开视频", "示例 Memo", "工具流程", "协议隐私"), Modifier.weight(1f))
            GuestPermissionColumn("登录后做", "保存", Icons.Filled.Person, DSColors.trophyGold, listOf("收藏互动", "新建 Memo", "录制挥杆", "AI 历史"), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(50))
                .background(DSColors.mistGreen.copy(alpha = 0.82f)).clickable(onClick = onLogin)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.LockOpen, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
            Text("登录后开始保存", color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun GuestPermissionColumn(
    title: String,
    badge: String,
    icon: ImageVector,
    accent: Color,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(DSColors.warmWhite.copy(alpha = 0.76f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Text(title, color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            Text(badge, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 4.dp))
        }
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(accent.copy(alpha = 0.72f)))
                Text(item, color = DSColors.shadowGray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun V2ProfileGuestValue(onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().guestSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(DSColors.deepGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("登录后会保存什么", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("目标进度、Memo、挥杆记录、收藏视频和 AI 建议都会跟随账号，换设备也能继续看。", color = DSColors.shadowGray, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GuestValuePill(Icons.Filled.Flag, "目标进度", Modifier.weight(1f))
            GuestValuePill(Icons.Filled.Bookmark, "视频收藏", Modifier.weight(1f))
            GuestValuePill(Icons.Filled.Psychology, "AI 档案", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(50))
                .background(DSColors.mistGreen).clickable(onClick = onLogin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("建立我的练习档案", color = DSColors.fairway, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = DSColors.fairway, modifier = Modifier.padding(start = 6.dp).size(16.dp))
        }
    }
}

@Composable
private fun GuestValuePill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(30.dp).clip(RoundedCornerShape(50)).background(DSColors.mistGreen.copy(alpha = 0.75f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
        Text(text, color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp), maxLines = 1)
    }
}

@Composable
private fun V2ProfileGuestScope() {
    Column(
        modifier = Modifier.fillMaxWidth().guestSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GuestSectionHeader("游客模式边界", "可以安心浏览；一旦要保存到你的练习档案，会先进入登录/注册。", Icons.Filled.Shield)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GuestBoundaryTile(R.drawable.v2_quickadd_video, Icons.Filled.Visibility, "可以浏览", "公开视频 / 示例 Memo / 工具预览", "只读", DSColors.fairway, Modifier.weight(1f))
            GuestBoundaryTile(R.drawable.homepage_analysis, Icons.Filled.Person, "登录保存", "目标 / 录制 / 收藏 / AI 历史", "账号", DSColors.trophyGold, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen.copy(alpha = 0.42f)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Text("游客不会生成个人练习记录；取消登录后仍停留在原页面继续浏览。", color = DSColors.shadowGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun GuestBoundaryTile(
    @DrawableRes imageRes: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(112.dp).clip(RoundedCornerShape(16.dp))) {
        Image(painterResource(imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.68f)))))
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(badge, color = DSColors.deepGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.88f)).padding(horizontal = 7.dp, vertical = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V2ProfileGuestSupport(onFeedback: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().guestSurface()) {
        Text("帮助与条款", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 12.dp))
        GuestSupportRow(Icons.Filled.Forum, Color(0xFFF39C12), "意见反馈", "登录后提交问题与建议", true, onFeedback)
        GuestSupportRow(Icons.Filled.Description, Color(0xFF8E8E93), "用户协议", "了解服务条款", true, onTerms)
        GuestSupportRow(
            Icons.Filled.Shield,
            Color(0xFF3D5A80),
            "隐私政策",
            "数据安全与隐私保护",
            false,
            onPrivacy,
            iconStyle = V2ProfileMenuIconStyle.LOCK_SHIELD,
        )
    }
}

@Composable
private fun GuestSupportRow(
    icon: ImageVector,
    background: Color,
    title: String,
    subtitle: String,
    showDivider: Boolean,
    action: () -> Unit,
    iconStyle: V2ProfileMenuIconStyle = V2ProfileMenuIconStyle.STANDARD,
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = action)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            V2ProfileMenuIconTile(icon = icon, background = background, style = iconStyle)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = DSColors.shadowGray, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = DSColors.shadowGray.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 16.dp + 40.dp + 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF0F7F4)),
            )
        }
    }
}

@Composable
private fun GuestImageTile(
    @DrawableRes imageRes: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(100.dp).clip(RoundedCornerShape(16.dp))) {
        Image(painterResource(imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.62f)))))
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun GuestPrimaryButton(text: String, icon: ImageVector, action: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50)).background(DSColors.deepGreen).clickable(onClick = action),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun GuestSectionHeader(title: String, subtitle: String, icon: ImageVector, prominent: Boolean = false) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Box(
            Modifier.size(if (prominent) 48.dp else 34.dp).clip(if (prominent) RoundedCornerShape(16.dp) else CircleShape)
                .background(if (prominent) Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen)) else Brush.linearGradient(listOf(DSColors.mistGreen, DSColors.mistGreen))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (prominent) Color.White else DSColors.fairway, modifier = Modifier.size(if (prominent) 21.dp else 17.dp))
        }
    }
}

private fun Modifier.guestSurface(radius: Int = 20): Modifier =
    shadow(8.dp, RoundedCornerShape(radius.dp), ambientColor = DSColors.fairway.copy(alpha = 0.08f), spotColor = DSColors.fairway.copy(alpha = 0.08f))
        .clip(RoundedCornerShape(radius.dp))
        .background(DSColors.cardWhite)
