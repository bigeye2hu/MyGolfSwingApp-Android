package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** AI 接口配置（对齐 iOS `AIServiceConfigView`） */
@Composable
fun V2AIServiceConfigScreen(onBack: () -> Unit) {
    val session = ServiceLocator.session
    val savedProvider by session.aiProviderId.collectAsState(initial = V2AIService.ProviderId.BUILTIN.id)
    val savedModel by session.aiModel.collectAsState(initial = null)
    val savedBase by session.aiBaseUrl.collectAsState(initial = null)

    var provider by remember(savedProvider) { mutableStateOf(V2AIService.ProviderId.from(savedProvider)) }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember(savedBase, provider) {
        mutableStateOf(savedBase ?: provider.defaultBase.orEmpty())
    }
    var model by remember(savedModel, provider) {
        mutableStateOf(savedModel ?: provider.defaultModel)
    }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "AI 接口配置", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("选择 AI 供应商", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            V2AIService.ProviderId.entries.forEach { p ->
                ProviderRow(
                    name = p.displayName,
                    selected = p == provider,
                    onClick = {
                        provider = p
                        baseUrl = p.defaultBase.orEmpty()
                        model = p.defaultModel
                    },
                )
            }

            if (provider != V2AIService.ProviderId.BUILTIN) {
                LabeledField("API Base URL") {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        singleLine = true,
                    )
                }
                LabeledField("API Key") {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = fieldColors(),
                        singleLine = true,
                    )
                }
                LabeledField("模型名称") {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        singleLine = true,
                    )
                }
            } else {
                Text(
                    "使用内置「挥呗 AI」，无需配置 Key，每日有免费额度。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        session.setAIProvider(
                            id = provider.id,
                            baseUrl = baseUrl.takeIf { it.isNotBlank() },
                            apiKey = apiKey.takeIf { it.isNotBlank() },
                            model = model.takeIf { it.isNotBlank() },
                        )
                        saved = true
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("保存配置", style = DSTypography.bodyBold, color = DSColors.warmWhite)
            }

            if (saved) {
                Text("已保存", style = DSTypography.caption, color = DSColors.fairway)
            }
        }
    }

    // 预填已有 key（只读一次）
    LaunchedEffect(Unit) {
        apiKey = session.aiApiKey.first().orEmpty()
    }
}

@Composable
private fun ProviderRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) DSColors.mistGreen else DSColors.cardWhite,
                RoundedCornerShape(12.dp),
            )
            .border(
                1.dp,
                if (selected) DSColors.fairway else DSColors.borderLine,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = DSTypography.body, color = DSColors.inkGreen)
        if (selected) Text("✓", color = DSColors.fairway)
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = DSTypography.caption, color = DSColors.shadowGray)
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    cursorColor = DSColors.fairway,
)
