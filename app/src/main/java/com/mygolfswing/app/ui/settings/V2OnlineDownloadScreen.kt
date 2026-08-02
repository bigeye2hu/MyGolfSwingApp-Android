package com.mygolfswing.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.core.content.edit
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.repo.V2DownloadStartResult
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun V2OnlineDownloadScreen(
    onBack: () -> Unit,
    onMemoCreated: (UUID) -> Unit,
    initialUrl: String = "",
    allowDirectUrlDownload: Boolean = false,
    onRequestLogin: () -> Unit = {},
    onOpenOnlineMemo: (String) -> Unit = {},
    onRequestLoginForOnlineMemo: (String) -> Unit = { onRequestLogin() },
    onOpenHistory: () -> Unit = {},
    onOpenCreditsReset: () -> Unit = {},
    onOpenCreditsPurchase: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by V2ProfileStore.profile.collectAsState()
    var urlInput by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    var startResult by remember { mutableStateOf<V2DownloadStartResult?>(null) }
    var videoInfo by remember { mutableStateOf<DownloadVideoInfo?>(null) }
    var phase by remember { mutableStateOf(DownloadPhase.IDLE) }
    var error by remember { mutableStateOf<String?>(null) }
    var phaseNote by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableDoubleStateOf(0.0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var createdMemoId by remember { mutableStateOf<UUID?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showAsyncDownloadTip by remember { mutableStateOf(false) }
    var asyncTipAcceptedJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var creditMessage by remember { mutableStateOf<String?>(null) }
    var refreshingCredits by remember { mutableStateOf(false) }
    val memos by V2MemoStore.allMemos.collectAsState()
    val downloadStats = remember(memos) { DownloadStats.from(memos) }
    val canAffordDownload = profile.isLoggedIn && profile.credits >= DOWNLOAD_COST_CREDITS
    val cleanedUrl = remember(urlInput) { V2OnlineVideoMetadataFetcher.cleanVideoUrl(urlInput) }
    val detectedPlatform = remember(cleanedUrl) {
        if (cleanedUrl.isBlank()) "" else V2OnlineVideoMetadataFetcher.detectPlatform(cleanedUrl)
    }
    val isDouyinDownloadUrl = detectedPlatform == "抖音"
    val isDirectDownloadUrl = allowDirectUrlDownload && cleanedUrl.isHttpUrl()
    val canCreateOnlineMemo = !allowDirectUrlDownload && cleanedUrl.isHttpUrl() && detectedPlatform in ONLINE_MEMO_PLATFORMS
    val canCreateDirectOnlineMemo = allowDirectUrlDownload && cleanedUrl.isHttpUrl() && phase == DownloadPhase.IDLE
    val hasDownloadableUrl = urlInput.isNotBlank() && (isDouyinDownloadUrl || isDirectDownloadUrl)
    val canStartDownload = hasDownloadableUrl
    val draftScopeKey = remember(profile.userId, allowDirectUrlDownload) {
        downloadDraftKey(profile.userId, allowDirectUrlDownload)
    }
    val anonymousDraftKey = remember(allowDirectUrlDownload) {
        downloadDraftKey(userId = null, directMode = allowDirectUrlDownload)
    }

    fun resetForNewUrl() {
        clearPendingDownloadTask(context, profile.userId)
        startResult = null
        videoInfo = null
        error = null
        phaseNote = null
        progress = 0.0
        elapsedSeconds = 0
        createdMemoId = null
        showSuccessDialog = false
        phase = DownloadPhase.IDLE
    }

    fun resetAfterSuccess() {
        clearPendingDownloadTask(context, profile.userId)
        clearDownloadDraftUrl(context, draftScopeKey)
        urlInput = ""
        startResult = null
        videoInfo = null
        error = null
        phaseNote = null
        progress = 0.0
        elapsedSeconds = 0
        createdMemoId = null
        showSuccessDialog = false
        phase = DownloadPhase.IDLE
    }

    LaunchedEffect(phase) {
        if (phase !in timedDownloadPhases) return@LaunchedEffect
        while (true) {
            delay(1_000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(profile.userId, profile.isLoggedIn, initialUrl) {
        val userId = profile.userId?.takeIf { profile.isLoggedIn && it.isNotBlank() } ?: return@LaunchedEffect
        if (initialUrl.isNotBlank() || startResult != null || phase != DownloadPhase.IDLE) return@LaunchedEffect
        val pending = restorePendingDownloadTask(context, userId) ?: return@LaunchedEffect
        urlInput = pending.url
        startResult = pending.start
        videoInfo = pending.info
        error = null
        phaseNote = "已恢复上次未完成的下载任务，可继续确认或取消返还积分。"
        progress = 0.0
        createdMemoId = null
        phase = DownloadPhase.READY
    }

    LaunchedEffect(draftScopeKey, anonymousDraftKey, profile.isLoggedIn, initialUrl, phase) {
        if (initialUrl.isNotBlank() || urlInput.isNotBlank() || phase != DownloadPhase.IDLE) return@LaunchedEffect
        val restored = restoreDownloadDraftUrl(context, draftScopeKey)
            ?: if (profile.isLoggedIn && draftScopeKey != anonymousDraftKey) {
                restoreDownloadDraftUrl(context, anonymousDraftKey)
            } else {
                null
            }
        restored?.let {
            urlInput = it
            if (draftScopeKey != anonymousDraftKey) {
                persistDownloadDraftUrl(context, draftScopeKey, it)
                clearDownloadDraftUrl(context, anonymousDraftKey)
            }
        }
    }

    LaunchedEffect(draftScopeKey, cleanedUrl, phase, startResult, createdMemoId) {
        if (shouldPersistDownloadDraft(phase, startResult, createdMemoId)) {
            persistDownloadDraftUrl(context, draftScopeKey, cleanedUrl)
        }
    }

    fun refreshCredits() {
        scope.launch {
            refreshingCredits = true
            creditMessage = null
            val balance = ServiceLocator.creditsRepo.fetchBalance(force = true)
            V2ProfileStore.refresh(force = true)
            creditMessage = if (balance == null) {
                "积分刷新失败，请稍后重试"
            } else {
                "已刷新：当前 $balance 积分"
            }
            refreshingCredits = false
        }
    }

    fun startDownload() {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(urlInput)
        if (cleaned.isBlank()) {
            error = "请先粘贴视频链接"
            return
        }
        if (!profile.isLoggedIn) {
            onRequestLogin()
            return
        }
        val platform = V2OnlineVideoMetadataFetcher.detectPlatform(cleaned)
        if (platform != "抖音" && !allowDirectUrlDownload) {
            error = if (platform in ONLINE_MEMO_PLATFORMS) {
                "${platform}链接暂不支持下载，可改为添加在线视频 Memo"
            } else {
                "当前仅支持抖音视频下载"
            }
            return
        }
        if (!canAffordDownload) {
            creditMessage = "当前积分不足，下载需要 $DOWNLOAD_COST_CREDITS 积分。请先充值；若余额显示异常，再刷新或校准。"
            return
        }
        urlInput = cleaned
        clearDownloadDraftUrl(context, draftScopeKey)
        scope.launch {
            error = null
            phaseNote = null
            elapsedSeconds = 0
            phase = DownloadPhase.FETCHING
            val result = try {
                ServiceLocator.videoDownloadRepo.start(cleaned)
            } catch (e: CancellationException) {
                phase = downloadPhaseAfterCancellation(phase, startResult != null)
                phaseNote = null
                throw e
            } catch (e: Throwable) {
                downloadResultFailure(e)
            }
            try {
                result.onSuccess { start ->
                    startResult = start
                    videoInfo = DownloadVideoInfo.from(start.videoInfo, cleaned, start.platform)
                    persistPendingDownloadTask(
                        context = context,
                        userId = profile.userId,
                        url = cleaned,
                        start = start,
                    )
                    phase = DownloadPhase.READY
                    ServiceLocator.creditsRepo.fetchBalance(force = true)
                    V2ProfileStore.refresh(force = true)
                }.onFailure { e ->
                    error = downloadThrowableError("无法获取视频信息，请检查链接后重试", e)
                    phase = DownloadPhase.IDLE
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = downloadThrowableError("无法获取视频信息，请检查链接后重试", e)
                phase = DownloadPhase.IDLE
            }
        }
    }

    fun cancelDownload() {
        val jobId = startResult?.jobId ?: return
        scope.launch {
            error = null
            phaseNote = null
            phase = DownloadPhase.CANCELLING
            val cancel = try {
                ServiceLocator.videoDownloadRepo.cancel(jobId)
            } catch (e: CancellationException) {
                phase = downloadPhaseAfterCancellation(phase, startResult != null)
                phaseNote = null
                throw e
            } catch (e: Throwable) {
                downloadResultFailure(e)
            }
            try {
                ServiceLocator.creditsRepo.fetchBalance(force = true)
                V2ProfileStore.refresh(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Balance refresh is a recovery nicety; the cancel result still drives the task state.
            }
            cancel.onSuccess {
                resetForNewUrl()
                creditMessage = if (it.creditsRefunded > 0) {
                    "已取消，返还 ${it.creditsRefunded} 积分"
                } else {
                    "已取消下载任务，已刷新当前积分余额"
                }
            }.onFailure { e ->
                error = downloadThrowableError("取消失败，请稍后重试；如已扣积分，请稍后刷新余额", e)
                phase = DownloadPhase.READY
            }
        }
    }

    fun confirmDownload() {
        val start = startResult ?: return
        if (shouldSuggestAsyncDownload(videoInfo, start.jobId, asyncTipAcceptedJobId)) {
            showAsyncDownloadTip = true
            return
        }
        scope.launch {
            error = null
            phaseNote = null
            phase = DownloadPhase.CONFIRMING
            try {
                val confirm = ServiceLocator.videoDownloadRepo.confirm(start.jobId)
                confirm.onSuccess { confirmed ->
                    if (!confirmed.success) {
                        error = downloadErrorMessage("下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分", confirmed.message)
                        phase = DownloadPhase.READY
                        return@onSuccess
                    }
                    ServiceLocator.creditsRepo.fetchBalance(force = true)
                    V2ProfileStore.refresh(force = true)
                    val confirmedInfo = DownloadVideoInfo.from(confirmed.videoInfo, urlInput, start.platform)
                    val effective = confirmedInfo.mergeWith(videoInfo)
                    videoInfo = effective
                    val downloadUrl = effective.downloadUrl
                    if (!downloadUrl.isNullOrBlank()) {
                        phase = DownloadPhase.DOWNLOADING
                        val imported = V2VideoFileStore.downloadRemoteVideo(context, downloadUrl) { p ->
                            if (p >= 0.0) progress = p
                        }
                        if (imported == null) {
                            createOnlineMemo(effective)?.let {
                                phaseNote = "本地下载失败，已改为在线视频 Memo，可继续播放和复盘。"
                                createdMemoId = it
                                showSuccessDialog = true
                                clearPendingDownloadTask(context, profile.userId)
                                phase = DownloadPhase.COMPLETED
                            } ?: run {
                                error = "未能保存视频，也没有可播放的在线地址"
                                phase = DownloadPhase.READY
                            }
                        } else {
                            progress = 1.0
                            createdMemoId = createLocalMemo(imported, effective)
                            showSuccessDialog = true
                            clearPendingDownloadTask(context, profile.userId)
                            phase = DownloadPhase.COMPLETED
                        }
                    } else {
                        createOnlineMemo(effective)?.let {
                            phaseNote = "下载服务未返回本地下载地址，已创建在线视频 Memo。"
                            createdMemoId = it
                            showSuccessDialog = true
                            clearPendingDownloadTask(context, profile.userId)
                            phase = DownloadPhase.COMPLETED
                        } ?: run {
                            error = "下载服务暂时没有返回可播放地址，请稍后重试"
                            phase = DownloadPhase.READY
                        }
                    }
                }.onFailure { e ->
                    error = downloadThrowableError("下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分", e)
                    phase = DownloadPhase.READY
                }
            } catch (e: CancellationException) {
                phase = downloadPhaseAfterCancellation(phase, startResult != null)
                phaseNote = null
                throw e
            } catch (e: Throwable) {
                error = downloadThrowableError("下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分", e)
                phase = DownloadPhase.READY
            }
        }
    }

    fun createDirectOnlineMemoFallback() {
        if (!profile.isLoggedIn) {
            onRequestLogin()
            return
        }
        val url = cleanedUrl
        if (!url.isHttpUrl()) {
            error = "需要有效的视频播放地址"
            return
        }
        scope.launch {
            error = null
            phaseNote = "正在改存为在线视频 Memo..."
            val info = DownloadVideoInfo(
                title = videoInfo?.title?.takeIf { it.isNotBlank() } ?: "广场视频",
                author = videoInfo?.author,
                thumbnailUrl = videoInfo?.thumbnailUrl,
                durationSec = videoInfo?.durationSec,
                downloadUrl = null,
                playUrl = url,
                platform = "视频广场",
            )
            val memoId = try {
                createOnlineMemo(info)
            } catch (e: CancellationException) {
                phaseNote = null
                throw e
            } catch (e: Throwable) {
                null
            }
            if (memoId == null) {
                error = "无法创建在线视频 Memo，请检查视频地址后重试"
                phaseNote = null
            } else {
                createdMemoId = memoId
                showSuccessDialog = true
                phaseNote = "下载服务暂不可用时，已先保存为在线视频 Memo，可继续播放和复盘。"
                phase = DownloadPhase.COMPLETED
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBar(
            title = "视频下载",
            onBack = onBack,
            trailing = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DSColors.cardWhite)
                        .clickable(onClick = onOpenHistory),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.History, contentDescription = "下载记录", tint = DSColors.fairway, modifier = Modifier.size(20.dp))
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
        ) {
            DownloadHero(isDirectUrlMode = allowDirectUrlDownload)
            CreditCard(
                balance = profile.credits,
                isLoggedIn = profile.isLoggedIn,
                refreshing = refreshingCredits,
                message = creditMessage,
                onLogin = onRequestLogin,
                onRefresh = ::refreshCredits,
                onOpenCreditsReset = onOpenCreditsReset,
                onOpenCreditsPurchase = onOpenCreditsPurchase,
            )
            UrlCard(
                url = urlInput,
                enabled = phase == DownloadPhase.IDLE || phase == DownloadPhase.READY,
                placeholder = if (allowDirectUrlDownload) "粘贴广场视频播放地址..." else "粘贴抖音视频分享链接...",
                onUrlChange = {
                    urlInput = it
                    resetForNewUrl()
                },
                onPaste = {
                    val pasted = context.readClipboardText()
                    if (pasted == null) {
                        error = "剪贴板没有可用链接，请手动粘贴"
                        phaseNote = null
                    } else {
                        urlInput = pasted
                        resetForNewUrl()
                        phaseNote = "已从剪贴板粘贴链接"
                    }
                },
                onCopy = {
                    if (urlInput.isBlank()) {
                        error = "请先输入视频链接"
                        phaseNote = null
                    } else if (context.copyTextToClipboard("video-url", urlInput)) {
                        error = null
                        phaseNote = "链接已复制"
                    } else {
                        error = "复制失败，请稍后重试"
                        phaseNote = null
                    }
                },
            )
            if (canCreateOnlineMemo) {
                OnlineMemoFallbackCard(
                    platform = detectedPlatform,
                    onOpenOnlineMemo = {
                        if (profile.isLoggedIn) onOpenOnlineMemo(cleanedUrl) else onRequestLoginForOnlineMemo(cleanedUrl)
                    },
                )
            }
            if (allowDirectUrlDownload) {
                DirectDownloadSourceSection()
            } else {
                PlatformSupportSection()
            }
            if (canCreateDirectOnlineMemo) {
                DirectOnlineMemoFallbackCard(
                    hasError = error != null,
                    onOpenOnlineMemo = ::createDirectOnlineMemoFallback,
                )
            }
            startResult?.let { start ->
                VideoInfoCard(
                    info = videoInfo,
                    costCredits = start.costCredits,
                    platform = start.platform,
                )
            }
            PhaseCard(phase = phase, progress = progress, elapsedSeconds = elapsedSeconds, error = error, note = phaseNote)
            TodaySummaryCard(stats = downloadStats)
            SupportedDownloadCard(isDirectUrlMode = allowDirectUrlDownload)
            ActionRow(
                phase = phase,
                canStart = canStartDownload,
                startActionText = when {
                    !profile.isLoggedIn && hasDownloadableUrl -> "登录后下载"
                    profile.isLoggedIn && !canAffordDownload && hasDownloadableUrl -> "去充值后下载"
                    else -> "获取视频信息"
                },
                onlineMemoActionText = if (canCreateOnlineMemo && phase == DownloadPhase.IDLE) {
                    if (profile.isLoggedIn) "添加在线视频 Memo" else "登录后添加在线视频 Memo"
                } else {
                    null
                },
                startDisabledReason = when {
                    !profile.isLoggedIn && canCreateOnlineMemo -> "登录后可添加在线视频 Memo"
                    !profile.isLoggedIn -> "登录后可下载"
                    urlInput.isBlank() -> "先粘贴视频链接"
                    allowDirectUrlDownload && !cleanedUrl.isHttpUrl() -> "需要有效的视频播放地址"
                    !isDouyinDownloadUrl && canCreateOnlineMemo -> "${detectedPlatform}链接可改为添加在线视频 Memo"
                    !isDouyinDownloadUrl && !allowDirectUrlDownload -> "当前仅支持抖音视频下载"
                    !canAffordDownload -> "积分不足，先去充值；余额异常再刷新或校准"
                    else -> null
                },
                canConfirm = startResult != null,
                createdMemoId = createdMemoId,
                onStart = {
                    if (profile.isLoggedIn && !canAffordDownload && hasDownloadableUrl) {
                        persistDownloadDraftUrl(context, draftScopeKey, cleanedUrl)
                        onOpenCreditsPurchase()
                    } else {
                        startDownload()
                    }
                },
                onCancel = ::cancelDownload,
                onConfirm = ::confirmDownload,
                onOpenMemo = { createdMemoId?.let(onMemoCreated) },
                onOpenOnlineMemo = {
                    if (profile.isLoggedIn) onOpenOnlineMemo(cleanedUrl) else onRequestLoginForOnlineMemo(cleanedUrl)
                },
            )
            FooterNote()
        }
    }

    error?.takeIf { phase == DownloadPhase.IDLE && !allowDirectUrlDownload }?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("下载失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text("知道了") }
            },
        )
    }

    if (showSuccessDialog && createdMemoId != null) {
        DownloadSuccessDialog(
            platform = videoInfo?.platform?.takeIf { it.isNotBlank() } ?: if (allowDirectUrlDownload) "视频广场" else "在线视频",
            costCredits = startResult?.costCredits,
            isOnlineMemo = videoInfo?.downloadUrl.isNullOrBlank() || phaseNote?.contains("在线") == true,
            onOpenMemo = {
                showSuccessDialog = false
                createdMemoId?.let(onMemoCreated)
            },
            onContinue = ::resetAfterSuccess,
            onDismiss = { showSuccessDialog = false },
        )
    }

    startResult?.let { start ->
        if (showAsyncDownloadTip) {
            AsyncDownloadTipDialog(
                info = videoInfo,
                onLater = {
                    showAsyncDownloadTip = false
                    phaseNote = "已保留待确认任务，再次进入视频下载会自动恢复。"
                    onBack()
                },
                onContinue = {
                    asyncTipAcceptedJobId = start.jobId
                    showAsyncDownloadTip = false
                    confirmDownload()
                },
                onCancel = { showAsyncDownloadTip = false },
            )
        }
    }
}

@Composable
fun V2DownloadHistoryScreen(
    onBack: () -> Unit,
    onOpenMemo: (UUID) -> Unit,
    onStartDownload: () -> Unit,
    isLoggedIn: Boolean = true,
    onRequestLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val profile by V2ProfileStore.profile.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
    val items = remember(memos) {
        memos.filter { it.isOnlineDownloadMemo() }.sortedByDescending { it.createdAt }
    }
    val pendingTask = remember(profile.userId, isLoggedIn) {
        profile.userId
            ?.takeIf { isLoggedIn && it.isNotBlank() }
            ?.let { restorePendingDownloadTask(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBar(title = "下载记录", onBack = onBack)
        if (!isLoggedIn) {
            DownloadHistoryLoginRequired(onLogin = onRequestLogin)
        } else if (items.isEmpty() && pendingTask == null) {
            DownloadHistoryEmpty(onStartDownload = onStartDownload)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .v2NavigationBarPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = DSSpacing.lg,
                    end = DSSpacing.lg,
                    top = DSSpacing.md,
                    bottom = DSSpacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pendingTask?.let { task ->
                    item {
                        PendingDownloadTaskCard(task = task, onContinue = onStartDownload)
                    }
                }
                item {
                    DownloadHistorySummary(items = items)
                }
                items(items, key = { it.id }) { memo ->
                    DownloadHistoryRow(memo = memo, onClick = { onOpenMemo(memo.id) })
                }
            }
        }
    }
}

@Composable
private fun PendingDownloadTaskCard(task: PendingDownloadTask, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CloudDownload, null, tint = DSColors.fairway, modifier = Modifier.size(21.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("待处理下载任务", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                Text(
                    "${task.start.platform ?: task.info.platform ?: "视频平台"} · ${task.start.costCredits} 积分 · 待确认",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        }
        Text(
            task.info.title,
            style = DSTypography.body,
            color = DSColors.inkGreen,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "任务已保留在本机。继续处理会回到下载页，用户可确认下载或取消返还积分。",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
        PrimaryAction("继续处理", enabled = true, onClick = onContinue)
    }
}

@Composable
private fun DownloadHistoryLoginRequired(onLogin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding()
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
            }
            Text("登录后查看下载记录", style = DSTypography.titleMedium, color = DSColors.inkGreen)
            Text(
                "下载任务和在线视频 Memo 会跟随账号同步，登录后再继续查看。",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
            PrimaryAction("去登录", enabled = true, onClick = onLogin)
        }
    }
}

internal enum class DownloadPhase {
    IDLE, FETCHING, READY, CONFIRMING, DOWNLOADING, CANCELLING, COMPLETED
}

private val timedDownloadPhases = setOf(
    DownloadPhase.FETCHING,
    DownloadPhase.CONFIRMING,
    DownloadPhase.DOWNLOADING,
    DownloadPhase.CANCELLING,
)

private const val DOWNLOAD_COST_CREDITS = 10
private const val PENDING_DOWNLOAD_PREFS = "v2_pending_video_download"
private const val DOWNLOAD_DRAFT_PREFIX = "draft"
private val ONLINE_MEMO_PLATFORMS = setOf("哔哩哔哩", "YouTube", "小红书")

internal data class PendingDownloadTask(
    val url: String,
    val start: V2DownloadStartResult,
    val info: DownloadVideoInfo,
)

private fun persistPendingDownloadTask(
    context: Context,
    userId: String?,
    url: String,
    start: V2DownloadStartResult,
) {
    val uid = userId?.takeIf { it.isNotBlank() } ?: return
    val payload = JsonObject(
        mapOf(
            "url" to JsonPrimitive(url),
            "jobId" to JsonPrimitive(start.jobId),
            "platform" to jsonStringOrNull(start.platform),
            "costCredits" to JsonPrimitive(start.costCredits),
            "status" to JsonPrimitive(start.status),
            "message" to JsonPrimitive(start.message),
            "videoInfo" to (start.videoInfo ?: JsonNull),
        ),
    ).toString()
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(pendingDownloadKey(uid), payload)
        }
}

private fun restorePendingDownloadTask(context: Context, userId: String): PendingDownloadTask? {
    val raw = context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .getString(pendingDownloadKey(userId), null)
        ?: return null
    return parsePendingDownloadTask(raw)
}

internal fun parsePendingDownloadTask(raw: String): PendingDownloadTask? = runCatching {
    val obj = Json.parseToJsonElement(raw).jsonObject
    val url = obj["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@runCatching null
    val start = V2DownloadStartResult(
        jobId = obj["jobId"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@runCatching null,
        platform = obj["platform"]?.jsonPrimitive?.contentOrNull,
        costCredits = obj["costCredits"]?.jsonPrimitive?.intOrNull ?: DOWNLOAD_COST_CREDITS,
        status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "",
        message = obj["message"]?.jsonPrimitive?.contentOrNull ?: "",
        videoInfo = obj["videoInfo"]?.takeUnless { it is JsonNull },
    )
    PendingDownloadTask(
        url = url,
        start = start,
        info = DownloadVideoInfo.from(start.videoInfo, url, start.platform),
    )
}.getOrNull()

private fun clearPendingDownloadTask(context: Context, userId: String?) {
    val uid = userId?.takeIf { it.isNotBlank() } ?: return
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .edit {
            remove(pendingDownloadKey(uid))
        }
}

private fun pendingDownloadKey(userId: String): String = "pending_$userId"

private fun downloadDraftKey(userId: String?, directMode: Boolean): String =
    "$DOWNLOAD_DRAFT_PREFIX:${userId?.takeIf { it.isNotBlank() } ?: "anonymous"}:${if (directMode) "direct" else "normal"}"

internal fun shouldPersistDownloadDraft(
    phase: DownloadPhase,
    startResult: V2DownloadStartResult?,
    createdMemoId: UUID?,
): Boolean =
    phase == DownloadPhase.IDLE && startResult == null && createdMemoId == null

internal fun downloadPhaseAfterCancellation(currentPhase: DownloadPhase, hasActiveTask: Boolean): DownloadPhase {
    if (currentPhase !in timedDownloadPhases) return currentPhase
    return if (hasActiveTask) DownloadPhase.READY else DownloadPhase.IDLE
}

private fun persistDownloadDraftUrl(context: Context, key: String, url: String) {
    val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(url)
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE).edit {
        if (cleaned.isBlank()) {
            remove(key)
        } else {
            putString(key, cleaned)
        }
    }
}

private fun restoreDownloadDraftUrl(context: Context, key: String): String? =
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .getString(key, null)
        ?.takeIf { it.isNotBlank() }

private fun clearDownloadDraftUrl(context: Context, key: String) {
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .edit {
            remove(key)
        }
}

private fun jsonStringOrNull(value: String?): JsonElement =
    value?.takeIf { it.isNotBlank() }?.let { JsonPrimitive(it) } ?: JsonNull

internal data class DownloadVideoInfo(
    val title: String,
    val author: String?,
    val thumbnailUrl: String?,
    val durationSec: Double?,
    val downloadUrl: String?,
    val playUrl: String?,
    val platform: String?,
) {
    fun mergeWith(old: DownloadVideoInfo?): DownloadVideoInfo = DownloadVideoInfo(
        title = title.takeIf { it.isNotBlank() } ?: old?.title.orEmpty(),
        author = author ?: old?.author,
        thumbnailUrl = thumbnailUrl ?: old?.thumbnailUrl,
        durationSec = durationSec ?: old?.durationSec,
        downloadUrl = downloadUrl ?: old?.downloadUrl,
        playUrl = playUrl ?: old?.playUrl,
        platform = platform ?: old?.platform,
    )

    companion object {
        fun from(element: JsonElement?, originalUrl: String, platform: String?): DownloadVideoInfo {
            val title = findString(element, "title", "desc", "description", "caption", "aweme_desc")
                ?: "下载视频"
            val author = findString(element, "author", "nickname", "user_name", "unique_id")
            val thumb = findUrl(element, "cover", "cover_url", "thumbnail", "thumbnail_url", "origin_cover", "dynamic_cover")
            val download = findUrl(
                element,
                "no_watermark_url",
                "noWatermarkVideoURL",
                "no_watermark_video_url",
                "download_url",
                "downloadAddr",
                "download_addr",
                "play_addr",
                "url_list",
                "highestQualityVideoURL",
            )
            val play = findUrl(element, "play_url", "playURL", "onlinePlayURL", "url", "share_url") ?: originalUrl
            val duration = findDuration(element)
            return DownloadVideoInfo(
                title = title,
                author = author,
                thumbnailUrl = thumb,
                durationSec = duration,
                downloadUrl = download,
                playUrl = play,
                platform = platform,
            )
        }
    }
}

private data class DownloadStats(
    val todayCount: Int,
    val last30DaysCount: Int,
    val localSavedCount: Int,
) {
    companion object {
        fun from(memos: List<V2Memo>): DownloadStats {
            val today = LocalDate.now()
            val windowStart = today.minusDays(29)
            val downloaded = memos.filter { it.isOnlineDownloadMemo() }
            return DownloadStats(
                todayCount = downloaded.count { it.createdAt.toLocalDate() == today },
                last30DaysCount = downloaded.count {
                    val date = it.createdAt.toLocalDate()
                    !date.isBefore(windowStart) && !date.isAfter(today)
                },
                localSavedCount = downloaded.count { it.videoUrl?.startsWith("file://") == true },
            )
        }
    }
}

@Composable
private fun DownloadHistoryEmpty(onStartDownload: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding()
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.History, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
            }
            Text("还没有下载记录", style = DSTypography.titleMedium, color = DSColors.inkGreen)
            Text(
                "下载完成或添加在线视频后，会在这里集中管理。",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
            PrimaryAction("去下载视频", enabled = true, onClick = onStartDownload)
        }
    }
}

@Composable
private fun DownloadHistorySummary(items: List<V2Memo>) {
    val localCount = items.count { it.videoUrl?.startsWith("file://") == true }
    val onlineCount = items.size - localCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111827))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadHistoryMetric("全部", items.size.toString(), DSColors.trophyGold, Modifier.weight(1f))
        CreditDivider()
        DownloadHistoryMetric("本地", localCount.toString(), Color(0xFF10B981), Modifier.weight(1f))
        CreditDivider()
        DownloadHistoryMetric("在线", onlineCount.toString(), Color(0xFF60A5FA), Modifier.weight(1f))
    }
}

@Composable
private fun DownloadHistoryMetric(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}

@Composable
private fun DownloadHistoryRow(memo: V2Memo, onClick: () -> Unit) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("M月d日 HH:mm") }
    val source = memo.platform?.takeIf { it.isNotBlank() }
        ?: memo.trainingMethod?.takeIf { it.isNotBlank() }
        ?: "在线视频"
    val isLocal = memo.videoUrl?.startsWith("file://") == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 62.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            if (!memo.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = memo.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.VideoLibrary, null, tint = Color.White.copy(alpha = 0.84f), modifier = Modifier.size(28.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                memo.displayTitle ?: memo.title ?: "下载视频",
                style = DSTypography.bodyBold,
                color = DSColors.inkGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$source · ${timeFmt.format(memo.createdAt)}",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (isLocal) "本地视频 · 可离线复盘" else "在线视频 · 可直接播放",
                style = DSTypography.caption,
                color = if (isLocal) DSColors.fairway else Color(0xFF2563EB),
            )
        }
    }
}

private fun V2Memo.isOnlineDownloadMemo(): Boolean {
    val method = trainingMethod.orEmpty()
    val meta = metaText.orEmpty()
    return method == "在线下载" ||
        method == "在线视频" ||
        meta.startsWith("视频下载") ||
        meta.startsWith("在线素材") ||
        tags.any { it == "在线视频" }
}

private suspend fun createLocalMemo(
    imported: V2VideoFileStore.ImportResult,
    info: DownloadVideoInfo,
): UUID {
    val topic = V2Topic.SWING
    val memo = V2Memo.mixedMemo(
        displayTitle = info.title.ifBlank { "下载视频" }.take(36),
        body = "来自${info.platform?.takeIf { it.isNotBlank() } ?: "在线视频"}的练球素材。",
        videoUrl = "file://${imported.absolutePath}",
        durationSec = imported.durationSec.takeIf { it > 0.0 } ?: info.durationSec,
        topic = topic,
        tags = listOfNotNull(info.platform?.takeIf { it.isNotBlank() }, "在线视频"),
        metaText = "视频下载 · ${info.platform ?: "在线"}",
    ).apply {
        thumbnailUrl = imported.thumbnailPath?.let { "file://$it" } ?: info.thumbnailUrl
        trainingMethod = "在线下载"
        platform = info.platform
        thumbnailKind = V2MemoBridge.thumbnailKindFor(topic)
    }
    return V2MemoStore.createAndWait(memo)
}

private suspend fun createOnlineMemo(info: DownloadVideoInfo): UUID? {
    val topic = V2Topic.SWING
    val url = info.playUrl ?: info.downloadUrl ?: return null
    val memo = V2Memo.mixedMemo(
        displayTitle = info.title.ifBlank { "在线视频" }.take(36),
        body = "来自${info.platform?.takeIf { it.isNotBlank() } ?: "在线视频"}的练球素材。",
        videoUrl = url,
        durationSec = info.durationSec,
        topic = topic,
        tags = listOfNotNull(info.platform?.takeIf { it.isNotBlank() }, "在线视频"),
        metaText = "在线素材 · ${info.platform ?: "视频"}",
    ).apply {
        source = V2MemoSource.SHARE
        thumbnailUrl = info.thumbnailUrl
        trainingMethod = "在线视频"
        isOnlineVideo = true
        platform = info.platform
        thumbnailKind = V2MemoBridge.thumbnailKindFor(topic)
    }
    return V2MemoStore.createAndWait(memo)
}

@Composable
private fun DownloadHero(isDirectUrlMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .height(214.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.v2_hero_grass_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            DSColors.deepGreen.copy(alpha = 0.92f),
                            DSColors.deepGreen.copy(alpha = 0.58f),
                            DSColors.fairway.copy(alpha = 0.30f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )
        Icon(
            Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 4.dp)
                .size(92.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.26f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDirectUrlMode) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    } else {
                        Text("抖", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (isDirectUrlMode) "广场视频保存" else "抖音视频下载",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        if (isDirectUrlMode) "把广场视频留在本地" else "把好内容变成练球笔记素材",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 29.sp,
                    )
                    Text(
                        if (isDirectUrlMode) {
                            "用于慢放、对比和复盘，确认积分后会自动生成视频 Memo。"
                        } else {
                            "支持抖音分享文案、短链和视频页链接，下载后可以直接创建 Memo。"
                        },
                        color = Color.White.copy(alpha = 0.78f),
                        style = DSTypography.caption,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DownloadMetricPill(Icons.Filled.Download, "10 积分/次")
                    DownloadMetricPill(Icons.Filled.CheckCircle, "确认后扣除")
                    DownloadMetricPill(Icons.Filled.VideoLibrary, "可建 Memo")
                }
            }
        }
    }
}

@Composable
private fun DownloadMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun CreditCard(
    balance: Int,
    isLoggedIn: Boolean,
    refreshing: Boolean,
    message: String?,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onOpenCreditsReset: () -> Unit,
    onOpenCreditsPurchase: () -> Unit,
) {
    val downloads = if (isLoggedIn) balance.coerceAtLeast(0) / DOWNLOAD_COST_CREDITS else 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = DSColors.trophyGold, modifier = Modifier.size(22.dp))
            Text("积分费用", style = DSTypography.bodyBold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Text("确认前可取消返还", style = DSTypography.caption, color = Color(0xFF9CA3AF))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreditMetric(value = DOWNLOAD_COST_CREDITS.toString(), label = "积分/次", tint = DSColors.trophyGold, modifier = Modifier.weight(1f))
            CreditDivider()
            CreditMetric(
                value = if (isLoggedIn) balance.toString() else "--",
                label = if (isLoggedIn) "当前积分" else "未登录",
                tint = if (balance >= DOWNLOAD_COST_CREDITS) Color(0xFFA855F7) else DSColors.flagRed,
                modifier = Modifier.weight(1f),
            )
            CreditDivider()
            CreditMetric(
                value = "约${downloads}个",
                label = "可下载",
                tint = if (downloads > 0) Color(0xFF10B981) else DSColors.flagRed,
                modifier = Modifier.weight(1f),
            )
        }
        when {
            !isLoggedIn -> CreditActionPanel(
                title = "登录后使用下载积分",
                subtitle = "积分冻结、扣除和取消返还都需要账号态。",
                primaryText = "去登录",
                secondaryText = null,
                primaryIcon = Icons.AutoMirrored.Filled.Login,
                secondaryIcon = Icons.Filled.Refresh,
                primaryEnabled = true,
                secondaryEnabled = false,
                onPrimary = onLogin,
                onSecondary = {},
            )
            balance < DOWNLOAD_COST_CREDITS -> CreditActionPanel(
                title = "积分不足",
                subtitle = "当前余额不足 $DOWNLOAD_COST_CREDITS 积分。可先充值；若余额显示异常，再校准本地缓存。",
                primaryText = "去充值",
                secondaryText = "校准余额",
                primaryIcon = Icons.Filled.AccountBalanceWallet,
                secondaryIcon = Icons.Filled.AccountBalanceWallet,
                primaryEnabled = true,
                secondaryEnabled = !refreshing,
                onPrimary = onOpenCreditsPurchase,
                onSecondary = onOpenCreditsReset,
            )
            else -> CreditActionPanel(
                title = "积分可用",
                subtitle = "确认下载前会先冻结积分，取消后自动返还并刷新余额。",
                primaryText = if (refreshing) "刷新中..." else "刷新积分",
                secondaryText = null,
                primaryIcon = Icons.Filled.Refresh,
                secondaryIcon = Icons.Filled.AccountBalanceWallet,
                primaryEnabled = !refreshing,
                secondaryEnabled = false,
                onPrimary = onRefresh,
                onSecondary = {},
            )
        }
        message?.let {
            Text(
                it,
                style = DSTypography.caption,
                color = if (it.contains("失败")) DSColors.flagRed else Color(0xFF10B981),
            )
        }
    }
}

@Composable
private fun CreditActionPanel(
    title: String,
    subtitle: String,
    primaryText: String,
    secondaryText: String?,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryEnabled: Boolean,
    secondaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFFCBD5E1), style = DSTypography.caption)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CreditPillAction(
                text = primaryText,
                icon = primaryIcon,
                enabled = primaryEnabled,
                prominent = true,
                onClick = onPrimary,
                modifier = Modifier.weight(1f),
            )
            secondaryText?.let {
                CreditPillAction(
                    text = it,
                    icon = secondaryIcon,
                    enabled = secondaryEnabled,
                    prominent = false,
                    onClick = onSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CreditPillAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.08f)
                    prominent -> DSColors.fairway
                    else -> Color.White.copy(alpha = 0.14f)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            color = if (enabled) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlatformStatusPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF10B981).copy(alpha = 0.12f))
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.32f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981)),
        )
        Text("目前优先支持抖音平台", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CreditMetric(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = tint, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color(0xFF9CA3AF), fontSize = 11.sp)
    }
}

@Composable
private fun CreditDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color(0xFF374151)),
    )
}

@Composable
private fun UrlCard(
    url: String,
    enabled: Boolean,
    placeholder: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Link, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            Text("视频链接", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        }
        BasicTextField(
            value = url,
            onValueChange = onUrlChange,
            enabled = enabled,
            textStyle = DSTypography.body.copy(color = DSColors.inkGreen),
            cursorBrush = SolidColor(DSColors.fairway),
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.sandBeige.copy(alpha = 0.55f))
                .padding(12.dp),
            decorationBox = { inner ->
                if (url.isBlank()) Text(placeholder, style = DSTypography.body, color = DSColors.shadowGray)
                inner()
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallActionButton("粘贴", Icons.Filled.ContentPaste, onPaste)
            SmallActionButton("复制", Icons.Filled.Link, onCopy)
        }
    }
}

@Composable
private fun DirectDownloadSourceSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("广场视频保存", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        SupportedDownloadRow(
            icon = Icons.Filled.VideoLibrary,
            title = "来自视频广场",
            subtitle = "使用当前播放地址进入本地保存流程",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.CheckCircle,
            title = "确认后扣除积分",
            subtitle = "保存前仍会先识别信息并等待确认",
        )
    }
}

@Composable
private fun PlatformSupportSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("平台支持状态", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PlatformSupportBadge("抖音", "可下载", Color(0xFF111827), active = true)
            PlatformSupportBadge("哔哩哔哩", "在线 Memo", Color(0xFF00A1D6))
            PlatformSupportBadge("小红书", "在线 Memo", Color(0xFFFF2442))
            PlatformSupportBadge("YouTube", "在线 Memo", Color(0xFFFF0000))
        }
        Text(
            "抖音支持下载到本地并生成 Memo；其他平台可先添加为在线视频 Memo，无需扣下载积分。",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
    }
}

@Composable
private fun OnlineMemoFallbackCard(
    platform: String,
    onOpenOnlineMemo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenOnlineMemo)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.VideoLibrary, null, tint = Color(0xFF2563EB), modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("$platform 链接建议添加为在线视频", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text("无需扣下载积分，进入 Memo 后可直接观看和复盘。", style = DSTypography.caption, color = DSColors.shadowGray)
        }
        Text(
            "去添加",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2563EB))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun PlatformSupportBadge(
    name: String,
    label: String,
    color: Color,
    active: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.first().toString(), color = color.copy(alpha = 0.65f), fontWeight = FontWeight.Bold)
        }
        Text(name, style = DSTypography.caption, color = DSColors.shadowGray)
        Text(
            label,
            color = if (active) Color.White else Color(0xFF2563EB),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (active) DSColors.fairway else Color(0xFFEFF6FF))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun DirectOnlineMemoFallbackCard(
    hasError: Boolean,
    onOpenOnlineMemo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (hasError) Color(0xFFFFFBEB) else Color(0xFFEFF6FF))
            .border(
                1.dp,
                if (hasError) Color(0xFFFDE68A) else Color(0xFFBFDBFE),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onOpenOnlineMemo)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background((if (hasError) DSColors.trophyGold else Color(0xFF2563EB)).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.VideoLibrary,
                null,
                tint = if (hasError) DSColors.trophyGold else Color(0xFF2563EB),
                modifier = Modifier.size(21.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (hasError) "下载失败时先存为在线视频" else "也可先存为在线视频 Memo",
                style = DSTypography.bodyBold,
                color = DSColors.inkGreen,
            )
            Text(
                "不扣下载积分，仍可进入视频 Memo 播放、复盘和关联目标。",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Text(
            "去保存",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (hasError) DSColors.trophyGold else Color(0xFF2563EB))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun VideoInfoCard(info: DownloadVideoInfo?, costCredits: Int, platform: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 112.dp, height = 78.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            if (!info?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = info?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.VideoLibrary, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(30.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(info?.title ?: "已识别视频", style = DSTypography.bodyBold, color = DSColors.inkGreen, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${platform ?: info?.platform ?: "视频平台"} · $costCredits 积分", style = DSTypography.caption, color = DSColors.shadowGray)
            Text("确认后开始下载，完成后自动写入 Memo。", style = DSTypography.caption, color = DSColors.fairway)
        }
    }
}

@Composable
private fun DownloadSuccessDialog(
    platform: String,
    costCredits: Int?,
    isOnlineMemo: Boolean,
    onOpenMemo: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val costText = costCredits?.takeIf { it > 0 }?.let { "，消耗 $it 积分" }.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DSColors.mistGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(24.dp))
                }
                Text("已创建视频 Memo", color = DSColors.inkGreen, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "已从 $platform ${if (isOnlineMemo) "保存为在线视频 Memo" else "保存视频"}$costText。",
                    style = DSTypography.body,
                    color = DSColors.inkGreen,
                )
                Text(
                    "现在可以先补充标题、备注、标签和目标，再进入 Memo 慢放和对比；也可以继续粘贴下一个视频链接。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenMemo) {
                Text("补充 Memo", color = DSColors.fairway, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onContinue) {
                Text("继续下载", color = DSColors.shadowGray, fontWeight = FontWeight.Medium)
            }
        },
    )
}

@Composable
private fun PhaseCard(
    phase: DownloadPhase,
    progress: Double,
    elapsedSeconds: Int,
    error: String?,
    note: String?,
) {
    val (icon, title, subtitle, tint) = when (phase) {
        DownloadPhase.IDLE -> arrayOf(Icons.Filled.Download, "等待链接", "先获取视频信息，再确认下载。", DSColors.shadowGray)
        DownloadPhase.FETCHING -> arrayOf(Icons.Filled.CloudDownload, "正在获取信息", "正在请求下载服务...", DSColors.fairway)
        DownloadPhase.READY -> arrayOf(Icons.Filled.CheckCircle, "可确认下载", "积分已冻结，取消会返还。", DSColors.fairway)
        DownloadPhase.CONFIRMING -> arrayOf(Icons.Filled.CloudDownload, "正在确认", "确认扣除积分并获取下载链接。", DSColors.fairway)
        DownloadPhase.DOWNLOADING -> arrayOf(Icons.Filled.Download, "正在下载", "请保持页面打开。", DSColors.fairway)
        DownloadPhase.CANCELLING -> arrayOf(Icons.Filled.CloudDownload, "正在取消", "正在返还冻结积分。", DSColors.shadowGray)
        DownloadPhase.COMPLETED -> arrayOf(Icons.Filled.CheckCircle, "已创建 Memo", "视频素材已经进入视频笔记。", DSColors.fairway)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(if (error == null) icon as androidx.compose.ui.graphics.vector.ImageVector else Icons.Filled.ErrorOutline, null, tint = if (error == null) tint as Color else DSColors.flagRed)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (error == null) title as String else "操作失败", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(
                error ?: note ?: subtitle as String,
                style = DSTypography.caption,
                color = when {
                    error != null -> DSColors.flagRed
                    note != null -> DSColors.fairway
                    else -> DSColors.shadowGray
                },
            )
            if (phase == DownloadPhase.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { progress.toFloat().coerceIn(0f, 1f) },
                    color = DSColors.fairway,
                    trackColor = DSColors.mistGreen,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                )
            }
            if (phase in timedDownloadPhases) {
                Text(
                    activeDownloadStatusText(phase, progress, elapsedSeconds),
                    style = DSTypography.caption,
                    color = DSColors.fairway,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (phase == DownloadPhase.FETCHING || phase == DownloadPhase.CONFIRMING || phase == DownloadPhase.CANCELLING) {
            CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

private fun activeDownloadStatusText(phase: DownloadPhase, progress: Double, elapsedSeconds: Int): String {
    val time = "${elapsedSeconds.coerceAtLeast(0)}s"
    return when (phase) {
        DownloadPhase.DOWNLOADING -> "下载进度 ${(progress * 100).toInt().coerceIn(0, 100)}% · $time · 进行中"
        DownloadPhase.FETCHING -> "已用 $time · 正在识别"
        DownloadPhase.CONFIRMING -> "已用 $time · 正在确认"
        DownloadPhase.CANCELLING -> "已用 $time · 正在取消"
        else -> ""
    }
}

@Composable
private fun TodaySummaryCard(stats: DownloadStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadStatCell(
            title = "今日下载",
            value = stats.todayCount.toString(),
            subtitle = "次",
            modifier = Modifier.weight(1f),
        )
        CreditDivider()
        DownloadStatCell(
            title = "近 30 天",
            value = stats.last30DaysCount.toString(),
            subtitle = "次",
            modifier = Modifier.weight(1f),
        )
        CreditDivider()
        DownloadStatCell(
            title = "本地保存",
            value = stats.localSavedCount.toString(),
            subtitle = "条",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DownloadStatCell(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = DSColors.shadowGray, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = value,
                color = DSColors.inkGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(subtitle, color = DSColors.shadowGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun SupportedDownloadCard(isDirectUrlMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("当前可用能力", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        SupportedDownloadRow(
            icon = Icons.Filled.CloudDownload,
            title = if (isDirectUrlMode) "广场视频保存" else "抖音视频下载",
            subtitle = if (isDirectUrlMode) {
                "从广场播放页进入本地保存和 Memo 创建流程"
            } else {
                "主流程支持识别、确认、下载和创建 Memo"
            },
        )
        SupportedDownloadRow(
            icon = Icons.Filled.CheckCircle,
            title = "视频信息确认",
            subtitle = "确认标题、积分和视频源后再扣除积分",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.VideoLibrary,
            title = "素材复盘",
            subtitle = "完成后进入视频 Memo，可继续慢放和记录",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.AccountBalanceWallet,
            title = "积分保护",
            subtitle = "确认前冻结，取消后刷新余额并恢复状态",
        )
    }
}

@Composable
private fun SupportedDownloadRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(subtitle, style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun FooterNote() {
    Text(
        text = "下载内容仅用于个人学习与练球复盘，请尊重原作者权益。",
        style = DSTypography.caption,
        color = DSColors.shadowGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 16.dp),
    )
}

@Composable
private fun ActionRow(
    phase: DownloadPhase,
    canStart: Boolean,
    startActionText: String,
    onlineMemoActionText: String?,
    startDisabledReason: String?,
    canConfirm: Boolean,
    createdMemoId: UUID?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onOpenMemo: () -> Unit,
    onOpenOnlineMemo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (phase) {
            DownloadPhase.IDLE -> {
                if (onlineMemoActionText != null) {
                    PrimaryAction(onlineMemoActionText, enabled = true, onClick = onOpenOnlineMemo)
                } else {
                    PrimaryAction(startActionText, enabled = canStart, onClick = onStart)
                }
                startDisabledReason?.let {
                    Text(
                        it,
                        style = DSTypography.caption,
                        color = if (it.contains("积分") || it.contains("登录")) DSColors.flagRed else DSColors.shadowGray,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            DownloadPhase.READY -> {
                PrimaryAction("确认下载并创建 Memo", enabled = canConfirm, onClick = onConfirm)
                SecondaryAction("取消并返还冻结积分", onClick = onCancel)
            }
            DownloadPhase.COMPLETED -> PrimaryAction("补充刚创建的 Memo", enabled = createdMemoId != null, onClick = onOpenMemo)
            else -> PrimaryAction("处理中...", enabled = false)
        }
    }
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.28f))
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = DSColors.shadowGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SmallActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.mistGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
        Text(label, style = DSTypography.caption, color = DSColors.fairway)
    }
}

internal fun downloadErrorMessage(base: String, debugDetail: String?): String {
    return downloadErrorMessage(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun downloadErrorMessage(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun downloadThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return downloadErrorMessage(base, error.message)
}

internal fun <T> downloadResultFailure(error: Throwable): Result<T> {
    if (error is CancellationException) throw error
    return Result.failure(error)
}

private const val ASYNC_DOWNLOAD_TIP_MIN_DURATION_SEC = 180.0

private fun shouldSuggestAsyncDownload(
    info: DownloadVideoInfo?,
    jobId: String,
    acceptedJobId: String?,
): Boolean =
    acceptedJobId != jobId && (info?.durationSec ?: 0.0) >= ASYNC_DOWNLOAD_TIP_MIN_DURATION_SEC

private fun formatDownloadDuration(durationSec: Double?): String {
    val total = durationSec?.toInt()?.coerceAtLeast(0) ?: return "时长较长"
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes > 0) "${minutes}分${seconds.toString().padStart(2, '0')}秒" else "${seconds}秒"
}

@Composable
private fun AsyncDownloadTipDialog(
    info: DownloadVideoInfo?,
    onLater: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("异步下载建议", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "这个视频${formatDownloadDuration(info?.durationSec)}，下载可能需要更久。",
                    style = DSTypography.body,
                    color = DSColors.inkGreen,
                )
                Text(
                    "Android 会保留当前待确认任务；你可以稍后回到视频下载继续确认，或留在当前页直接开始下载。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("继续当前下载", color = DSColors.fairway, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text("稍后处理", color = DSColors.shadowGray, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = DSColors.cardWhite,
    )
}

private fun Context.readClipboardText(): String? = runCatching {
    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clip.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(this)
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}.getOrNull()

private fun Context.copyTextToClipboard(label: String, text: String): Boolean = runCatching {
    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clip.setPrimaryClip(ClipData.newPlainText(label, text))
}.isSuccess

private fun findDuration(element: JsonElement?): Double? {
    val raw = findNumber(element, "duration", "duration_ms", "durationMs", "video_duration")
        ?: return null
    return if (raw > 1000.0) raw / 1000.0 else raw
}

private fun findNumber(element: JsonElement?, vararg keys: String): Double? {
    when (element) {
        is JsonObject -> {
            keys.forEach { key ->
                element[key]?.jsonPrimitive?.doubleOrNull?.let { return it }
                element[key]?.jsonPrimitive?.intOrNull?.let { return it.toDouble() }
            }
            element.values.forEach { findNumber(it, *keys)?.let { value -> return value } }
        }
        is JsonArray -> element.forEach { findNumber(it, *keys)?.let { value -> return value } }
        else -> Unit
    }
    return null
}

private fun findUrl(element: JsonElement?, vararg keys: String): String? =
    findString(element, *keys)?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

private fun String.isHttpUrl(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

private fun findString(element: JsonElement?, vararg keys: String): String? {
    when (element) {
        is JsonObject -> {
            keys.forEach { key ->
                element[key]?.let { value ->
                    value.asStringOrFirstUrl()?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
            element.values.forEach { findString(it, *keys)?.let { value -> return value } }
        }
        is JsonArray -> element.forEach { findString(it, *keys)?.let { value -> return value } }
        else -> Unit
    }
    return null
}

private fun JsonElement.asStringOrFirstUrl(): String? {
    return when (this) {
        is JsonArray -> firstNotNullOfOrNull { it.asStringOrFirstUrl()?.takeIf(String::isNotBlank) }
        is JsonObject -> values.firstNotNullOfOrNull { it.asStringOrFirstUrl()?.takeIf { s -> s.startsWith("http") } }
        else -> runCatching { jsonPrimitive.content }.getOrNull()
    }
}
