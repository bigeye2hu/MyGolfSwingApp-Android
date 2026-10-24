package com.mygolfswing.app.ui.legal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography

enum class V2LegalDocType(val title: String, val assetPath: String, val updated: String) {
    TERMS("用户服务协议", "legal/terms_of_service.md", "2026年8月31日"),
    PRIVACY("隐私政策", "legal/privacy_policy.md", "2026年8月28日"),
}

/**
 * 法律文档页（对齐 iOS `LegalDocumentView`）。
 * 正文从 assets 读取 Markdown 原文，简单渲染为纯文本。
 */
@Composable
fun V2LegalScreen(type: V2LegalDocType, onBack: () -> Unit) {
    val context = LocalContext.current
    val content = remember(type) { loadAsset(context, type.assetPath) }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LegalTopBar(onClose = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(bottom = 40.dp),
            ) {
                LegalHero(type)
                LegalDocumentContent(
                    blocks = remember(content) { parseLegalDocument(content) },
                    accent = if (type == V2LegalDocType.TERMS) DSColors.fairway else DSColors.deepGreen,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun LegalTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .background(DSColors.warmWhite)
            .padding(horizontal = 16.dp)
            .testTag("legal-top-bar"),
    ) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = "关闭",
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .clickable(onClick = onClose)
                .padding(10.dp),
        )
    }
}

@Composable
private fun LegalHero(type: V2LegalDocType) {
    val accent = if (type == V2LegalDocType.TERMS) DSColors.fairway else DSColors.deepGreen
    val icon = if (type == V2LegalDocType.TERMS) Icons.Filled.Description else Icons.Filled.Lock
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.12f), DSColors.warmWhite),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-80).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.08f)),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp),
        ) {
            LegalIcon(icon = icon, accent = accent)
            Text(
                text = type.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "更新于 ${type.updated}",
                fontSize = 13.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun LegalIcon(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun LegalDocumentContent(
    blocks: List<LegalDocumentBlock>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        blocks.forEach { block ->
            when (block.kind) {
                LegalBlockKind.INTRO -> LegalBodyCard(block.lines, accent)
                LegalBlockKind.WARNING -> LegalWarningBlock(block, accent)
                LegalBlockKind.CHAPTER -> LegalChapterBlock(block, accent)
                LegalBlockKind.CONTACT -> LegalContactBlock(block, accent)
                LegalBlockKind.FOOTER -> LegalFooterBlock(block)
            }
        }
    }
}

@Composable
private fun LegalWarningBlock(block: LegalDocumentBlock, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Warning, null, tint = DSColors.flagRed, modifier = Modifier.size(16.dp))
            Text(block.title, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(DSColors.flagRed.copy(alpha = 0.06f))
                .border(1.dp, DSColors.flagRed.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            block.lines.forEach { LegalBodyLine(it, accent) }
        }
    }
}

@Composable
private fun LegalChapterBlock(block: LegalDocumentBlock, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            block.chapterNumber?.let { number ->
                Text(
                    "第${number}章",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(accent)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(block.title, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        LegalBodyCard(block.lines, accent)
    }
}

@Composable
private fun LegalBodyCard(lines: List<LegalLine>, accent: Color) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = accent.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite).padding(16.dp),
    ) {
        lines.forEach { LegalBodyLine(it, accent) }
    }
}

@Composable
private fun LegalContactBlock(block: LegalDocumentBlock, accent: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Email, null, tint = accent, modifier = Modifier.size(14.dp))
            Text("联系我们", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        block.lines.forEach { LegalBodyLine(it, accent) }
    }
}

@Composable
private fun LegalFooterBlock(block: LegalDocumentBlock) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        block.lines.filter { it.kind != LegalLineKind.SPACER }.forEachIndexed { index, line ->
            Text(
                line.text,
                color = DSColors.shadowGray.copy(alpha = if (index == 0) 1f else 0.7f),
                fontSize = if (index == 0) 14.sp else 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LegalBodyLine(line: LegalLine, accent: Color) {
    when (line.kind) {
        LegalLineKind.SPACER -> Spacer(Modifier.height(8.dp))
        LegalLineKind.SUBSECTION -> Text(
            line.text,
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 23.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 5.dp),
        )
        LegalLineKind.EMPHASIS -> Text(
            line.text,
            style = DSTypography.bodyBold,
            color = DSColors.inkGreen.copy(alpha = 0.90f),
            lineHeight = 23.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        LegalLineKind.BULLET -> Text(
            "• ${line.text}",
            style = DSTypography.body,
            color = DSColors.inkGreen.copy(alpha = 0.85f),
            lineHeight = 23.sp,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        LegalLineKind.TABLE -> Text(
            line.text,
            style = DSTypography.caption,
            color = DSColors.inkGreen.copy(alpha = 0.78f),
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp)).background(DSColors.listMutedBg)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
        LegalLineKind.BODY -> Text(
            line.text,
            style = DSTypography.body,
            color = DSColors.inkGreen.copy(alpha = 0.85f),
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
    }
}

internal data class LegalDocumentBlock(
    val kind: LegalBlockKind,
    val title: String = "",
    val chapterNumber: String? = null,
    val lines: List<LegalLine> = emptyList(),
)

internal enum class LegalBlockKind { INTRO, WARNING, CHAPTER, CONTACT, FOOTER }

internal data class LegalLine(val kind: LegalLineKind, val text: String = "")

internal enum class LegalLineKind { SUBSECTION, EMPHASIS, BULLET, TABLE, BODY, SPACER }

internal fun parseLegalDocument(raw: String): List<LegalDocumentBlock> {
    val blocks = mutableListOf<LegalDocumentBlock>()
    var kind = LegalBlockKind.INTRO
    var title = ""
    var chapterNumber: String? = null
    var lines = mutableListOf<LegalLine>()

    fun flush() {
        val trimmed = lines.dropWhile { it.kind == LegalLineKind.SPACER }
            .dropLastWhile { it.kind == LegalLineKind.SPACER }
        if (trimmed.isNotEmpty()) {
            blocks += LegalDocumentBlock(kind, title, chapterNumber, trimmed)
        }
        lines = mutableListOf()
    }

    fun beginSection(rawTitle: String) {
        flush()
        title = cleanMarkdown(rawTitle)
        chapterNumber = null
        when {
            title == "重要提示" -> kind = LegalBlockKind.WARNING
            title.contains("联系") -> kind = LegalBlockKind.CONTACT
            else -> {
                kind = LegalBlockKind.CHAPTER
                Regex("""^([一二三四五六七八九十百]+)[、.]\s*(.+)$""").matchEntire(title)?.let { match ->
                    chapterNumber = match.groupValues[1]
                    title = match.groupValues[2]
                }
            }
        }
    }

    raw.lines().forEach { original ->
        val line = original.trim()
        when {
            line.startsWith("# ") -> Unit
            line.startsWith("**应用名称：") || line.startsWith("**开发者：") ||
                line.startsWith("**更新日期：") || line.startsWith("**生效日期：") -> Unit
            line == "---" -> Unit
            line.startsWith("## ") -> beginSection(line.removePrefix("## "))
            line.startsWith("**感谢您") || line.startsWith("*© ") -> {
                if (kind != LegalBlockKind.FOOTER) {
                    flush()
                    kind = LegalBlockKind.FOOTER
                    title = ""
                    chapterNumber = null
                }
                lines += LegalLine(LegalLineKind.BODY, cleanMarkdown(line))
            }
            line.isBlank() -> {
                if (lines.lastOrNull()?.kind != LegalLineKind.SPACER) lines += LegalLine(LegalLineKind.SPACER)
            }
            line.startsWith("#### ") -> lines += LegalLine(
                LegalLineKind.SUBSECTION,
                cleanMarkdown(line.removePrefix("#### ")),
            )
            line.startsWith("### ") -> lines += LegalLine(
                LegalLineKind.SUBSECTION,
                cleanMarkdown(line.removePrefix("### ")),
            )
            line.startsWith("|---") || line.startsWith("|--") -> Unit
            line.startsWith("|") -> lines += LegalLine(
                LegalLineKind.TABLE,
                line.trim('|').split("|").joinToString("  ·  ") { cleanMarkdown(it.trim()) },
            )
            line.startsWith("- ") -> lines += LegalLine(
                LegalLineKind.BULLET,
                cleanMarkdown(line.removePrefix("- ")),
            )
            Regex("""^\d+\.\s+""").containsMatchIn(line) -> lines += LegalLine(
                LegalLineKind.BULLET,
                cleanMarkdown(line.replaceFirst(Regex("""^\d+\.\s+"""), "")),
            )
            line.startsWith("**") && line.endsWith("**") -> lines += LegalLine(
                LegalLineKind.EMPHASIS,
                cleanMarkdown(line),
            )
            else -> lines += LegalLine(LegalLineKind.BODY, cleanMarkdown(line))
        }
    }
    flush()
    return blocks
}

private fun cleanMarkdown(text: String): String =
    text.replace("**", "")
        .replace(Regex("""\[([^]]+)]\([^)]+\)"""), "$1")
        .replace("*", "")
        .replace("`", "")
        .trim()

private fun loadAsset(context: Context, path: String): String = runCatching {
    context.assets.open(path).bufferedReader().use { it.readText() }
}.getOrElse {
    buildString {
        append("文档加载失败，请稍后重试；如多次失败，请联系客服。")
        if (ServiceLocator.isDebuggable) {
            append("\n\n（$path）")
        }
    }
}
