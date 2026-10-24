package com.mygolfswing.app.ui.interaction

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.R
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachLessonMode
import com.mygolfswing.app.data.coach.V2CoachStudentAge
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.coach.CoachGreen
import com.mygolfswing.app.ui.coach.CoachPaper
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class TeachingSpaceDayGroup(
    val key: String,
    val primary: String,
    val secondary: String?,
    val items: List<V2CoachWorkspaceItem>,
)

internal fun teachingSpaceAsksForReview(item: V2CoachWorkspaceItem): Boolean =
    item.requiresCoachReview == true &&
        item.reviewState != "responded" &&
        item.senderRole != "coach" &&
        item.resolvedKind != "coach_post"

internal fun teachingSpaceHasFreshReply(item: V2CoachWorkspaceItem, coachPerspective: Boolean): Boolean =
    if (coachPerspective) {
        item.latestReplySenderRole == "student" && item.reviewState != "responded"
    } else {
        item.studentHasUnread && item.latestReplySenderRole == "coach"
    }

internal fun teachingSpaceRowStatus(item: V2CoachWorkspaceItem, coachPerspective: Boolean): String? = when {
    coachPerspective && teachingSpaceHasFreshReply(item, coachPerspective = true) -> "有新回复"
    coachPerspective && teachingSpaceAsksForReview(item) -> "求点评"
    !coachPerspective && teachingSpaceHasFreshReply(item, coachPerspective = false) -> "有新反馈"
    else -> null
}

internal fun teachingSpacePublishEpoch(raw: String?): Long {
    val instant = raw?.let { value ->
        runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    } ?: return 0L
    return instant.toEpochMilli()
}

internal fun teachingSpaceDayGroups(
    items: List<V2CoachWorkspaceItem>,
    newestFirst: Boolean,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<TeachingSpaceDayGroup> {
    val today = now.atZone(zoneId).toLocalDate()
    val sorted = items.sortedBy { teachingSpacePublishEpoch(it.createdAt) }
        .let { if (newestFirst) it.asReversed() else it }
    val grouped = linkedMapOf<String, MutableList<V2CoachWorkspaceItem>>()
    val labels = mutableMapOf<String, Pair<String, String?>>()
    sorted.forEach { item ->
        val date = teachingSpaceLocalDate(item.createdAt, zoneId)
        val key = date?.toString() ?: "unknown"
        if (key !in labels) {
            labels[key] = when {
                date == null -> "更早" to null
                date == today -> "今天" to null
                date == today.minusDays(1) -> "昨天" to null
                else -> date.dayOfMonth.toString() to "${date.monthValue}月"
            }
        }
        grouped.getOrPut(key) { mutableListOf() }.add(item)
    }
    return grouped.map { (key, rows) ->
        val (primary, secondary) = labels.getValue(key)
        TeachingSpaceDayGroup(key, primary, secondary, rows)
    }
}

private fun teachingSpaceLocalDate(raw: String?, zoneId: ZoneId): LocalDate? {
    val instant = raw?.let { value ->
        runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    } ?: return null
    return instant.atZone(zoneId).toLocalDate()
}

internal fun teachingSpaceClock(raw: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
    val instant = raw?.let { value ->
        runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    } ?: return null
    return instant.atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
}

internal fun teachingSpaceSourceLine(
    item: V2CoachWorkspaceItem,
    coachPerspective: Boolean,
    peerName: String,
): String {
    val fromCoach = item.senderRole == "coach" || item.resolvedKind == "coach_post"
    val clock = teachingSpaceClock(item.createdAt)
    return if (coachPerspective) {
        if (fromCoach) "我发的 · 教学视频" else listOfNotNull("学员练习", clock).joinToString(" · ")
    } else if (fromCoach) {
        "${peerName.ifBlank { "教练" }} · 教学视频"
    } else {
        listOfNotNull("我的练习", clock).joinToString(" · ")
    }
}

internal fun teachingSpaceRelationLine(connection: V2CoachConnection, coachPerspective: Boolean): String? {
    if (!coachPerspective) return "我的教练"
    val mode = connection.lessonMode?.takeIf { it.isNotBlank() }?.let { wire ->
        V2CoachLessonMode.entries.firstOrNull { it.wireValue == wire }?.title
    }
    val age = V2CoachStudentAge.entries.firstOrNull { it.wireValue == connection.ageTag }?.title
    return listOfNotNull(mode, age).joinToString(" · ").ifBlank { null }
}

@Composable
internal fun TeachingSpaceHeader(
    connection: V2CoachConnection,
    name: String,
    avatar: String?,
    coachPerspective: Boolean,
    active: Boolean,
    busy: Boolean,
    pinnedStudent: Boolean,
    canSwitch: Boolean,
    onBack: () -> Unit,
    onSwitch: () -> Unit,
    onEditProfile: (() -> Unit)?,
    onTogglePinnedStudent: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val focus = connection.resolvedTrainingFocus?.trim()?.takeIf { it.isNotEmpty() }
    val relation = teachingSpaceRelationLine(connection, coachPerspective)
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("workspace-header-artwork")) {
        Box(Modifier.fillMaxWidth().height(if (coachPerspective) 168.dp else 214.dp)) {
            Image(
                painter = painterResource(R.drawable.space_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.62f to Color.Black.copy(alpha = 0.12f),
                        1f to CoachPaper,
                    ),
                ),
            )
            Column(
                Modifier.fillMaxWidth().then(
                    if (coachPerspective) Modifier.v2StatusBarPadding() else Modifier,
                ),
            ) {
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    Text("教学空间", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(34.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.22f))
                            .clickable(onClick = onBack).testTag("workspace-top-back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    if (active) {
                        Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))
                                    .clickable(enabled = !busy) { menuExpanded = true }.testTag("workspace-top-more"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.MoreHoriz, "更多", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                if (!coachPerspective && canSwitch) {
                                    DropdownMenuItem(
                                        text = { Text("切换教练") },
                                        onClick = { menuExpanded = false; onSwitch() },
                                    )
                                }
                                onEditProfile?.let { editProfile ->
                                    DropdownMenuItem(
                                        text = { Text("教学档案") },
                                        onClick = { menuExpanded = false; editProfile() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (pinnedStudent) "取消重点学员" else "设为重点学员") },
                                        onClick = { menuExpanded = false; onTogglePinnedStudent() },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("解除连接", color = DSColors.flagRed) },
                                    onClick = { menuExpanded = false; onDisconnect() },
                                )
                            }
                        }
                    }
                }
                if (coachPerspective) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        V2UserAvatar(avatar, Modifier.size(44.dp), fallbackText = name, fallbackFontSize = 16.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            relation?.let {
                                Text(it, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        Text(
                            "切换学员",
                            color = CoachGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(Color.White)
                                .clickable(enabled = !busy, onClick = onSwitch)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("workspace-top-switcher"),
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                name,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                relation.orEmpty(),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                            )
                        }
                        V2UserAvatar(avatar, Modifier.size(64.dp), fallbackText = name, fallbackFontSize = 22.sp)
                    }
                }
            }
        }
        if (focus != null || onEditProfile != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (focus != null) {
                    Text("当前重点", color = DSColors.shadowGray, fontSize = 13.sp)
                    Text(
                        focus,
                        color = CoachGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                onEditProfile?.let { openProfile ->
                    Text(
                        "教学档案  ›",
                        color = CoachGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = openProfile).padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun TeachingSpaceFilterBar(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("all" to "全部", "review" to "求点评", "fresh" to "新互动").forEach { (value, label) ->
            val chosen = selected == value
            Text(
                label,
                color = if (chosen) Color.White else CoachGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape)
                    .background(if (chosen) CoachGreen else Color.White)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("teaching-filter-$value"),
            )
        }
    }
}

internal fun LazyListScope.teachingSpaceFeed(
    groups: List<TeachingSpaceDayGroup>,
    coachPerspective: Boolean,
    peerName: String,
    newestFirst: Boolean,
    onToggleSort: () -> Unit,
    preparingItemId: String?,
    onOpen: (V2CoachWorkspaceItem) -> Unit,
    onOpenFresh: (V2CoachWorkspaceItem) -> Unit,
    onPlay: (V2CoachWorkspaceItem) -> Unit,
) {
    item(key = "teaching-section") {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("练习与指导", color = CoachGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                if (newestFirst) "最新发布" else "最早发布",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onToggleSort).padding(4.dp),
            )
        }
    }
    groups.forEach { group ->
        group.items.forEachIndexed { index, item ->
            item(key = item.itemId) {
                TeachingSpaceRow(
                    item = item,
                    dayPrimary = if (index == 0) group.primary else null,
                    daySecondary = if (index == 0) group.secondary else null,
                    coachPerspective = coachPerspective,
                    peerName = peerName,
                    preparing = preparingItemId == item.itemId,
                    onOpen = { onOpen(item) },
                    onOpenFresh = { onOpenFresh(item) },
                    onPlay = { onPlay(item) },
                )
            }
        }
    }
}

@Composable
private fun TeachingSpaceRow(
    item: V2CoachWorkspaceItem,
    dayPrimary: String?,
    daySecondary: String?,
    coachPerspective: Boolean,
    peerName: String,
    preparing: Boolean,
    onOpen: () -> Unit,
    onOpenFresh: () -> Unit,
    onPlay: () -> Unit,
) {
    val fromCoach = item.senderRole == "coach" || item.resolvedKind == "coach_post"
    val title = V2CoachWorkspaceItem.visibleCopy(item.title)
        ?: if (fromCoach) "教学视频" else if (item.hasVideoContent) "练习视频" else "练习记录"
    val note = V2CoachWorkspaceItem.visibleCopy(item.text)?.takeIf { it != title }
    val status = teachingSpaceRowStatus(item, coachPerspective)
    val cover = item.primaryAsset?.coverUrl ?: item.coverUrl
    val duration = item.primaryAsset?.durationSeconds ?: item.durationSeconds
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("teaching-space-row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(42.dp).padding(top = 2.dp)) {
            if (dayPrimary != null) {
                Column {
                    Text(
                        dayPrimary,
                        color = CoachGreen,
                        fontSize = if (daySecondary == null) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                    )
                    daySecondary?.let {
                        Text(it, color = DSColors.shadowGray, fontSize = 11.sp)
                    }
                }
            }
        }
        Box(
            Modifier.size(width = 74.dp, height = 96.dp).clip(RoundedCornerShape(10.dp))
                .background(DSColors.mistGreen)
                .clickable(onClick = onPlay)
                .testTag("teaching-space-cover"),
        ) {
            if (cover != null) {
                AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Icon(
                Icons.Filled.PlayArrow,
                if (preparing) "正在打开" else "播放",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
            )
            duration?.takeIf { it.isFinite() && it > 0 }?.let {
                Text(
                    teachingSpaceDuration(it),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Column(
            Modifier.weight(1f).clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                title,
                color = DSColors.inkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            note?.let {
                Text(
                    it,
                    color = DSColors.inkGreen.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                teachingSpaceSourceLine(item, coachPerspective, peerName),
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            status?.let { label ->
                val review = label == "求点评"
                Text(
                    if (review) label else "● $label",
                    color = if (review) Color(0xFF8A6232) else CoachGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(
                            if (review) {
                                Modifier.clip(CircleShape).background(Color(0xFFF3E6D0)).padding(horizontal = 8.dp, vertical = 3.dp)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(onClick = onOpenFresh),
                )
            }
        }
    }
}

@Composable
internal fun TeachingSpaceEmptyNote() {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp).testTag("teaching-space-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.room_bg_2),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(96.dp),
        )
        Text("挥杆视频和练习记录会留在这里", color = DSColors.shadowGray, fontSize = 13.sp)
    }
}

@Composable
internal fun TeachingSpaceLoadFailure(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp).testTag("teaching-space-load-failure"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("教学内容暂时没有读出来", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "重试",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).background(CoachGreen).clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

private fun teachingSpaceDuration(seconds: Double): String {
    val value = seconds.roundToInt().coerceAtLeast(0)
    return "%d:%02d".format(value / 60, value % 60)
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
