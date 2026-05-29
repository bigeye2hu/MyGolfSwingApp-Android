package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

/** AI 接口配置（对齐 iOS `AIServiceConfigView`） */
@Composable
fun V2AIServiceConfigScreen(onBack: () -> Unit) {
    val session = ServiceLocator.session
    val savedProvider by session.aiProviderId.collectAsState(initial = V2AIService.ProviderId.BUILTIN.id)
    val savedModel by session.aiModel.collectAsState(initial = null)
    val savedBase by session.aiBaseUrl.collectAsState(initial = null)
    val activeProviderId = savedProvider ?: V2AIService.ProviderId.BUILTIN.id
    val activeProvider = V2AIService.ProviderId.from(activeProviderId)
    val activeModel = activeProvider.modelOrDefault(savedModel)
    val providerOptions = remember {
        V2AIService.ProviderId.entries.filterNot { it == V2AIService.ProviderId.CUSTOM }
    }
    val scope = rememberCoroutineScope()

    var provider by remember(savedProvider) { mutableStateOf(V2AIService.ProviderId.from(savedProvider)) }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember(savedBase, provider) {
        mutableStateOf(savedBase ?: provider.defaultBase.orEmpty())
    }
    var model by remember(savedModel, provider) {
        mutableStateOf(provider.modelOrDefault(savedModel))
    }
    var saved by remember { mutableStateOf(false) }
    val isActive = provider.id == activeProviderId && model == activeModel
    val canSave = provider == V2AIService.ProviderId.BUILTIN || apiKey.isNotBlank()

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
            ConfigHero()
            SectionTitle("选择 AI 供应商")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(276.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(providerOptions, key = { it.id }) { p ->
                    ProviderCard(
                        provider = p,
                        selected = p == provider,
                        active = p.id == activeProviderId,
                        onClick = {
                            scope.launch {
                                provider = p
                                baseUrl = p.defaultBase.orEmpty()
                                val restoredModel = session.storedAIModel(p.id)
                                    ?: if (p.id == activeProviderId) activeModel else p.defaultModel
                                model = p.modelOrDefault(restoredModel)
                                apiKey = if (p == V2AIService.ProviderId.BUILTIN) "" else session.storedAIKey(p.id).orEmpty()
                                saved = false
                            }
                        },
                    )
                }
            }

            ModelList(
                models = provider.models,
                selectedModel = model,
                onSelect = {
                    model = it
                    saved = false
                },
            )

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
                        onValueChange = {
                            apiKey = it
                            saved = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = fieldColors(),
                        singleLine = true,
                        placeholder = { Text(provider.keyHint, style = DSTypography.caption) },
                    )
                }
            } else {
                BuiltinHint()
            }

            if (saved) {
                StatusBanner("已应用到 AI 对话", success = true)
            } else if (!canSave) {
                StatusBanner("请填写 API Key 后再保存", success = false)
            }

            val buttonTitle = when {
                !canSave -> "请填写 API Key"
                isActive -> "当前模型"
                provider == V2AIService.ProviderId.BUILTIN -> "切换到挥呗 AI →"
                else -> "保存并应用 →"
            }
            Button(
                onClick = {
                    if (!canSave || isActive) return@Button
                    scope.launch {
                        session.setAIProvider(
                            id = provider.id,
                            baseUrl = baseUrl.takeIf { it.isNotBlank() },
                            apiKey = apiKey.takeIf { it.isNotBlank() },
                            model = model.takeIf { it.isNotBlank() },
                            verified = true,
                        )
                        V2AIChatStore.refreshConfigState()
                        saved = true
                    }
                },
                enabled = canSave && !isActive,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DSColors.fairway,
                    disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.4f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    buttonTitle,
                    style = DSTypography.bodyBold,
                    color = DSColors.warmWhite,
                )
            }
        }
    }

    LaunchedEffect(provider.id, activeProviderId, activeModel) {
        val restoredModel = session.storedAIModel(provider.id)
            ?: if (provider.id == activeProviderId) activeModel else provider.defaultModel
        model = provider.modelOrDefault(restoredModel)
        apiKey = if (provider == V2AIService.ProviderId.BUILTIN) "" else session.storedAIKey(provider.id).orEmpty()
        baseUrl = provider.defaultBase ?: savedBase.orEmpty()
    }
}

@Composable
private fun ConfigHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = DSColors.fairway.copy(alpha = 0.16f))
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFD8E2DC), DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Memory, null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Text("配置你的 AI 教练", style = DSTypography.titleLarge, color = DSColors.inkGreen)
        Text(
            "内置模型每天30条免费，也可配置自己的 API Key 无限使用",
            style = DSTypography.caption,
            color = Color(0xFF6C757D),
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = DSTypography.bodyBold, color = DSColors.inkGreen)
}

@Composable
private fun ProviderCard(
    provider: V2AIService.ProviderId,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(
                if (selected) DSColors.mistGreen else DSColors.cardWhite,
                RoundedCornerShape(18.dp),
            )
            .border(
                1.2.dp,
                if (selected) DSColors.fairway else DSColors.borderLine,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) DSColors.fairway else DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    null,
                    tint = if (selected) Color.White else DSColors.shadowGray,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (active) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = DSColors.fairway,
                    modifier = Modifier.padding(start = 6.dp).size(14.dp),
                )
            }
        }
        Column {
            Text(provider.displayName, style = DSTypography.bodyBold, color = DSColors.inkGreen, maxLines = 1)
            Text(provider.description, fontSize = 10.sp, color = DSColors.shadowGray, maxLines = 1)
        }
    }
}

@Composable
private fun ModelList(
    models: List<V2AIService.ModelOption>,
    selectedModel: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("选择模型", style = DSTypography.bodyBold, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            Text("${models.size} 个可用", style = DSTypography.caption, color = DSColors.shadowGray)
        }
        models.forEach { option ->
            val selected = option.id == selectedModel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) DSColors.mistGreen.copy(alpha = 0.5f) else Color.Transparent)
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = 10.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = if (selected) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    option.displayName,
                    style = if (selected) DSTypography.bodyBold else DSTypography.body,
                    color = if (selected) DSColors.fairway else DSColors.inkGreen,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                )
                Text(option.trait, style = DSTypography.caption, color = DSColors.shadowGray, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BuiltinHint() {
    Text(
        "使用内置「挥呗 AI」，无需配置 Key，每日有免费额度。",
        style = DSTypography.caption,
        color = DSColors.shadowGray,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.7f))
            .padding(12.dp),
    )
}

@Composable
private fun StatusBanner(message: String, success: Boolean) {
    val color = if (success) DSColors.fairway else DSColors.flagRed
    Text(
        message,
        style = DSTypography.caption,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
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
