package com.mygolfswing.app.ui.coach

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun coachHomeGreeting(hour: Int): String = when (hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

internal fun coachHomeDisplayName(session: V2Session): String {
    val raw = session.nickname?.trim().takeUnless { it.isNullOrEmpty() }
        ?: session.username?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "教练"
    return if (raw.endsWith("教练")) raw else "${raw}教练"
}

internal fun coachHomeStatusText(questionCount: Int, newCount: Int): String =
    if (questionCount <= 0 && newCount <= 0) "今天暂无新动态"
    else "今天 ${questionCount.coerceAtLeast(0)}条提问 · ${newCount.coerceAtLeast(0)}条新动态"

internal fun coachHomeFormattedDate(now: ZonedDateTime): String =
    now.format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA))

@Composable
internal fun V2CoachHomeHero(
    session: V2Session,
    questionCount: Int,
    newCount: Int,
    sentUnreadCount: Int,
    now: ZonedDateTime = ZonedDateTime.now(),
    onNotice: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenStudents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasNotice = questionCount > 0 || newCount > 0
    V2PageArtworkBackground(
        artwork = V2PageArtwork.GOLF_BAG,
        tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
        modifier = modifier.fillMaxWidth().height(310.dp).testTag("coach-home-hero"),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box {
                        V2UserAvatar(
                            session.avatar,
                            Modifier.size(64.dp).border(2.dp, Color.White, CircleShape),
                            fallbackFontSize = 20.sp,
                            placeholderIconSize = 34.dp,
                            fallbackText = session.nickname ?: session.username ?: "教练",
                            fallbackColor = DSColors.sandBeige,
                        )
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(14.dp)
                                .border(2.dp, DSColors.warmWhite, CircleShape)
                                .clip(CircleShape).background(Color(0xFF3BB273)),
                        )
                    }
                    Column(
                        Modifier.weight(1f).padding(start = 10.dp, end = 52.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${coachHomeGreeting(now.hour)}，${coachHomeDisplayName(session)}",
                                    color = DSColors.inkGreen,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Text("👏", fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                            Text(
                                "专注每一次挥杆的进步",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().height(36.dp).clip(CircleShape)
                                .background(DSColors.cardWhite.copy(alpha = 0.92f))
                                .border(1.dp, Color.White.copy(alpha = 0.70f), CircleShape)
                                .clickable(role = Role.Button, onClick = onOpenReview)
                                .padding(horizontal = 9.dp).testTag("coach-home-today-status"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(DSColors.mistGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
                            }
                            Text(
                                coachHomeFormattedDate(now),
                                color = DSColors.inkGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Box(Modifier.size(width = 1.dp, height = 14.dp).background(DSColors.shadowGray.copy(alpha = 0.26f)))
                            Text(
                                coachHomeStatusText(questionCount, newCount),
                                color = DSColors.fairway,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = DSColors.shadowGray,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).size(44.dp)
                        .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f))
                        .clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.9f))
                        .clickable(role = Role.Button, onClick = onNotice).testTag("coach-home-notice"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Notifications, "今日提醒", tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                    if (hasNotice) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 6.dp).size(8.dp)
                                .clip(CircleShape).background(DSColors.flagRed),
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.10f))
                    .clip(RoundedCornerShape(22.dp)).background(DSColors.cardWhite.copy(alpha = 0.78f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(DSColors.trophyGold.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.QueryStats, null, tint = DSColors.trophyGold, modifier = Modifier.size(16.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("今日总览", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("学员的进步，就是最好的回报", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "查看全部",
                        color = DSColors.fairway,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onOpenReview).padding(vertical = 6.dp),
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoachOverviewMetric(Icons.Filled.ChatBubble, DSColors.fairway, questionCount, "待跟进提问", Modifier.weight(1f), onOpenReview)
                    CoachOverviewMetric(Icons.Filled.PlayCircle, DSColors.fairway, newCount, "学员新动态", Modifier.weight(1f), onOpenReview)
                    CoachOverviewMetric(Icons.AutoMirrored.Filled.Send, DSColors.trophyGold, sentUnreadCount, "已发送待查看", Modifier.weight(1f), onOpenStudents)
                }
            }
        }
    }
}

@Composable
private fun CoachOverviewMetric(
    icon: ImageVector,
    tint: Color,
    value: Int,
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.height(78.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(27.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(value.coerceAtLeast(0).toString(), color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
internal fun V2CoachHomeSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionTitle: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        Text(title, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        subtitle?.let {
            Text(
                it,
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        } ?: Spacer(Modifier.weight(1f))
        actionTitle?.let {
            Text(
                it,
                color = DSColors.fairway,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(vertical = 7.dp),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
internal fun V2CoachHomePriorityEmpty(
    hasStudents: Boolean,
    onInvite: () -> Unit,
    onOpenStudents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (hasStudents) "今天的内容已处理完成" else "先连接一位学员"
    val subtitle = if (hasStudents) "可以进入学员空间主动发一条消息" else "连接后，新视频和消息会出现在这里"
    Row(
        modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = if (hasStudents) onOpenStudents else onInvite)
            .padding(14.dp).testTag("coach-home-priority-empty"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(
                if (hasStudents) Icons.Filled.CheckCircle else Icons.Filled.PersonAdd,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
    }
}

@Composable
internal fun V2CoachHomeInviteFirstStudent(onInvite: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onInvite).padding(horizontal = 18.dp)
            .testTag("coach-home-invite-first-student"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.PersonAdd, null, tint = DSColors.fairway, modifier = Modifier.size(17.dp))
        Text(
            "邀请第一位学员",
            color = DSColors.fairway,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}
