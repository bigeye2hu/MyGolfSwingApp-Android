package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.platform.testTag
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class V2AIConfigPresentation { PROFILE, CHAT }

/**
 * AI 接口配置。
 *
 * iOS keeps two deliberately different entry presentations: Profile opens the
 * management-oriented `AIServiceConfigView`, while AI Chat opens the immersive
 * `V2AIConfigView`. Android preserves that distinction instead of routing both
 * entry points to one screen.
 */
@Composable
fun V2AIServiceConfigScreen(
    onBack: () -> Unit,
    presentation: V2AIConfigPresentation = V2AIConfigPresentation.CHAT,
) {
    val profilePresentation = presentation == V2AIConfigPresentation.PROFILE
    val session = ServiceLocator.session
    val savedProvider by session.aiProviderId.collectAsState(initial = AI_CONFIG_LOADING_PROVIDER)
    val savedModel by session.aiModel.collectAsState(initial = null)
    val savedBase by session.aiBaseUrl.collectAsState(initial = null)
    val savedApiKey by session.aiApiKey.collectAsState(initial = null)
    val configLoaded = savedProvider != AI_CONFIG_LOADING_PROVIDER
    val activeProviderId = savedProvider
        ?.takeUnless { it == AI_CONFIG_LOADING_PROVIDER }
        ?: V2AIService.ProviderId.BUILTIN.id
    val activeProvider = V2AIService.ProviderId.from(activeProviderId)
    val activeModel = activeProvider.modelOrDefault(savedModel)
    val providerOptions = remember(profilePresentation) {
        if (profilePresentation) V2AIService.ProviderId.entries
        else V2AIService.ProviderId.entries.filterNot { it == V2AIService.ProviderId.CUSTOM }
    }
    val scope = rememberCoroutineScope()

    var focusedProviderId by remember { mutableStateOf<String?>(null) }
    val provider = V2AIService.ProviderId.from(focusedProviderId ?: activeProviderId)
    var apiKey by remember { mutableStateOf("") }
    var storedApiKeyForProvider by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(provider.defaultModel) }
    var baseUrl by remember { mutableStateOf(provider.defaultBase.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testingChat by remember { mutableStateOf(false) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }
    var lastTestFailed by remember { mutableStateOf(false) }
    var chatTestResult by remember { mutableStateOf<String?>(null) }
    var chatTestFailed by remember { mutableStateOf(false) }
    var profileConfigDirty by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var verifiedProviderIds by remember { mutableStateOf(emptySet<String>()) }
    val effectiveApiKey = apiKey.trim().ifBlank {
        storedApiKeyForProvider.takeIf { it.isNotBlank() }
    }
    val focusedIsActive = provider.id == activeProviderId
    val focusedIsDirty = when {
        provider == V2AIService.ProviderId.BUILTIN -> false
        focusedIsActive -> {
            (apiKey.trim().isNotBlank() && apiKey.trim() != savedApiKey.orEmpty()) ||
                model != activeModel ||
                (profilePresentation && baseUrl.trim() != savedBase.orEmpty().trim())
        }
        else -> (apiKey.trim().isNotBlank() && apiKey.trim() != storedApiKeyForProvider) ||
            (profilePresentation && baseUrl.trim() != provider.defaultBase.orEmpty().trim())
    }
    val profileConfigurationReady = provider != V2AIService.ProviderId.CUSTOM ||
        (baseUrl.trim().startsWith("https://") && model.trim().isNotBlank())
    val primaryState = if (profilePresentation) {
        aiManagementPrimaryState(
            focusedIsBuiltin = provider == V2AIService.ProviderId.BUILTIN,
            focusedHasEffectiveKey = !effectiveApiKey.isNullOrBlank(),
            focusedIsVerified = provider.id in verifiedProviderIds,
            focusedIsDirty = profileConfigDirty,
            isTesting = testing,
            lastTestFailed = lastTestFailed,
        )
    } else {
        aiConfigPrimaryState(
            focusedIsBuiltin = provider == V2AIService.ProviderId.BUILTIN,
            focusedIsActive = focusedIsActive,
            focusedIsDirty = focusedIsDirty,
            focusedIsVerified = provider.id in verifiedProviderIds,
            focusedHasEffectiveKey = !effectiveApiKey.isNullOrBlank(),
            isTesting = testing,
            lastTestFailed = lastTestFailed,
        )
    }
    val profileImmediateAction = primaryState == AIConfigPrimaryState.CURRENT_NO_EDIT ||
        primaryState == AIConfigPrimaryState.SWITCH_VERIFIED ||
        primaryState == AIConfigPrimaryState.SWITCH_BUILTIN
    val buttonEnabled = (
        primaryState.enabled || (profilePresentation && profileImmediateAction)
    ) && !saving && (!profilePresentation || profileConfigurationReady)
    val buttonTone = when {
        profilePresentation && profileImmediateAction -> DSColors.fairway
        else -> when (primaryState.tone) {
        AIConfigButtonTone.PRIMARY -> DSColors.fairway
        AIConfigButtonTone.WARNING -> DSColors.trophyGold
        AIConfigButtonTone.DANGER -> DSColors.flagRed
        AIConfigButtonTone.NEUTRAL -> DSColors.shadowGray.copy(alpha = 0.5f)
        }
    }
    val primaryButtonTitle = if (!profileConfigurationReady && profilePresentation) {
        "请完善高级配置"
    } else if (profilePresentation) {
        when (primaryState) {
            AIConfigPrimaryState.CURRENT_NO_EDIT,
            AIConfigPrimaryState.SWITCH_VERIFIED,
            AIConfigPrimaryState.SWITCH_BUILTIN -> "开始对话"
            AIConfigPrimaryState.CURRENT_EDITED,
            AIConfigPrimaryState.SWITCH_VERIFIED_EDITED -> "重新测试连接"
            AIConfigPrimaryState.NO_KEY -> "请先填写 API Key"
            AIConfigPrimaryState.UNVERIFIED_HAS_KEY -> "测试连接"
            AIConfigPrimaryState.TESTING -> "连接中…"
            AIConfigPrimaryState.FAILED -> "重试连接"
        }
    } else primaryState.title
    val selectProvider: (V2AIService.ProviderId) -> Unit = { nextProvider ->
        scope.launch {
            focusedProviderId = nextProvider.id
            val restoredModel = session.storedAIModel(nextProvider.id)
                ?: if (nextProvider.id == activeProviderId) activeModel else nextProvider.defaultModel
            val restoredKey = if (nextProvider == V2AIService.ProviderId.BUILTIN) ""
            else session.storedAIKey(nextProvider.id).orEmpty()
            model = nextProvider.modelOrDefault(restoredModel)
            baseUrl = if (nextProvider.id == activeProviderId) {
                savedBase ?: nextProvider.defaultBase.orEmpty()
            } else {
                nextProvider.defaultBase.orEmpty()
            }
            storedApiKeyForProvider = restoredKey
            apiKey = if (profilePresentation) restoredKey else ""
            testResultMessage = null
            lastTestFailed = false
            chatTestResult = null
            chatTestFailed = false
            profileConfigDirty = false
            saveError = null
            if (profilePresentation) {
                setAIProviderForConfig(
                    session = session,
                    id = nextProvider.id,
                    baseUrl = nextProvider.defaultBase,
                    apiKey = restoredKey.takeIf { it.isNotBlank() },
                    model = nextProvider.modelOrDefault(restoredModel).takeIf { it.isNotBlank() },
                    verified = session.isAIProviderVerified(nextProvider.id),
                )
                V2AIChatStore.refreshConfigState()
            }
        }
    }

    val performPrimaryAction: () -> Unit = primaryAction@{
        if (!buttonEnabled) return@primaryAction
        val targetProvider = provider
        val targetModel = model
        val targetApiKey = effectiveApiKey
        val targetBaseUrl = if (profilePresentation) baseUrl.trim() else targetProvider.defaultBase
        val actionState = primaryState
        scope.launch {
            saving = true
            testing = actionState.requiresConnectionTest
            saveError = null
            testResultMessage = null
            chatTestResult = null
            try {
                if (actionState.requiresConnectionTest) {
                    val snapshot = AIConfigActiveSnapshot(
                        providerId = activeProviderId,
                        baseUrl = savedBase,
                        apiKey = savedApiKey,
                        model = activeModel,
                        verified = session.isAIProviderVerified(activeProviderId),
                    )
                    try {
                        setAIProviderForConfig(
                            session = session,
                            id = targetProvider.id,
                            baseUrl = targetBaseUrl,
                            apiKey = targetApiKey,
                            model = targetModel.takeIf { it.isNotBlank() },
                            verified = false,
                        )
                        V2AIChatStore.refreshConfigState()
                        if (profilePresentation) {
                            V2AIConfigTestHooks.connectivityOverride?.invoke()
                                ?: ServiceLocator.aiService.testConnectivity()
                        } else {
                            val response = StringBuilder()
                            val history = listOf("system" to "你好", "user" to "你好")
                            withTimeout(V2AIConfigTestHooks.timeoutMsOverride ?: AI_CONFIG_TEST_TIMEOUT_MS) {
                                (V2AIConfigTestHooks.streamChatOverride?.invoke(history)
                                    ?: ServiceLocator.aiService.streamChat(history))
                                    .collect { response.append(it) }
                            }
                            check(response.isNotBlank()) { "empty AI configuration test response" }
                        }
                        setAIProviderForConfig(
                            session = session,
                            id = targetProvider.id,
                            baseUrl = targetBaseUrl,
                            apiKey = targetApiKey,
                            model = targetModel.takeIf { it.isNotBlank() },
                            verified = true,
                        )
                        V2AIChatStore.refreshConfigState()
                        storedApiKeyForProvider = targetApiKey.orEmpty()
                        apiKey = if (profilePresentation) targetApiKey.orEmpty() else ""
                        lastTestFailed = false
                        profileConfigDirty = false
                        testResultMessage = if (profilePresentation) null
                        else "${targetProvider.displayName} 连接成功，已切换为当前模型"
                    } catch (e: TimeoutCancellationException) {
                        restoreAIConfigSnapshot(session, snapshot)
                        V2AIChatStore.refreshConfigState()
                        lastTestFailed = true
                        testResultMessage = aiConfigError(
                            "${targetProvider.displayName} 连接失败，请检查 API Key 或网络",
                            e.message,
                        )
                    } catch (e: CancellationException) {
                        restoreAIConfigSnapshot(session, snapshot)
                        V2AIChatStore.refreshConfigState()
                        throw e
                    } catch (e: Throwable) {
                        restoreAIConfigSnapshot(session, snapshot)
                        V2AIChatStore.refreshConfigState()
                        lastTestFailed = true
                        testResultMessage = aiConfigThrowableError(
                            "${targetProvider.displayName} 连接失败，请检查 API Key 或网络",
                            e,
                        )
                    }
                } else {
                    setAIProviderForConfig(
                        session = session,
                        id = targetProvider.id,
                        baseUrl = targetBaseUrl,
                        apiKey = targetApiKey,
                        model = targetModel.takeIf { it.isNotBlank() },
                        verified = true,
                    )
                    V2AIChatStore.refreshConfigState()
                    if (profilePresentation) onBack()
                }
                verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = aiConfigThrowableError("保存失败，请稍后重试", e)
            } finally {
                testing = false
                saving = false
            }
        }
    }

    val revalidateConnection: () -> Unit = revalidate@{
        if (testing || testingChat || provider == V2AIService.ProviderId.BUILTIN) return@revalidate
        val targetProvider = provider
        val targetKey = effectiveApiKey
        if (targetKey.isNullOrBlank()) return@revalidate
        scope.launch {
            testing = true
            testResultMessage = null
            chatTestResult = null
            try {
                setAIProviderForConfig(
                    session = session,
                    id = targetProvider.id,
                    baseUrl = baseUrl.trim(),
                    apiKey = targetKey,
                    model = model.takeIf { it.isNotBlank() },
                    verified = false,
                )
                V2AIChatStore.refreshConfigState()
                V2AIConfigTestHooks.connectivityOverride?.invoke()
                    ?: ServiceLocator.aiService.testConnectivity()
                setAIProviderForConfig(
                    session = session,
                    id = targetProvider.id,
                    baseUrl = baseUrl.trim(),
                    apiKey = targetKey,
                    model = model.takeIf { it.isNotBlank() },
                    verified = true,
                )
                storedApiKeyForProvider = targetKey
                apiKey = targetKey
                verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
                lastTestFailed = false
                profileConfigDirty = false
                V2AIChatStore.refreshConfigState()
            } catch (e: TimeoutCancellationException) {
                lastTestFailed = true
                testResultMessage = aiConfigError("连接失败，请检查 API Key 或网络", e.message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastTestFailed = true
                testResultMessage = aiConfigThrowableError("连接失败，请检查 API Key 或网络", e)
            } finally {
                testing = false
            }
        }
    }

    val testConversation: () -> Unit = testConversation@{
        if (testing || testingChat || provider.id !in verifiedProviderIds) return@testConversation
        scope.launch {
            testingChat = true
            chatTestResult = null
            try {
                val response = StringBuilder()
                val history = listOf(
                    "system" to "请用一句话回复：连接成功",
                    "user" to "请用一句话回复：连接成功",
                )
                withTimeout(V2AIConfigTestHooks.timeoutMsOverride ?: AI_CONFIG_TEST_TIMEOUT_MS) {
                    (V2AIConfigTestHooks.streamChatOverride?.invoke(history)
                        ?: ServiceLocator.aiService.streamChat(history))
                        .collect { response.append(it) }
                }
                check(response.isNotBlank()) { "empty AI chat test response" }
                chatTestFailed = false
                chatTestResult = "对话正常：${response.toString().trim().take(40)}"
            } catch (e: TimeoutCancellationException) {
                chatTestFailed = true
                chatTestResult = aiConfigError("对话测试失败，请稍后重试", e.message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                chatTestFailed = true
                chatTestResult = aiConfigThrowableError("对话测试失败，请稍后重试", e)
            } finally {
                testingChat = false
            }
        }
    }

    val disconnectProvider: () -> Unit = {
        if (provider != V2AIService.ProviderId.BUILTIN) {
            scope.launch {
                setAIProviderForConfig(
                    session = session,
                    id = provider.id,
                    baseUrl = baseUrl.ifBlank { provider.defaultBase.orEmpty() },
                    apiKey = null,
                    model = model.takeIf { it.isNotBlank() },
                    verified = false,
                )
                storedApiKeyForProvider = ""
                apiKey = ""
                testResultMessage = null
                chatTestResult = null
                lastTestFailed = false
                profileConfigDirty = false
                verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
                V2AIChatStore.refreshConfigState()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (profilePresentation) {
                AIProfileConfigTopBar(onDone = onBack)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .then(if (profilePresentation) Modifier else Modifier.v2StatusBarPadding())
                    .imePadding()
                    .v2NavigationBarPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!profilePresentation) ConfigHero()
                if (profilePresentation) {
                    ManagementProviderGrid(
                        providers = providerOptions,
                        selectedProvider = provider,
                        verifiedProviderIds = verifiedProviderIds,
                        onSelect = selectProvider,
                    )
                } else {
                    ProviderGrid(
                        providers = providerOptions,
                        selectedProvider = provider,
                        activeProviderId = activeProviderId,
                        verifiedProviderIds = verifiedProviderIds,
                        onSelect = selectProvider,
                    )
                }

                if (provider.models.isNotEmpty()) {
                    val selectModel: (String) -> Unit = {
                        model = it
                        testResultMessage = null
                        chatTestResult = null
                        lastTestFailed = false
                        saveError = null
                        if (profilePresentation) {
                            scope.launch {
                                setAIProviderForConfig(
                                    session = session,
                                    id = provider.id,
                                    baseUrl = baseUrl.ifBlank { provider.defaultBase.orEmpty() },
                                    apiKey = effectiveApiKey,
                                    model = it,
                                    verified = provider.id in verifiedProviderIds,
                                )
                                V2AIChatStore.refreshConfigState()
                            }
                        }
                    }
                    if (profilePresentation) {
                        ManagementModelList(provider.models, model, selectModel)
                    } else {
                        ModelList(provider.models, model, onSelect = selectModel)
                    }
                }

                if (provider == V2AIService.ProviderId.BUILTIN) {
                    if (profilePresentation) BuiltinAIKeyHint()
                } else {
                    val changeApiKey: (String) -> Unit = {
                            apiKey = it
                            testResultMessage = null
                            chatTestResult = null
                            lastTestFailed = false
                            if (profilePresentation) profileConfigDirty = true
                            saveError = null
                    }
                    if (profilePresentation) {
                        ManagementApiKeyCard(apiKey, provider.keyHint, changeApiKey)
                    } else {
                        ApiKeyCard(
                            value = apiKey,
                            maskedKey = maskedAIKey(storedApiKeyForProvider),
                            keyHint = provider.keyHint,
                            managementStyle = false,
                            onValueChange = changeApiKey,
                            onDisconnect = null,
                        )
                    }
                }

                if (profilePresentation && provider != V2AIService.ProviderId.BUILTIN) {
                    ManagementAdvancedConfig(
                        baseUrl = baseUrl,
                        model = model,
                        onBaseUrlChange = {
                            baseUrl = it
                            testResultMessage = null
                            lastTestFailed = false
                            profileConfigDirty = true
                            val nextBaseUrl = it
                            scope.launch {
                                setAIProviderForConfig(
                                    session = session,
                                    id = provider.id,
                                    baseUrl = nextBaseUrl,
                                    apiKey = effectiveApiKey,
                                    model = model.takeIf { value -> value.isNotBlank() },
                                    verified = provider.id in verifiedProviderIds,
                                )
                                V2AIChatStore.refreshConfigState()
                            }
                        },
                        onModelChange = {
                            model = it
                            testResultMessage = null
                            lastTestFailed = false
                            profileConfigDirty = true
                            val nextModel = it
                            scope.launch {
                                setAIProviderForConfig(
                                    session = session,
                                    id = provider.id,
                                    baseUrl = baseUrl,
                                    apiKey = effectiveApiKey,
                                    model = nextModel.takeIf { value -> value.isNotBlank() },
                                    verified = provider.id in verifiedProviderIds,
                                )
                                V2AIChatStore.refreshConfigState()
                            }
                        },
                    )
                }

                if (profilePresentation) {
                    ManagementConnectionSummary(
                        provider = provider,
                        model = model,
                        verified = provider == V2AIService.ProviderId.BUILTIN || provider.id in verifiedProviderIds,
                        testing = testing,
                        testingChat = testingChat,
                        failed = lastTestFailed,
                        hasKey = provider == V2AIService.ProviderId.BUILTIN || !effectiveApiKey.isNullOrBlank(),
                        configurationReady = profileConfigurationReady,
                        primaryButtonTitle = primaryButtonTitle,
                        primaryButtonEnabled = buttonEnabled,
                        primaryButtonTone = buttonTone,
                        connectionResult = testResultMessage ?: saveError,
                        chatResult = chatTestResult,
                        chatResultFailed = chatTestFailed,
                        onPrimaryAction = performPrimaryAction,
                        onRevalidate = revalidateConnection,
                        onTestChat = testConversation,
                        onDisconnect = disconnectProvider.takeIf {
                            provider != V2AIService.ProviderId.BUILTIN && provider.id in verifiedProviderIds
                        },
                    )
                }

                if (!profilePresentation && testResultMessage != null) {
                    StatusBanner(testResultMessage.orEmpty(), success = !lastTestFailed)
                } else if (!profilePresentation && saveError != null) {
                    StatusBanner(saveError.orEmpty(), success = false)
                }

                if (!profilePresentation) {
                    AIConfigPrimaryButton(
                        title = primaryButtonTitle,
                        enabled = buttonEnabled,
                        tone = buttonTone,
                        testing = testing,
                        rounded = true,
                        onClick = performPrimaryAction,
                    )
                }
                if (!profilePresentation) {
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
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (!profilePresentation) {
            AIConfigFloatingCloseButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .v2StatusBarPadding()
                    .padding(start = 16.dp, top = 16.dp),
            )
        }
    }

    LaunchedEffect(configLoaded, activeProviderId) {
        if (configLoaded && focusedProviderId == null) focusedProviderId = activeProviderId
    }

    LaunchedEffect(provider.id) {
        val restoredModel = session.storedAIModel(provider.id)
            ?: if (provider.id == activeProviderId) activeModel else provider.defaultModel
        val restoredKey = if (provider == V2AIService.ProviderId.BUILTIN) "" else session.storedAIKey(provider.id).orEmpty()
        model = provider.modelOrDefault(restoredModel)
        baseUrl = if (provider.id == activeProviderId) {
            savedBase ?: provider.defaultBase.orEmpty()
        } else {
            provider.defaultBase.orEmpty()
        }
        storedApiKeyForProvider = restoredKey
        apiKey = if (profilePresentation) restoredKey else ""
        testResultMessage = null
        lastTestFailed = false
        chatTestResult = null
        chatTestFailed = false
        profileConfigDirty = false
        saveError = null
    }

    LaunchedEffect(activeProviderId, savedApiKey) {
        verifiedProviderIds = loadVerifiedAIProviders(session, providerOptions)
    }
}

private const val AI_CONFIG_LOADING_PROVIDER = "__loading__"
private const val AI_CONFIG_TEST_TIMEOUT_MS = 30_000L

internal data class AIConfigProviderWrite(
    val id: String,
    val baseUrl: String?,
    val apiKey: String?,
    val model: String?,
    val verified: Boolean,
)

internal object V2AIConfigTestHooks {
    @Volatile
    var streamChatOverride: ((List<Pair<String, String>>) -> Flow<String>)? = null

    @Volatile
    var connectivityOverride: (suspend () -> Unit)? = null

    @Volatile
    var timeoutMsOverride: Long? = null

    @Volatile
    var beforeProviderWriteOverride: (suspend (AIConfigProviderWrite) -> Unit)? = null

    fun reset() {
        streamChatOverride = null
        connectivityOverride = null
        timeoutMsOverride = null
        beforeProviderWriteOverride = null
    }
}

internal enum class AIConfigButtonTone { PRIMARY, WARNING, DANGER, NEUTRAL }

internal enum class AIConfigPrimaryState(
    val title: String,
    val enabled: Boolean,
    val tone: AIConfigButtonTone,
    val requiresConnectionTest: Boolean,
) {
    CURRENT_NO_EDIT("当前模型", false, AIConfigButtonTone.NEUTRAL, false),
    CURRENT_EDITED("测试并应用 →", true, AIConfigButtonTone.WARNING, true),
    SWITCH_VERIFIED("切换到该模型 →", true, AIConfigButtonTone.PRIMARY, false),
    SWITCH_VERIFIED_EDITED("测试并切换 →", true, AIConfigButtonTone.WARNING, true),
    NO_KEY("请填写 API Key", false, AIConfigButtonTone.NEUTRAL, false),
    UNVERIFIED_HAS_KEY("测试连接 →", true, AIConfigButtonTone.PRIMARY, true),
    SWITCH_BUILTIN("切换到挥呗 AI →", true, AIConfigButtonTone.PRIMARY, false),
    TESTING("测试中…", false, AIConfigButtonTone.PRIMARY, false),
    FAILED("重试 →", true, AIConfigButtonTone.DANGER, true),
}

internal fun aiConfigPrimaryState(
    focusedIsBuiltin: Boolean,
    focusedIsActive: Boolean,
    focusedIsDirty: Boolean,
    focusedIsVerified: Boolean,
    focusedHasEffectiveKey: Boolean,
    isTesting: Boolean,
    lastTestFailed: Boolean,
): AIConfigPrimaryState = when {
    isTesting -> AIConfigPrimaryState.TESTING
    lastTestFailed -> AIConfigPrimaryState.FAILED
    focusedIsBuiltin && focusedIsActive -> AIConfigPrimaryState.CURRENT_NO_EDIT
    focusedIsBuiltin -> AIConfigPrimaryState.SWITCH_BUILTIN
    focusedIsActive && focusedIsDirty -> AIConfigPrimaryState.CURRENT_EDITED
    focusedIsActive -> AIConfigPrimaryState.CURRENT_NO_EDIT
    focusedIsVerified && focusedIsDirty -> AIConfigPrimaryState.SWITCH_VERIFIED_EDITED
    focusedIsVerified -> AIConfigPrimaryState.SWITCH_VERIFIED
    !focusedHasEffectiveKey -> AIConfigPrimaryState.NO_KEY
    else -> AIConfigPrimaryState.UNVERIFIED_HAS_KEY
}

internal fun aiManagementPrimaryState(
    focusedIsBuiltin: Boolean,
    focusedHasEffectiveKey: Boolean,
    focusedIsVerified: Boolean,
    focusedIsDirty: Boolean,
    isTesting: Boolean,
    lastTestFailed: Boolean,
): AIConfigPrimaryState = when {
    isTesting -> AIConfigPrimaryState.TESTING
    lastTestFailed -> AIConfigPrimaryState.FAILED
    focusedIsBuiltin -> AIConfigPrimaryState.CURRENT_NO_EDIT
    !focusedHasEffectiveKey -> AIConfigPrimaryState.NO_KEY
    !focusedIsVerified -> AIConfigPrimaryState.UNVERIFIED_HAS_KEY
    focusedIsDirty -> AIConfigPrimaryState.CURRENT_EDITED
    else -> AIConfigPrimaryState.CURRENT_NO_EDIT
}

private data class AIConfigActiveSnapshot(
    val providerId: String,
    val baseUrl: String?,
    val apiKey: String?,
    val model: String,
    val verified: Boolean,
)

private suspend fun setAIProviderForConfig(
    session: com.mygolfswing.app.data.session.V2SessionStore,
    id: String,
    baseUrl: String?,
    apiKey: String?,
    model: String?,
    verified: Boolean,
) {
    val write = AIConfigProviderWrite(
        id = id,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        verified = verified,
    )
    V2AIConfigTestHooks.beforeProviderWriteOverride?.invoke(write)
    session.setAIProvider(
        id = id,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        verified = verified,
    )
}

private suspend fun restoreAIConfigSnapshot(
    session: com.mygolfswing.app.data.session.V2SessionStore,
    snapshot: AIConfigActiveSnapshot,
) {
    session.setAIProvider(
        id = snapshot.providerId,
        baseUrl = snapshot.baseUrl,
        apiKey = snapshot.apiKey,
        model = snapshot.model,
        verified = snapshot.verified,
    )
}

private suspend fun loadVerifiedAIProviders(
    session: com.mygolfswing.app.data.session.V2SessionStore,
    providers: List<V2AIService.ProviderId>,
): Set<String> =
    providers
        .filter { session.isAIProviderVerified(it.id) }
        .map { it.id }
        .toSet()

internal fun aiConfigError(base: String, debugDetail: String?): String {
    return aiConfigError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun aiConfigError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun aiConfigThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return aiConfigError(base, error.message)
}

private fun maskedAIKey(key: String): String {
    val trimmed = key.trim()
    if (trimmed.isBlank()) return ""
    val prefix = trimmed.take(3).takeIf { trimmed.length > 6 }.orEmpty()
    val suffix = trimmed.takeLast(minOf(4, trimmed.length))
    return "$prefix••••••••$suffix"
}

@Composable
private fun AIProfileConfigTopBar(onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite.copy(alpha = 0.96f))
            .v2StatusBarPadding()
            .height(44.dp)
            .testTag("ai-profile-config-top-bar"),
    ) {
        Text(
            "AI 接口配置",
            color = DSColors.inkGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "完成",
            color = DSColors.fairway,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onDone)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ManagementProviderGrid(
    providers: List<V2AIService.ProviderId>,
    selectedProvider: V2AIService.ProviderId,
    verifiedProviderIds: Set<String>,
    onSelect: (V2AIService.ProviderId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("选择供应商", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(206.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(providers, key = { it.id }) { item ->
                val selected = item == selectedProvider
                val verified = item == V2AIService.ProviderId.BUILTIN || item.id in verifiedProviderIds
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) DSColors.fairway else DSColors.mistGreen)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 6.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            item.displayName,
                            color = if (selected) Color.White else DSColors.inkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (verified) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(if (selected) Color.White else DSColors.fairway),
                            )
                        }
                    }
                    Text(
                        item.description,
                        color = if (selected) Color.White.copy(alpha = 0.85f) else DSColors.shadowGray,
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(DSColors.fairway))
            Text("已配置成功", fontSize = 11.sp, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun ManagementModelList(
    models: List<V2AIService.ModelOption>,
    selectedModel: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("选择模型", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        models.forEach { option ->
            val selected = option.id == selectedModel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) DSColors.fairway.copy(alpha = 0.10f) else DSColors.warmWhite)
                    .border(
                        1.dp,
                        if (selected) DSColors.fairway.copy(alpha = 0.40f) else DSColors.borderLine,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        option.displayName,
                        color = DSColors.inkGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(option.trait, color = DSColors.shadowGray, fontSize = 11.sp)
                }
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.60f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun BuiltinAIKeyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            Text("内置模式无需 API Key", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text("每天 30 条免费额度，开箱即用", color = DSColors.shadowGray, fontSize = 12.sp)
    }
}

@Composable
private fun ManagementApiKeyCard(
    value: String,
    keyHint: String,
    onValueChange: (String) -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("API Key", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DSColors.mistGreen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(12.dp),
                singleLine = true,
                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
                cursorBrush = Brush.verticalGradient(listOf(DSColors.fairway, DSColors.fairway)),
                visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text("粘贴你的 API Key", color = DSColors.shadowGray.copy(alpha = 0.65f), fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
            TextButton(
                onClick = { revealed = !revealed },
                modifier = Modifier.size(42.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (revealed) "隐藏 API Key" else "显示 API Key",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (keyHint.isNotBlank()) Text(keyHint, color = DSColors.shadowGray, fontSize = 12.sp)
    }
}

@Composable
private fun ManagementAdvancedConfig(
    baseUrl: String,
    model: String,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Settings, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
            Text(
                "高级配置",
                color = DSColors.shadowGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起高级配置" else "展开高级配置",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(16.dp),
            )
        }
        if (expanded) {
            ManagementTextField("Base URL", "https://api.example.com/v1", baseUrl, onBaseUrlChange)
            ManagementTextField("模型名称", "例如：gpt-4o-mini", model, onModelChange)
        }
    }
}

@Composable
private fun ManagementTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 13.sp),
            cursorBrush = Brush.verticalGradient(listOf(DSColors.fairway, DSColors.fairway)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DSColors.mistGreen)
                .padding(horizontal = 10.dp, vertical = 11.dp),
            decorationBox = { input ->
                Box {
                    if (value.isBlank()) Text(placeholder, color = DSColors.shadowGray.copy(alpha = 0.65f), fontSize = 13.sp)
                    input()
                }
            },
        )
    }
}

@Composable
private fun ManagementConnectionSummary(
    provider: V2AIService.ProviderId,
    model: String,
    verified: Boolean,
    testing: Boolean,
    testingChat: Boolean,
    failed: Boolean,
    hasKey: Boolean,
    configurationReady: Boolean,
    primaryButtonTitle: String,
    primaryButtonEnabled: Boolean,
    primaryButtonTone: Color,
    connectionResult: String?,
    chatResult: String?,
    chatResultFailed: Boolean,
    onPrimaryAction: () -> Unit,
    onRevalidate: () -> Unit,
    onTestChat: () -> Unit,
    onDisconnect: (() -> Unit)?,
) {
    var advancedExpanded by remember { mutableStateOf(false) }
    val displayModel = provider.models.firstOrNull { it.id == model }?.displayName
        ?: model.ifBlank { "未选模型" }
    val color = when {
        failed -> DSColors.flagRed
        testing -> DSColors.fairway
        verified -> DSColors.fairway
        else -> DSColors.shadowGray
    }
    val title = when {
        failed -> "验证失败 · ${provider.displayName} · $displayModel"
        testing -> "测试中 · ${provider.displayName} · $displayModel"
        provider == V2AIService.ProviderId.BUILTIN -> "已就绪 · ${provider.displayName} · $displayModel"
        verified -> "已连接 · ${provider.displayName} · $displayModel"
        else -> "待验证 · ${provider.displayName} · $displayModel"
    }
    val subtitle = when {
        failed -> connectionResult ?: "连接失败，请检查配置后重试"
        testing -> "正在向服务端验证…"
        provider == V2AIService.ProviderId.BUILTIN -> "内置免费模型，每天 30 条额度"
        verified -> "已验证可用"
        !configurationReady -> "请先完善 Base URL 与模型名称"
        hasKey -> "已填写配置，点击下方测试连接"
        else -> "请先填写 API Key"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.WifiTethering, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text("当前模型", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                when {
                    failed -> Icons.Filled.Cancel
                    testing -> Icons.Filled.Refresh
                    provider == V2AIService.ProviderId.BUILTIN -> Icons.Filled.Shield
                    verified -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.RadioButtonUnchecked
                },
                null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = DSColors.shadowGray, fontSize = 11.sp)
            }
        }

        if (connectionResult != null && failed) {
            StatusBanner(connectionResult, success = false)
        }
        if (chatResult != null) {
            StatusBanner(chatResult, success = !chatResultFailed)
        }

        AIConfigPrimaryButton(
            title = primaryButtonTitle,
            enabled = primaryButtonEnabled,
            tone = primaryButtonTone,
            testing = testing,
            rounded = false,
            onClick = onPrimaryAction,
        )

        if (provider != V2AIService.ProviderId.BUILTIN) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { advancedExpanded = !advancedExpanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Settings, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                Text(
                    "高级测试",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
                Icon(
                    if (advancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (advancedExpanded) "收起高级测试" else "展开高级测试",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(15.dp),
                )
            }
            if (advancedExpanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ManagementTestButton(
                        title = "重新验证",
                        icon = Icons.Filled.Bolt,
                        loading = testing,
                        enabled = hasKey && configurationReady && !testing && !testingChat,
                        modifier = Modifier.weight(1f),
                        onClick = onRevalidate,
                    )
                    ManagementTestButton(
                        title = "测试对话",
                        icon = Icons.Filled.Textsms,
                        loading = testingChat,
                        enabled = verified && !testing && !testingChat,
                        modifier = Modifier.weight(1f),
                        onClick = onTestChat,
                    )
                }
            }
        }

        if (onDisconnect != null) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDisconnect).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Cancel, null, tint = DSColors.flagRed.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                Text(
                    "解除连接",
                    color = DSColors.flagRed.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ManagementTestButton(
    title: String,
    icon: ImageVector,
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.mistGreen)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = DSColors.inkGreen,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(12.dp),
            )
        } else {
            Icon(icon, null, tint = DSColors.inkGreen, modifier = Modifier.size(12.dp))
        }
        Text(
            title,
            color = DSColors.inkGreen.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun AIConfigPrimaryButton(
    title: String,
    enabled: Boolean,
    tone: Color,
    testing: Boolean,
    rounded: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (rounded) 24.dp else 14.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = tone,
            disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .shadow(
                elevation = if (enabled) if (rounded) 10.dp else 8.dp else 0.dp,
                shape = shape,
                ambientColor = tone.copy(alpha = 0.30f),
                spotColor = tone.copy(alpha = 0.30f),
            )
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (testing) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(8.dp))
        } else if (!rounded) {
            Icon(
                when {
                    title == "开始对话" -> Icons.Filled.CheckCircle
                    title.contains("重试") || title.contains("重新") -> Icons.Filled.Refresh
                    else -> Icons.Filled.Bolt
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(title, style = DSTypography.bodyBold, color = Color.White)
    }
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
    maskedKey: String,
    keyHint: String,
    managementStyle: Boolean,
    onValueChange: (String) -> Unit,
    onDisconnect: (() -> Unit)?,
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
            Text(
                if (managementStyle) "API Key" else "输入 API Key",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
            )
            Spacer(Modifier.weight(1f))
            if (!managementStyle) {
                Text("安全加密存储", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6C757D))
            }
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
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai-config-api-key-input"),
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
                            if (!revealed && maskedKey.isNotBlank()) maskedKey else "sk-...",
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6C757D),
            )
        }
        if (managementStyle && keyHint.isNotBlank()) {
            Text(keyHint, color = DSColors.shadowGray, fontSize = 12.sp)
        }
        if (managementStyle && onDisconnect != null) {
            Text(
                "⊗  解除连接",
                color = DSColors.flagRed.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onDisconnect).padding(vertical = 2.dp),
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
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = DSColors.fairway.copy(alpha = 0.15f),
                    spotColor = DSColors.fairway.copy(alpha = 0.15f),
                )
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
                .widthIn(max = 300.dp),
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
            Text("选择 AI 供应商", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Spacer(Modifier.weight(1f))
            Text("点击查看", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6C757D))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(172.dp),
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
                    selected -> Color(0xFFD8E2DC).copy(alpha = 0.30f)
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
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(DSColors.fairway)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(8.dp))
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
                Canvas(Modifier.size(12.dp)) {
                    drawCircle(
                        color = DSColors.fairway,
                        radius = size.minDimension / 2f - 0.75.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(2.dp.toPx(), 1.5.dp.toPx()),
                            ),
                        ),
                    )
                }
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
                    .padding(end = 4.dp, bottom = 4.dp)
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
    showAvailableCount: Boolean = true,
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
            Text(
                "选择模型",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                modifier = Modifier.weight(1f),
            )
            if (showAvailableCount) {
                Text("${models.size} 个可用", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6C757D))
            }
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
