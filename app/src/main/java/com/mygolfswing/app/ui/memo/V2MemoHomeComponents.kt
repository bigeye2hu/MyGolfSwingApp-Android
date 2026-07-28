package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2MemoHomeHeader(totalCount: Int, onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 52.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Description, null, tint = DSColors.inkGreen, modifier = Modifier.size(20.dp))
                Text("记录 Memo", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            }
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("我的训练记录 ·", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
                Text("${totalCount}条", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(50), spotColor = DSColors.fairway.copy(0.25f))
                .clip(RoundedCornerShape(50))
                .background(DSColors.fairway)
                .clickable(onClick = onCreate)
                .padding(horizontal = 18.dp)
                .widthIn(min = 132.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(11.dp))
            Text("新建 Memo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun V2MemoHomeSectionHeader(
    icon: ImageVector,
    title: String,
    visibleCount: Int,
    totalCount: Int,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(width = 16.dp, height = 14.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            Text(
                "$visibleCount / $totalCount",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.clickable(onClick = onMore),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("更多", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}

enum class V2MemoSectionEmptyKind { MIXED, TEXT, SWING }

@Composable
fun V2MemoSectionEmptyCard(kind: V2MemoSectionEmptyKind, onTap: () -> Unit) {
    val (icon, title, subtitle, cta, colors) = when (kind) {
        V2MemoSectionEmptyKind.MIXED -> Quintuple(
            Icons.Filled.Movie,
            "还没有视频笔记",
            "拍一段挥杆视频，AI 帮你分析动作",
            "去录制 →",
            listOf(Color(0xFFE8E3D6), Color(0xFFEDE9DE), Color(0xFFE4DFD2)),
        )
        V2MemoSectionEmptyKind.TEXT -> Quintuple(
            Icons.AutoMirrored.Filled.Note,
            "还没有随手记",
            "打完球随手写下感受和心得",
            "写下第一条 →",
            listOf(Color(0xFFECE6D8), Color(0xFFF0EAD9), Color(0xFFE8E2D4)),
        )
        V2MemoSectionEmptyKind.SWING -> Quintuple(
            Icons.Filled.Videocam,
            "还没有挥杆记录",
            "用手表或手机录制你的挥杆动作",
            "去录制 →",
            listOf(Color(0xFFE4E0D3), Color(0xFFEAE5D8), Color(0xFFE0DBD0)),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.07f),
                spotColor = DSColors.fairway.copy(alpha = 0.07f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color(0xFFCFC9BA), RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.shadowGray.copy(0.55f), modifier = Modifier.size(26.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF9CA3AF), textAlign = TextAlign.Center)
        Text(cta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway, modifier = Modifier.padding(top = 8.dp))
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@Composable
fun V2MemoHomeAIInsightCard(
    memoSampleCount: Int,
    statusSummary: String?,
    currentFocus: String?,
    painPoint: String?,
    trainingGoal: String?,
    onContinueChat: () -> Unit,
) {
    val hasInsightContext =
        !statusSummary.isNullOrBlank() ||
            !currentFocus.isNullOrBlank() ||
            !painPoint.isNullOrBlank() ||
            !trainingGoal.isNullOrBlank()
    val empty = memoSampleCount == 0 && !hasInsightContext
    val title = when {
        empty -> "还没有洞察内容"
        !painPoint.isNullOrBlank() -> "本周值得关注"
        hasInsightContext -> "今天的关键点"
        else -> "开始记录你的训练"
    }
    val bodySegments = insightBodySegments(
        empty = empty,
        statusSummary = statusSummary,
        currentFocus = currentFocus,
        painPoint = painPoint,
        trainingGoal = trainingGoal,
    )
    val body = buildAnnotatedString {
        bodySegments.forEach { segment ->
            withStyle(
                SpanStyle(
                    color = if (segment.emphasis) DSColors.fairway else Color(0xFF6C757D),
                    fontWeight = if (segment.emphasis) FontWeight.SemiBold else FontWeight.Normal,
                ),
            ) {
                append(segment.text)
            }
        }
    }
    val sourceHint = when {
        empty -> "📊 暂无数据"
        memoSampleCount > 0 -> "📊 基于最近 ${minOf(memoSampleCount, 7)} 条 Memo"
        else -> "📊 暂无 Memo 样本"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg)
            .heightIn(min = 190.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.08f),
                spotColor = DSColors.fairway.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFD8E2DC), DSColors.warmWhite)))
            .border(1.dp, DSColors.goldBright.copy(0.08f), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(DSColors.goldBright.copy(alpha = 0.18f), Color.Transparent))),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 54.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE9DCC2)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = DSColors.goldBright, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 教练 · 我的 Memo 洞察",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                    letterSpacing = 0.6.sp,
                )
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                sourceHint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(enabled = !empty, onClick = onContinueChat),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (empty) "记录后解锁" else "继续聊聊",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
    }
}

private data class InsightTextSegment(
    val text: String,
    val emphasis: Boolean = false,
)

private fun insightBodySegments(
    empty: Boolean,
    statusSummary: String?,
    currentFocus: String?,
    painPoint: String?,
    trainingGoal: String?,
): List<InsightTextSegment> {
    if (empty) {
        return listOf(InsightTextSegment("记录你的第一条 Memo 后，AI 教练将为你生成专属训练分析。"))
    }

    val summary = statusSummary?.trim().orEmpty()
    if (summary.isNotEmpty()) {
        val keywords = listOfNotNull(currentFocus, painPoint, trainingGoal)
            .map { it.trim() }
            .filter { it.length in 2..10 }
            .distinct()
            .sortedByDescending { it.length }
        return tokenizeInsightSummary(summary, keywords)
    }

    if (!currentFocus.isNullOrBlank()) {
        return buildList {
            add(InsightTextSegment("最近你在 "))
            add(InsightTextSegment(currentFocus, emphasis = true))
            add(InsightTextSegment(" 上花了不少功夫"))
            if (!painPoint.isNullOrBlank()) {
                add(InsightTextSegment("，重点注意 "))
                add(InsightTextSegment(painPoint, emphasis = true))
                add(InsightTextSegment(" 这个老毛病"))
            }
            if (!trainingGoal.isNullOrBlank()) {
                add(InsightTextSegment("，继续朝 "))
                add(InsightTextSegment(trainingGoal, emphasis = true))
                add(InsightTextSegment(" 稳定推进"))
            }
            add(InsightTextSegment("。"))
        }
    }

    return listOf(
        InsightTextSegment("还没有看到足够的练习记录。试着记录一段 "),
        InsightTextSegment("挥杆视频", emphasis = true),
        InsightTextSegment(" 或一条 "),
        InsightTextSegment("随手心得", emphasis = true),
        InsightTextSegment("，让 AI 教练给你画一张专属技术档案。"),
    )
}

private fun tokenizeInsightSummary(text: String, keywords: List<String>): List<InsightTextSegment> {
    if (keywords.isEmpty()) return listOf(InsightTextSegment(text))

    val segments = mutableListOf<InsightTextSegment>()
    var cursor = 0
    while (cursor < text.length) {
        val match = keywords
            .mapNotNull { keyword ->
                val index = text.indexOf(keyword, startIndex = cursor)
                if (index >= 0) keyword to index else null
            }
            .minWithOrNull(compareBy<Pair<String, Int>> { it.second }.thenByDescending { it.first.length })

        if (match == null) {
            segments += InsightTextSegment(text.substring(cursor))
            break
        }

        val (keyword, index) = match
        if (index > cursor) {
            segments += InsightTextSegment(text.substring(cursor, index))
        }
        segments += InsightTextSegment(keyword, emphasis = true)
        cursor = index + keyword.length
    }

    return segments.ifEmpty { listOf(InsightTextSegment(text)) }
}

@Composable
fun V2MemoHomeFooter(totalCount: Int, onViewAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DSColors.mistGreen.copy(0.12f), DSColors.warmWhite)))
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = DSColors.fairway.copy(alpha = 0.15f),
                    spotColor = DSColors.fairway.copy(alpha = 0.15f),
                )
                .clip(RoundedCornerShape(50))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.fairway, RoundedCornerShape(50))
                .clickable(onClick = onViewAll)
                .height(52.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Layers, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(8.dp))
            Text("查看全部 Memo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            Spacer(Modifier.width(8.dp))
            Text(
                "${totalCount}条",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.fairway,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.mistGreen)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(12.dp).height(1.dp).clip(CircleShape).background(DSColors.goldBright.copy(0.3f)))
            Text(
                buildAnnotatedString {
                    append("— 你已经记录了 ")
                    withStyle(SpanStyle(color = DSColors.fairway, fontWeight = FontWeight.SemiBold)) {
                        append("${totalCount}次")
                    }
                    append(" 训练 —")
                },
                fontSize = 11.5.sp,
                color = Color(0xFF6C757D),
            )
            Box(Modifier.width(12.dp).height(1.dp).clip(CircleShape).background(DSColors.goldBright.copy(0.3f)))
        }
    }
}

/** Section 图标预设 */
object V2MemoHomeIcons {
    val videoNotes = Icons.AutoMirrored.Filled.Article
    val textNotes = Icons.Filled.ChatBubble
    val swingRecords = Icons.Filled.SmartDisplay
}
