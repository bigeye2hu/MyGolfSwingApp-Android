package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
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
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** iOS `V2MemoMockData` 的只读游客样例；不会写入账号数据库。 */
internal object V2GuestMemoSamples {
    private fun date(daysAgo: Long, hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.now().minusDays(daysAgo).withHour(hour).withMinute(minute).withSecond(0).withNano(0)

    val mixed: List<V2Memo> = listOf(
        V2Memo(
            id = UUID.fromString("71c4b7ca-b442-4a78-84f5-583461113101"),
            kind = V2MemoKind.MIXED,
            source = V2MemoSource.SELF_RECORD,
            videoUrl = "mock://memo/mixed/01",
            durationSec = 134.0,
            text = "切球落点控制，手腕翻转时机，50码距离练习",
            topic = V2Topic.SWING,
            tags = listOf("7号铁"),
            createdAt = date(0, 14, 32),
            updatedAt = date(0, 14, 32),
            isNew = true,
            accent = V2MemoAccent.BREAKTHROUGH,
            displayTitle = "下杆节奏练习 · 7号铁",
            metaText = "今天 14:32",
            thumbnailKind = V2MemoThumbnailKind.FAIRWAY,
            trainingMethod = "拍摄",
        ),
        V2Memo(
            id = UUID.fromString("71c4b7ca-b442-4a78-84f5-583461113102"),
            kind = V2MemoKind.MIXED,
            source = V2MemoSource.SELF_RECORD,
            videoUrl = "mock://memo/mixed/02",
            durationSec = 188.0,
            text = "不同距离的推杆力度控制，3米内进洞率提升",
            topic = V2Topic.PUTTING,
            tags = listOf("推杆"),
            createdAt = date(1, 9, 15),
            updatedAt = date(1, 9, 15),
            displayTitle = "果岭推杆训练 · 速度感知",
            metaText = "昨天 09:15",
            thumbnailKind = V2MemoThumbnailKind.GREEN_GRASS,
            trainingMethod = "拍摄",
        ),
        V2Memo(
            id = UUID.fromString("71c4b7ca-b442-4a78-84f5-583461113103"),
            kind = V2MemoKind.MIXED,
            source = V2MemoSource.SELF_RECORD,
            videoUrl = "mock://memo/mixed/03",
            durationSec = 262.0,
            text = "背部旋转角度调整，肩线平行球道",
            topic = V2Topic.SWING,
            tags = listOf("开球"),
            createdAt = date(2, 16, 40),
            updatedAt = date(2, 16, 40),
            displayTitle = "开球姿势校正练习",
            metaText = "前天 16:40",
            thumbnailKind = V2MemoThumbnailKind.SUNSET,
            trainingMethod = "拍摄",
        ),
    )

    val text: List<V2Memo> = listOf(
        textMemo(
            id = "71c4b7ca-b442-4a78-84f5-583461113201",
            body = "击球前的静默 2 秒，比挥速更重要。",
            subText = "保持内心安静，专注呼吸，让身体进入最佳击球状态。",
            topic = V2Topic.MIND,
            tags = listOf("心法"),
            accent = V2MemoAccent.INSIGHT,
            meta = "今天 09:00",
            createdAt = date(0, 9, 0),
            isNew = true,
        ),
        textMemo(
            id = "71c4b7ca-b442-4a78-84f5-583461113202",
            body = "果岭速度比距离更难判断，今天踩了3次。",
            subText = "下次要先观察果岭坡度，再决定力度。",
            topic = V2Topic.PUTTING,
            tags = listOf("反思"),
            accent = V2MemoAccent.REFLECTION,
            meta = "昨天 16:20",
            createdAt = date(1, 16, 20),
        ),
        textMemo(
            id = "71c4b7ca-b442-4a78-84f5-583461113203",
            body = "教练说：放松手腕，让杆头自己工作。",
            subText = "不要硬拉，保持手指轻握，让动作流畅。",
            topic = V2Topic.SWING,
            tags = emptyList(),
            accent = null,
            meta = "前天 11:30",
            createdAt = date(2, 11, 30),
        ),
    )

    val all: List<V2Memo> get() = (mixed + text).sortedByDescending { it.createdAt }

    val swingDay: V2SwingDayItem
        get() {
            val today = LocalDate.now()
            return V2SwingDayItem(
                id = UUID.fromString("71c4b7ca-b442-4a78-84f5-583461113301"),
                day = today,
                dayNumber = today.dayOfMonth.toString(),
                monthLabel = "${today.monthValue}月",
                relativeText = "今天",
                title = "4 个挥杆 · 9 分钟",
                location = "练习场",
                timeRange = "18:20 - 18:29",
                clipCount = 4,
                extraClipCount = 1,
            )
        }

    private fun textMemo(
        id: String,
        body: String,
        subText: String,
        topic: V2Topic,
        tags: List<String>,
        accent: V2MemoAccent?,
        meta: String,
        createdAt: LocalDateTime,
        isNew: Boolean = false,
    ) = V2Memo(
        id = UUID.fromString(id),
        kind = V2MemoKind.TEXT,
        source = V2MemoSource.TEXT_INPUT,
        text = body,
        title = body,
        topic = topic,
        tags = tags,
        createdAt = createdAt,
        updatedAt = createdAt,
        isNew = isNew,
        accent = accent,
        displayTitle = body,
        metaText = meta,
        subText = subText,
    )
}

/** 游客点击示例 Memo 后的只读预览，对齐 iOS `V2GuestMemoPreviewSheet`。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2GuestMemoPreviewSheet(
    memo: V2Memo,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        scrimColor = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .v2NavigationBarPadding(),
        ) {
            GuestMemoHeader(onDismiss)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { GuestMemoHero(memo) }
                item { GuestMemoStructure(memo) }
                item { GuestMemoContent(memo) }
                item { GuestMemoBoundary() }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.deepGreen)
                    .clickable {
                        onLogin()
                        onDismiss()
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GuestMemoLoginIcon()
                Text(
                    "登录/注册后开始记录",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun GuestMemoHeader(onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Memo 预览", color = DSColors.inkGreen, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text("游客只读 · 登录后保存到账号", color = DSColors.shadowGray, fontSize = 12.sp)
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(DSColors.mistGreen).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "关闭", tint = DSColors.inkGreen, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun GuestMemoHero(memo: V2Memo) {
    val title = memo.displayTitle?.takeIf { it.isNotBlank() }
        ?: memo.text?.take(24)?.takeIf { it.isNotBlank() }
        ?: if (memo.hasVideo) "示例视频 Memo" else "示例文字 Memo"
    Box(
        Modifier.fillMaxWidth().height(160.dp).shadow(14.dp, RoundedCornerShape(22.dp), spotColor = DSColors.deepGreen.copy(0.10f))
            .clip(RoundedCornerShape(22.dp)),
    ) {
        Image(
            painterResource(if (memo.hasVideo) R.drawable.v2_quickadd_video else R.drawable.v2_quickadd_text),
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        guestMemoPreviewColor(memo).copy(alpha = 0.16f),
                        Color.Black.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.66f),
                    ),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(DSColors.deepGreen.copy(alpha = 0.72f), Color.Transparent),
                ),
            ),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))),
            ),
        )
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (memo.hasVideo) "视频·笔记" else "文字",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(0.42f)).padding(horizontal = 9.dp, vertical = 4.dp),
                )
                memo.durationSec?.let {
                    val total = it.toInt()
                    Text("${total / 60}:${(total % 60).toString().padStart(2, '0')}", color = Color.White, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(0.42f)).padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                memo.metaText ?: V2Format.timelineLabel(memo.createdAt),
                color = Color.White.copy(0.78f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        GuestMemoVisualStack(memo, Modifier.align(Alignment.CenterEnd).padding(end = 24.dp, top = 2.dp))
    }
}

@Composable
private fun GuestMemoVisualStack(memo: V2Memo, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 124.dp, height = 114.dp)) {
        Box(
            Modifier.size(width = 118.dp, height = 96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(
                    if (memo.hasVideo) Icons.Filled.SmartDisplay else Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    if (memo.hasVideo) "视频片段" else "文字复盘",
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp)
                .width(98.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f)).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            GuestMemoVisualMiniRow("动作要点")
            GuestMemoVisualMiniRow("练习目标")
        }
    }
}

@Composable
private fun GuestMemoVisualMiniRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(DSColors.fairway))
        Text(text, color = DSColors.inkGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun GuestMemoLoginIcon() {
    Box(Modifier.size(18.dp)) {
        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(15.dp).align(Alignment.TopStart))
        Icon(Icons.Filled.Key, null, tint = Color.White, modifier = Modifier.size(8.dp).align(Alignment.BottomEnd))
    }
}

@Composable
private fun GuestMemoBoundaryIcon() {
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Shield, null, tint = DSColors.fairway, modifier = Modifier.fillMaxSize())
        Icon(Icons.Filled.Lock, null, tint = DSColors.mistGreen, modifier = Modifier.size(7.dp))
    }
}

private fun guestMemoPreviewColor(memo: V2Memo): Color = when (memo.thumbnailKind) {
    V2MemoThumbnailKind.FAIRWAY -> DSColors.fairway
    V2MemoThumbnailKind.SAND -> Color(0xFFB88A43)
    V2MemoThumbnailKind.GREEN_GRASS -> Color(0xFF6FAE75)
    V2MemoThumbnailKind.SUNSET -> Color(0xFFD99A4E)
    V2MemoThumbnailKind.MIST -> Color(0xFF718E78)
    V2MemoThumbnailKind.FOREST -> Color(0xFF335B46)
}

@Composable
private fun GuestMemoStructure(memo: V2Memo) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        GuestMemoStructurePill(if (memo.hasVideo) Icons.Filled.Videocam else Icons.Filled.Description, if (memo.hasVideo) "片段" else "想法", if (memo.hasVideo) "回放动作" else "复盘文字", Modifier.weight(1f))
        GuestMemoStructurePill(Icons.Filled.Sell, "重点", memo.tags.firstOrNull() ?: "节奏", Modifier.weight(1f))
        GuestMemoStructurePill(Icons.Filled.Flag, "目标", "登录后关联", Modifier.weight(1f))
    }
}

@Composable
private fun GuestMemoStructurePill(icon: ImageVector, title: String, subtitle: String, modifier: Modifier) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(DSColors.fairway), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GuestMemoContent(memo: V2Memo) {
    val body = memo.text?.takeIf { it.isNotBlank() }
        ?: "视频 Memo 会把挥杆片段、练习重点和标签放在一起，方便之后复盘。"
    Column(
        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp), spotColor = DSColors.fairway.copy(0.06f))
            .clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("笔记内容", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("示例", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).background(DSColors.mistGreen).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text(body, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        if (memo.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                memo.tags.take(3).forEach { tag ->
                    Text(tag, color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).background(DSColors.mistGreen).padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun GuestMemoBoundary() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            GuestMemoBoundaryIcon()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("游客模式不会保存这条内容", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("新建、编辑、保存到 Memo 或关联目标时，会先进入登录/注册。", color = DSColors.shadowGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
