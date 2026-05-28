package com.mygolfswing.app.ui.legal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
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
        V2NavTopBarCentered(title = type.title, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
        ) {
            Text(
                text = "更新日期：${type.updated}",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = content,
                style = DSTypography.body,
                color = DSColors.inkGreen,
                lineHeight = DSTypography.body.fontSize * 1.5f,
            )
        }
    }
}

private fun loadAsset(context: Context, path: String): String = runCatching {
    context.assets.open(path).bufferedReader().use { it.readText() }
}.getOrElse {
    "文档加载失败，请稍后重试。\n\n（$path）"
}
