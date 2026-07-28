package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.ai.normalizedAIBaseUrl
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** AI 接口配置（对齐 iOS `AIServiceConfigView`） */
@Composable
fun V2AIServiceConfigScreen(onBack: () -> Unit) {
    val session = ServiceLocator.session
    val savedProvider by session.aiProviderId.collectAsState(initial = V2AIService.ProviderId.BUILTIN.id)
    val savedModel by session.aiModel.collectAsState(initial = null)
    val savedBase by session.aiBaseUrl.collectAsState(initial = null)
    val savedApiKey by session.aiApiKey.collectAsState(initial = null)
    val activeProviderId = savedProvider ?: V2AIService.ProviderId.BUILTIN.id
    val activeProvider = V2AIService.ProviderId.from(activeProviderId)
    val activeModel = activeProvider.modelOrDefault(savedModel)
    val providerOptions = remember {
        V2AIService.ProviderId.entries.filterNot { it == V2AIService.ProviderId.CUSTOM }
    }
    val scope = rememberCoroutineScope()

    var provider by remember(savedProvider) { mutableStateOf(V2AIService.ProviderId.from(savedProvider)) }
    var apiKey by remember { mutableStateOf("") }
    var storedApiKeyForProvider by remember { mutableStateOf("") }
    var baseUrl by remember(savedBase, provider) {
        mutableStateOf(aiBaseUrlForProvider(provider, activeProviderId, savedBase))
    }
    var model by remember(savedModel, provider) {
        mutableStateOf(provider.modelOrDefault(savedModel))
    }
    var saved by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var verifiedProviderIds by remember { mutableStateOf(emptySet<String>()) }
    val normalizedBaseUrl = normalizedAIBaseUrl(baseUrl)
    val hasInvalidBaseUrl = provider != V2AIService.ProviderId.BUILTIN &&
        baseUrl.isNotBlank() &&
        normalizedBaseUrl == null
    val activeBaseForSelected = aiBaseUrlForProvider(provider, activeProviderId, savedBase)
    val hasActiveBaseUrlEdit = provider.id == activeProviderId &&
        provider != V2AIService.ProviderId.BUILTIN &&
        normalizedBaseUrl != normalizedAIBaseUrl(activeBaseForSelected)
    val hasActiveApiKeyEdit = provider.id == activeProviderId &&
        provider != V2AIService.ProviderId.BUILTIN &&
        apiKey.trim().isNotBlank() &&
        apiKey.trim() != savedApiKey.orEmpty()
    val hasActiveConfigEdit = hasActiveBaseUrlEdit || hasActiveApiKeyEdit
    val isActive = provider.id == activeProviderId && model == activeModel && !hasActiveConfigEdit
    val effectiveApiKey = apiKey.trim().ifBlank {
        storedApiKeyForProvider.takeIf { it.isNotBlank() }
    }
    val canSave = provider == V2AIService.ProviderId.BUILTIN ||
        (!effectiveApiKey.isNullOrBlank() && !hasInvalidBaseUrl)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .v2StatusBarPadding()
                .imePadding()
                .v2NavigationBarPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ConfigHero()
            ProviderGrid(
                providers = providerOptions,
                selectedProvider = provider,
                activeProviderId = activeProviderId,
                verifiedProviderIds = verifiedProviderIds,
                onSelect = { p ->
                    scope.launch {
                        provider = p
                        baseUrl = aiBaseUrlForProvider(p, activeProviderId, savedBase)
                        val restoredModel = session.storedAIModel(p.id)
                            ?: if (p.id == activeProviderId) activeModel else p.defaultModel
                        val restoredKey = if (p == V2AIService.ProviderId.BUILTIN) "" else session.storedAIKey(p.id).orEmpty()
                        model = p.modelOrDefault(restoredModel)
                        storedApiKeyForProvider = restoredKey
                        apiKey = ""
                        saved = false
                        saveError = null
                    }
                },
            )

            ModelList(
                models = provider.models,
                selectedModel = model,
                onSelect = {
                    model = it
                    saved = false
                    saveError = null
                },
            )

            if (provider != V2AIService.ProviderId.BUILTIN) {
                LabeledField("API Base URL") {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            saved = false
                            saveError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        singleLine = true,
                    )
                }
                ApiKeyCard(
                    value = apiKey,
                    hint = provider.keyHint,
                    maskedKey = maskedAIKey(storedApiKeyForProvider),
                    onValueChange = {
                        apiKey = it
                        saved = false
                        saveError = null
                    },
                )
            } else {
                BuiltinHint()
            }

            if (saved) {
                StatusBanner("已应用到 AI 对话", success = true)
            } else if (saveError != null) {
                StatusBanner(saveError.orEmpty(), success = false)
            } else if (hasInvalidBaseUrl) {
                StatusBanner("API Base URL 需要填写有效的 HTTPS 地址", success = false)
            } else if (!canSave) {
                StatusBanner("请填写 API Key 后再保存", success = false)
            }

            val buttonTitle = when {
                saving -> "保存中..."
                hasInvalidBaseUrl -> "请使用 HTTPS 地址"
                !canSave -> "请填写 API Key"
                isActive -> "当前模型"
                provider.id == activeProviderId && hasActiveConfigEdit -> "保存并应用 →"
                provider == V2AIService.ProviderId.BUILTIN -> "切换到挥呗 AI →"
                else -> "保存并应用 →"
            }
            val buttonEnabled = canSave && !isActive && !saving
            val buttonTone = if (provider.id == activeProviderId && hasActiveConfigEdit) {
                DSColors.trophyGold
            } else {
                DSColors.fairway
            }
            Button(
                onClick = {
                    if (!canSave || isActive || saving) return@Button
                    scope.launch {
                        saving = true
                        saveError = null
                        try {
                            session.setAIProvider(
                                id = provider.id,
                                baseUrl = normalizedBaseUrl,
                                apiKey = effectiveApiKey,
                                model = model.takeIf { it.isNotBlank() },
                                verified = true,
                            )
                            V2AIChatStore.refreshConfigState()
                            verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
                            saved = true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            saved = false
                            saveError = aiConfigError("保存失败，请稍后重试", e.message)
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = buttonEnabled,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonTone,
                    disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.4f),
                ),
                modifier = Modifier
                    .shadow(
                        elevation = if (buttonEnabled) 10.dp else 0.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = buttonTone.copy(alpha = 0.30f),
                        spotColor = buttonTone.copy(alpha = 0.30f),
                    )
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    buttonTitle,
                    style = DSTypography.bodyBold,
                    color = DSColors.warmWhite,
                )
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "关闭",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        AIConfigFloatingCloseButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .v2StatusBarPadding()
                .padding(start = 16.dp, top = 16.dp),
        )
    }

    LaunchedEffect(provider.id, activeProviderId, activeModel, savedBase, savedApiKey) {
        val restoredModel = session.storedAIModel(provider.id)
            ?: if (provider.id == activeProviderId) activeModel else provider.defaultModel
        val restoredKey = if (provider == V2AIService.ProviderId.BUILTIN) "" else session.storedAIKey(provider.id).orEmpty()
        model = provider.modelOrDefault(restoredModel)
        storedApiKeyForProvider = restoredKey
        apiKey = ""
        baseUrl = aiBaseUrlForProvider(provider, activeProviderId, savedBase)
    }

    LaunchedEffect(activeProviderId, savedApiKey) {
        verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
    }
}

private suspend fun loadVerifiedAIProviders(
    session: com.mygolfswing.app.data.session.V2SessionStore,
    providers: List<V2AIService.ProviderId>,
): Set<String> =
    providers
        .filter { session.isAIProviderVerified(it.id) }
        .map { it.id }
        .toSet()

private fun aiBaseUrlForProvider(
    provider: V2AIService.ProviderId,
    activeProviderId: String,
    savedBase: String?,
): String =
    if (provider.id == activeProviderId) {
        savedBase ?: provider.defaultBase.orEmpty()
    } else {
        provider.defaultBase.orEmpty()
    }

private fun aiConfigError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

private fun maskedAIKey(key: String): String {
    val trimmed = key.trim()
    if (trimmed.isBlank()) return ""
    val prefix = trimmed.take(3).takeIf { trimmed.length > 6 }.orEmpty()
    val suffix = trimmed.takeLast(minOf(4, trimmed.length))
    return "$prefix•••••••••••$suffix"
}

@Composable
private fun AIConfigFloatingCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(CircleShape)
            .background(DSColors.cardWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "关闭",
            tint = DSColors.shadowGray,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ApiKeyCard(
    value: String,
    hint: String,
    maskedKey: String,
    onValueChange: (String) -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }
    val hasKey = value.isNotBlank() || maskedKey.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("输入 API Key", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text("安全加密存储", style = DSTypography.caption, color = Color(0xFF6C757D))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFD8E2DC).copy(alpha = 0.30f))
                .border(1.dp, DSColors.fairway.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = Brush.verticalGradient(listOf(DSColors.fairway, DSColors.fairway)),
                visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            if (!revealed && maskedKey.isNotBlank()) maskedKey else hint,
                            color = if (!revealed && maskedKey.isNotBlank()) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            TextButton(
                onClick = { revealed = !revealed },
                modifier = Modifier.size(34.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    if (revealed) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (revealed) "隐藏 API Key" else "显示 API Key",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hasKey) {
                Icon(Icons.Filled.CheckCircle, null, tint = DSColors.goldBright, modifier = Modifier.size(12.dp))
            } else {
                Icon(
                    Icons.Filled.RadioButtonUnchecked,
                    null,
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                if (hasKey) "已输入 API Key" else "未输入 API Key",
                style = DSTypography.caption,
                color = Color(0xFF6C757D),
            )
        }
    }
}

@Composable
private fun ConfigHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = DSColors.fairway.copy(alpha = 0.16f))
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFD8E2DC), DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Memory, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text(
            "配置你的 AI 教练",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = DSColors.fairway)) {
                    append("内置模型每天30条免费")
                }
                withStyle(SpanStyle(color = Color(0xFF6C757D))) {
                    append("，也可配置自己的 API Key 无限使用")
                }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        )
    }
}

@Composable
private fun ProviderGrid(
    providers: List<V2AIService.ProviderId>,
    selectedProvider: V2AIService.ProviderId,
    activeProviderId: String,
    verifiedProviderIds: Set<String>,
    onSelect: (V2AIService.ProviderId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(
                1.dp,
                DSColors.fairway.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("选择 AI 供应商", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text("点击查看", style = DSTypography.caption, color = Color(0xFF6C757D))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(184.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(providers, key = { it.id }) { p ->
                ProviderCard(
                    provider = p,
                    selected = p == selectedProvider,
                    active = p.id == activeProviderId,
                    verified = p.id in verifiedProviderIds,
                    onClick = { onSelect(p) },
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: V2AIService.ProviderId,
    selected: Boolean,
    active: Boolean,
    verified: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    active -> DSColors.fairway.copy(alpha = 0.10f)
                    selected -> DSColors.mistGreen.copy(alpha = 0.30f)
                    else -> DSColors.cardWhite
                },
            )
            .border(
                if (active) 2.dp else if (selected) 1.5.dp else 1.dp,
                if (active || selected) DSColors.fairway else DSColors.fairway.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(providerBrush(provider)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    providerIcon(provider),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                provider.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.inkGreen,
                maxLines = 1,
            )
        }

        when {
            active -> Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(DSColors.fairway)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(8.dp))
                Text("使用中", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            selected -> Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        if (verified && !active) {
            Text(
                "已验证",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD8E2DC).copy(alpha = 0.80f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

private fun providerIcon(provider: V2AIService.ProviderId): ImageVector = when (provider) {
    V2AIService.ProviderId.BUILTIN -> Icons.Filled.AutoAwesome
    V2AIService.ProviderId.DASHSCOPE -> Icons.Filled.Cloud
    V2AIService.ProviderId.DEEPSEEK -> Icons.Filled.Lightbulb
    V2AIService.ProviderId.MOONSHOT -> Icons.Filled.Nightlight
    V2AIService.ProviderId.MINIMAX -> Icons.Filled.Speed
    V2AIService.ProviderId.OPENAI -> Icons.Filled.Psychology
    V2AIService.ProviderId.CUSTOM -> Icons.Filled.Memory
}

private fun providerBrush(provider: V2AIService.ProviderId): Brush {
    val colors = when (provider) {
        V2AIService.ProviderId.BUILTIN -> listOf(Color(0xFF10B981), DSColors.fairway)
        V2AIService.ProviderId.DASHSCOPE -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        V2AIService.ProviderId.DEEPSEEK -> listOf(Color(0xFF6366F1), Color(0xFF4338CA))
        V2AIService.ProviderId.MOONSHOT -> listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
        V2AIService.ProviderId.MINIMAX -> listOf(Color(0xFFF97316), Color(0xFFEA580C))
        V2AIService.ProviderId.OPENAI -> listOf(Color(0xFF10B981), DSColors.fairway)
        V2AIService.ProviderId.CUSTOM -> listOf(DSColors.shadowGray, DSColors.inkGreen)
    }
    return Brush.linearGradient(colors)
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
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("选择模型", style = DSTypography.bodyBold, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            Text("${models.size} 个可用", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6C757D))
        }
        models.forEach { option ->
            val selected = option.id == selectedModel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .height(44.dp)
                    .background(if (selected) Color(0xFFD8E2DC).copy(alpha = 0.30f) else Color.Transparent)
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    null,
                    tint = if (selected) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.60f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    option.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) DSColors.fairway else DSColors.inkGreen,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    option.trait,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = Color(0xFF6C757D),
                    maxLines = 1,
                )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp),
        )
        Text(
            message,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
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
