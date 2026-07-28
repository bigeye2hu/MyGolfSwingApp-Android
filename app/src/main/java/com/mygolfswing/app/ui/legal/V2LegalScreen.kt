package com.mygolfswing.app.ui.legal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

enum class V2LegalDocType(val title: String, val assetPath: String, val updated: String) {
    TERMS("用户服务协议", "legal/terms_of_service.md", "2026年2月6日"),
    PRIVACY("隐私政策", "legal/privacy_policy.md", "2026年2月12日"),
}

/**
 * 法律文档页（对齐 iOS `LegalDocumentView`）。
 * 正文从 assets 读取 Markdown 原文，简单渲染为纯文本。
 */
@Composable
fun V2LegalScreen(type: V2LegalDocType, onBack: () -> Unit) {
    val context = LocalContext.current
    val content = remember(type) { loadAsset(context, type.assetPath) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        LegalTopBar(onClose = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .v2NavigationBarPadding()
                .padding(bottom = 32.dp),
        ) {
            LegalHero(type)
            LegalContentCard(content = content, modifier = Modifier.padding(horizontal = DSSpacing.lg))
        }
    }
}

@Composable
private fun LegalTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = "关闭",
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .clickable(onClick = onClose),
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
                .offset(x = 72.dp, y = (-82).dp)
                .size(210.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.08f)),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                style = DSTypography.caption,
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
private fun LegalContentCard(content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(22.dp), spotColor = DSColors.fairway.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        parseMarkdownLines(content).forEach { line ->
            LegalLine(line)
        }
    }
}

@Composable
private fun LegalLine(line: LegalLine) {
    when (line.kind) {
        LegalLineKind.SPACER -> Spacer(Modifier.height(10.dp))
        LegalLineKind.DIVIDER -> Spacer(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
        )
        LegalLineKind.TITLE -> Text(
            text = line.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            lineHeight = 28.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LegalLineKind.SECTION -> Text(
            text = line.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            lineHeight = 25.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
        LegalLineKind.SUBSECTION -> Text(
            text = line.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.fairway,
            lineHeight = 23.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 5.dp),
        )
        LegalLineKind.EMPHASIS -> Text(
            text = line.text,
            style = DSTypography.bodyBold,
            color = DSColors.inkGreen,
            lineHeight = 23.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        LegalLineKind.BULLET -> Text(
            text = "• ${line.text}",
            style = DSTypography.body,
            color = Color(0xFF52635A),
            lineHeight = 23.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
        )
        LegalLineKind.TABLE -> Text(
            text = line.text,
            style = DSTypography.caption,
            color = Color(0xFF52635A),
            lineHeight = 21.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DSColors.listMutedBg)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
        LegalLineKind.BODY -> Text(
            text = line.text,
            style = DSTypography.body,
            color = Color(0xFF52635A),
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
    }
}

private data class LegalLine(val kind: LegalLineKind, val text: String = "")

private enum class LegalLineKind {
    TITLE, SECTION, SUBSECTION, EMPHASIS, BULLET, TABLE, BODY, DIVIDER, SPACER
}

private fun parseMarkdownLines(raw: String): List<LegalLine> =
    raw.lines().mapNotNull { original ->
        val line = original.trim()
        when {
            line.isBlank() -> LegalLine(LegalLineKind.SPACER)
            line == "---" -> LegalLine(LegalLineKind.DIVIDER)
            line.startsWith("# ") -> LegalLine(LegalLineKind.TITLE, cleanMarkdown(line.removePrefix("# ")))
            line.startsWith("## ") -> LegalLine(LegalLineKind.SECTION, cleanMarkdown(line.removePrefix("## ")))
            line.startsWith("### ") -> LegalLine(LegalLineKind.SUBSECTION, cleanMarkdown(line.removePrefix("### ")))
            line.startsWith("|---") || line.startsWith("|--") -> null
            line.startsWith("|") -> LegalLine(
                LegalLineKind.TABLE,
                line.trim('|').split("|").joinToString("  ·  ") { cleanMarkdown(it.trim()) },
            )
            line.startsWith("- ") -> LegalLine(LegalLineKind.BULLET, cleanMarkdown(line.removePrefix("- ")))
            Regex("""^\d+\.\s+""").containsMatchIn(line) -> LegalLine(
                LegalLineKind.BULLET,
                cleanMarkdown(line.replaceFirst(Regex("""^\d+\.\s+"""), "")),
            )
            line.startsWith("**") && line.endsWith("**") -> LegalLine(
                LegalLineKind.EMPHASIS,
                cleanMarkdown(line),
            )
            else -> LegalLine(LegalLineKind.BODY, cleanMarkdown(line))
        }
    }

private fun cleanMarkdown(text: String): String =
    text.replace("**", "")
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
