package com.mygolfswing.app.ui.role

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.role.V2ExperienceRole
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

private val RoleHeroTop = Color(0xFF0D2B1F)
private val RoleHeroMid = Color(0xFF1B4332)
private val RoleHeroStudent = Color(0xFF40916C)
private val RoleHeroBottom = Color(0xFF52B788)
private val RoleCoachGold = Color(0xFFF2C55C)
private val RoleCoachGoldDark = Color(0xFFE0A82E)

@Composable
fun V2RoleSelectionScreen(
    session: V2Session,
    allowsDismiss: Boolean,
    activeRole: V2ExperienceRole = V2ExperienceRole.STUDENT,
    onBack: () -> Unit,
    onSelect: (V2ExperienceRole) -> Unit,
) {
    V2BackHandler(enabled = allowsDismiss, onBack = onBack)
    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .v2NavigationBarPadding().testTag("role-selection-screen"),
        ) {
            RoleSelectionHero(allowsDismiss = allowsDismiss, onBack = onBack)
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().offset(y = (-54).dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RoleSelectionCard(
                        session = session,
                        activeRole = activeRole,
                        onSelect = onSelect,
                    )
                    RoleSafetyNote()
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionHero(allowsDismiss: Boolean, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(304.dp)) {
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.00f to RoleHeroTop.copy(alpha = 0.88f),
                    0.46f to RoleHeroMid.copy(alpha = 0.68f),
                    1.00f to DSColors.warmWhite.copy(alpha = 0.82f),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.08f)),
                ),
            ),
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (allowsDismiss) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(onClick = onBack).testTag("role-selection-back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回我的页面",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.size(40.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.height(32.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Filled.Groups, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("双身份账号", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("挥呗", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("一个账号，两套专属体验", color = Color.White.copy(alpha = 0.84f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.height(30.dp).clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(15.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RoleHeroChip(Icons.Filled.SportsGolf, "练习记录")
                    RoleHeroChip(Icons.Filled.Groups, "学员管理")
                    RoleHeroChip(Icons.Filled.SwapHoriz, "随时切换")
                }
            }
        }
    }
}

@Composable
private fun RoleHeroChip(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(10.dp))
        Text(title, color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoleSelectionCard(
    session: V2Session,
    activeRole: V2ExperienceRole,
    onSelect: (V2ExperienceRole) -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.17f), spotColor = Color.Black.copy(alpha = 0.17f))
            .clip(shape).background(Color.White)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.10f), shape)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        RoleAccountHeader(session)
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F0EA)))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("这次想以什么身份进入？", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("只改变界面和操作方式，不会切换账号或影响数据", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            RoleCard(
                role = V2ExperienceRole.STUDENT,
                detail = "继续记录练习、整理 Memo、连接教练和浏览视频",
                icon = Icons.Filled.SportsGolf,
                isCurrent = activeRole == V2ExperienceRole.STUDENT,
                onClick = { onSelect(V2ExperienceRole.STUDENT) },
            )
            RoleCard(
                role = V2ExperienceRole.COACH,
                detail = "进入教练工作台，集中处理学员、点评与消息",
                icon = Icons.Filled.Groups,
                isCurrent = activeRole == V2ExperienceRole.COACH,
                onClick = { onSelect(V2ExperienceRole.COACH) },
            )
        }
    }
}

@Composable
private fun RoleAccountHeader(session: V2Session) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier.size(66.dp).shadow(10.dp, CircleShape, spotColor = DSColors.fairway.copy(alpha = 0.14f))
                .border(2.dp, RoleCoachGold, CircleShape).padding(2.dp),
        ) {
            V2UserAvatar(
                avatar = session.avatar,
                modifier = Modifier.fillMaxSize(),
                fallbackFontSize = 25.sp,
                placeholderIconSize = 35.dp,
                fallbackText = session.nickname ?: session.username,
                fallbackColor = DSColors.sandBeige,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "你好，${session.nickname ?: session.username ?: "高尔夫球友"}",
                color = DSColors.inkGreen,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.height(27.dp).clip(RoundedCornerShape(14.dp))
                    .background(DSColors.mistGreen).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Verified, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
                Text("教练身份已认证", color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: V2ExperienceRole,
    detail: String,
    icon: ImageVector,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val coach = role == V2ExperienceRole.COACH
    val accent = if (coach) RoleCoachGold else RoleHeroStudent
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (isCurrent) {
        if (coach) Brush.linearGradient(listOf(RoleCoachGoldDark, RoleCoachGold))
        else Brush.linearGradient(listOf(Color(0xFF2D6A4F), RoleHeroBottom))
    } else {
        Brush.linearGradient(listOf(DSColors.borderLine, DSColors.borderLine))
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 94.dp)
            .shadow(if (isCurrent) 8.dp else 0.dp, shape, spotColor = accent.copy(alpha = 0.20f))
            .clip(shape)
            .background(
                when {
                    !isCurrent -> Color(0xFFFCFBF8)
                    coach -> Color(0xFFFFFBF1)
                    else -> Color(0xFFF8FBF9)
                },
            )
            .border(BorderStroke(if (isCurrent) 2.5.dp else 1.dp, borderBrush), shape)
            .clickable(onClick = onClick).testTag("role-option-${role.name.lowercase()}")
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                .background(if (coach) RoleCoachGold else DSColors.fairway),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (coach) DSColors.deepGreen else Color.White, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(role.title, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (isCurrent) {
                    Text(
                        "当前",
                        color = if (coach) DSColors.deepGreen else DSColors.fairway,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(20.dp).clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = if (coach) 0.18f else 0.10f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            Text(detail, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
        }
        Text("›", color = if (isCurrent) accent else DSColors.shadowGray.copy(alpha = 0.58f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoleSafetyNote() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Shield, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
        Text(
            "两套界面彼此独立，共用同一账号，切换不会丢失内容",
            color = DSColors.shadowGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
        )
    }
}
