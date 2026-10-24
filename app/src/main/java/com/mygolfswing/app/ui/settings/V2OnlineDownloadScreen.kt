package com.mygolfswing.app.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.download.V2AsyncDownloadStatus
import com.mygolfswing.app.data.download.V2AsyncDownloadTask
import com.mygolfswing.app.data.download.V2DownloadStatisticsSnapshot
import com.mygolfswing.app.data.download.V2DownloadStatisticsStore
import com.mygolfswing.app.data.download.V2LegacyDownloadRecord
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.repo.V2DownloadStartResult
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.data.video.publishDownloadedVideoToGallery
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.io.File
import java.util.UUID

@Composable
fun V2OnlineDownloadScreen(
    onBack: () -> Unit,
    onMemoCreated: (UUID) -> Unit,
    initialUrl: String = "",
    allowDirectUrlDownload: Boolean = false,
    onRequestLogin: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenCreditsPurchase: () -> Unit = {},
    readClipboardText: (() -> String?)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
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
    var showNonDouyinTip by remember { mutableStateOf(false) }
    var nonDouyinPlatform by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(DownloadServiceConnectionState.CHECKING) }
    var asyncTipAcceptedJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var creditMessage by remember { mutableStateOf<String?>(null) }
    val memos by V2MemoStore.allMemos.collectAsState()
    val downloadStats by V2DownloadStatisticsStore.stats.collectAsState()
    val canAffordDownload = profile.isLoggedIn && profile.credits >= DOWNLOAD_COST_CREDITS
    val cleanedUrl = remember(urlInput) { V2OnlineVideoMetadataFetcher.cleanVideoUrl(urlInput) }
    val canStartDownload = urlInput.isNotBlank()
    val draftScopeKey = remember(profile.userId, allowDirectUrlDownload) {
        downloadDraftKey(profile.userId, allowDirectUrlDownload)
    }
    val anonymousDraftKey = remember(allowDirectUrlDownload) {
        downloadDraftKey(userId = null, directMode = allowDirectUrlDownload)
    }

    LaunchedEffect(Unit) {
        connectionState = if (ServiceLocator.videoDownloadRepo.checkServiceConnection()) {
            DownloadServiceConnectionState.ONLINE
        } else {
            DownloadServiceConnectionState.WAITING
        }
    }

    LaunchedEffect(memos) {
        val legacyRecords = withContext(Dispatchers.IO) {
            memos.filter(V2Memo::isOnlineDownloadMemo).map { memo ->
                V2LegacyDownloadRecord(
                    date = memo.createdAt.toLocalDate(),
                    fileSizeBytes = memo.videoUrl
                        ?.takeIf { it.startsWith("file://") }
                        ?.removePrefix("file://")
                        ?.let(::File)
                        ?.takeIf(File::isFile)
                        ?.length()
                        ?: 0L,
                )
            }
        }
        V2DownloadStatisticsStore.migrateLegacyMemosIfNeeded(context, legacyRecords)
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

    fun preserveDraftBeforeHandoff() {
        persistDownloadDraftForHandoff(
            context = context,
            userId = profile.userId.takeIf { profile.isLoggedIn },
            directMode = allowDirectUrlDownload,
            url = urlInput,
            phase = phase,
            startResult = startResult,
            createdMemoId = createdMemoId,
        )
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
        restoreDownloadDraftForScope(
            context = context,
            userId = profile.userId.takeIf { profile.isLoggedIn },
            directMode = allowDirectUrlDownload,
        )?.let { urlInput = it }
    }

    LaunchedEffect(draftScopeKey, cleanedUrl, phase, startResult, createdMemoId) {
        if (shouldPersistDownloadDraft(phase, startResult, createdMemoId)) {
            persistDownloadDraftUrl(context, draftScopeKey, cleanedUrl)
        }
    }

    fun startDirectPlazaDownload(cleaned: String) {
        scope.launch {
            error = null
            creditMessage = null
            phaseNote = null
            elapsedSeconds = 0
            progress = 0.0
            phase = DownloadPhase.DOWNLOADING
            try {
                val imported = V2VideoFileStore.downloadRemoteVideo(context, cleaned) { value ->
                    if (value >= 0.0) progress = value
                }
                if (imported == null) {
                    error = "下载失败，请检查视频地址后重试"
                    phase = DownloadPhase.IDLE
                    return@launch
                }

                val info = DownloadVideoInfo(
                    title = "广场视频",
                    author = null,
                    thumbnailUrl = imported.thumbnailPath?.let { "file://$it" },
                    durationSec = imported.durationSec.takeIf { it > 0.0 },
                    fileSizeBytes = imported.sizeBytes,
                    downloadUrl = cleaned,
                    playUrl = cleaned,
                    platform = "视频广场",
                )
                val galleryResult = try {
                    publishDownloadedVideoToGallery(
                        context = context,
                        source = File(imported.absolutePath),
                        displayBase = info.title,
                    )
                } finally {
                    deleteFileCountingFreedBytes(File(imported.absolutePath))
                    imported.thumbnailPath?.let { deleteFileCountingFreedBytes(File(it)) }
                }

                if (!galleryResult.saved) {
                    error = galleryResult.message
                    phaseNote = null
                    phase = DownloadPhase.IDLE
                    return@launch
                }

                ServiceLocator.session.updateCredits((profile.credits - DOWNLOAD_COST_CREDITS).coerceAtLeast(0))
                try {
                    ServiceLocator.creditsRepo.fetchBalance(force = true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    // Match iOS: the local balance update succeeds even if the follow-up refresh cannot complete.
                }
                V2ProfileStore.refresh(force = true)

                videoInfo = info
                progress = 1.0
                phaseNote = galleryResult.message
                clearDownloadDraftUrl(context, draftScopeKey)
                phase = DownloadPhase.COMPLETED
                showSuccessDialog = true
            } catch (cancelled: CancellationException) {
                phase = downloadPhaseAfterCancellation(phase, hasActiveTask = false)
                phaseNote = null
                throw cancelled
            } catch (throwable: Throwable) {
                error = downloadThrowableError("下载失败，请检查视频地址后重试", throwable)
                phase = DownloadPhase.IDLE
            }
        }
    }

    fun startDownload() {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(urlInput)
        if (cleaned.isBlank()) {
            error = "请先粘贴视频链接"
            return
        }
        if (!profile.isLoggedIn) {
            preserveDraftBeforeHandoff()
            onRequestLogin()
            return
        }
        val platform = V2OnlineVideoMetadataFetcher.detectPlatform(cleaned)
        if (platform != "抖音" && !allowDirectUrlDownload) {
            nonDouyinPlatform = platform.takeIf { it.isNotBlank() } ?: "当前平台"
            showNonDouyinTip = true
            return
        }
        if (!canAffordDownload) {
            creditMessage = "当前积分不足，下载需要 $DOWNLOAD_COST_CREDITS 积分。请先充值；若余额显示异常，再刷新或校准。"
            return
        }
        urlInput = cleaned
        clearDownloadDraftUrl(context, draftScopeKey)
        if (onlineDownloadStartStrategy(allowDirectUrlDownload) == OnlineDownloadStartStrategy.DIRECT_PLAZA_GALLERY) {
            startDirectPlazaDownload(cleaned)
            return
        }
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

    fun requestAsyncDownloadNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun confirmDownload(asyncMode: Boolean = false) {
        val start = startResult ?: return
        if (!asyncMode && shouldSuggestAsyncDownload(videoInfo, start.jobId, asyncTipAcceptedJobId)) {
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
                        if (asyncMode) {
                            val ownerUserId = profile.userId?.takeIf(String::isNotBlank)
                            if (ownerUserId == null) {
                                error = "登录状态已失效，请重新登录后再试"
                                phase = DownloadPhase.READY
                                return@onSuccess
                            }
                            ServiceLocator.asyncDownloadQueue.addTask(
                                ownerUserId = ownerUserId,
                                title = effective.title,
                                originalUrl = urlInput,
                                downloadUrl = downloadUrl,
                                fileSizeBytes = effective.fileSizeBytes ?: 0L,
                                durationSeconds = effective.durationSec?.toInt(),
                            )
                            requestAsyncDownloadNotificationPermission()
                            clearPendingDownloadTask(context, profile.userId)
                            clearDownloadDraftUrl(context, draftScopeKey)
                            urlInput = ""
                            startResult = null
                            videoInfo = null
                            progress = 0.0
                            elapsedSeconds = 0
                            phase = DownloadPhase.IDLE
                            creditMessage = "已加入异步下载，可在右上角查看进度"
                            return@onSuccess
                        }
                        phase = DownloadPhase.DOWNLOADING
                        val imported = V2VideoFileStore.downloadRemoteVideo(context, downloadUrl) { p ->
                            if (p >= 0.0) progress = p
                        }
                        if (imported == null) {
                            if (allowDirectUrlDownload) {
                                error = "视频下载失败，未能保存到系统相册，请稍后重试"
                                phase = DownloadPhase.READY
                            } else {
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
                            }
                        } else if (allowDirectUrlDownload) {
                            val galleryResult = publishDownloadedVideoToGallery(
                                context = context,
                                source = File(imported.absolutePath),
                                displayBase = effective.title.ifBlank { "广场视频" },
                            )
                            deleteFileCountingFreedBytes(File(imported.absolutePath))
                            imported.thumbnailPath?.let { deleteFileCountingFreedBytes(File(it)) }
                            if (galleryResult.saved) {
                                progress = 1.0
                                createdMemoId = null
                                phaseNote = galleryResult.message
                                showSuccessDialog = true
                                clearPendingDownloadTask(context, profile.userId)
                                clearDownloadDraftUrl(context, draftScopeKey)
                                phase = DownloadPhase.COMPLETED
                            } else {
                                error = galleryResult.message
                                phaseNote = "积分已完成结算；请确认相册存储空间后重试。"
                                phase = DownloadPhase.READY
                            }
                        } else {
                            progress = 1.0
                            createdMemoId = createLocalMemo(imported, effective)
                            val galleryResult = publishDownloadedVideoToGallery(
                                context = context,
                                source = File(imported.absolutePath),
                                displayBase = effective.title.ifBlank { "下载视频" },
                            )
                            V2DownloadStatisticsStore.recordDownload(context, imported.sizeBytes)
                            if (!galleryResult.saved) {
                                phaseNote = "Memo 已创建；系统相册保存失败，可在 Memo 中继续使用。"
                            }
                            showSuccessDialog = true
                            clearPendingDownloadTask(context, profile.userId)
                            phase = DownloadPhase.COMPLETED
                        }
                    } else if (asyncMode) {
                        error = "异步下载创建失败：无法获取视频直链"
                        phase = DownloadPhase.READY
                    } else if (allowDirectUrlDownload) {
                        error = "下载服务暂时没有返回可保存的视频地址，请稍后重试"
                        phase = DownloadPhase.READY
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

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnlineDownloadTopBar(
                isDirectUrlMode = allowDirectUrlDownload,
                onBack = onBack,
                onOpenHistory = {
                    preserveDraftBeforeHandoff()
                    onOpenHistory()
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
                isLoggedIn = profile.isLoggedIn,
                onLogin = {
                    preserveDraftBeforeHandoff()
                    onRequestLogin()
                },
                onOpenCreditsPurchase = {
                    preserveDraftBeforeHandoff()
                    onOpenCreditsPurchase()
                },
            )
            UrlCard(
                url = urlInput,
                enabled = phase == DownloadPhase.IDLE || phase == DownloadPhase.READY,
                isDirectUrlMode = allowDirectUrlDownload,
                connectionState = connectionState,
                placeholder = if (allowDirectUrlDownload) "粘贴广场视频播放地址..." else "粘贴抖音分享文案或视频链接...",
                actionText = when {
                    phase == DownloadPhase.FETCHING -> "识别中"
                    phase == DownloadPhase.DOWNLOADING || phase == DownloadPhase.CONFIRMING -> "下载中"
                    !profile.isLoggedIn && urlInput.isNotBlank() -> "登录后下载"
                    profile.isLoggedIn && !canAffordDownload && urlInput.isNotBlank() -> "去充值后下载"
                    allowDirectUrlDownload -> "保存到本地"
                    else -> "开始下载"
                },
                actionEnabled = canStartDownload && phase == DownloadPhase.IDLE,
                onUrlChange = {
                    urlInput = it
                    resetForNewUrl()
                },
                onPaste = {
                    val pasted = if (readClipboardText != null) {
                        readClipboardText()
                    } else {
                        context.readClipboardText()
                    }
                    if (pasted != null) {
                        urlInput = pasted
                        resetForNewUrl()
                        phaseNote = "已从剪贴板粘贴链接"
                    }
                },
                onStart = {
                    if (profile.isLoggedIn && !canAffordDownload && urlInput.isNotBlank()) {
                        preserveDraftBeforeHandoff()
                        onOpenCreditsPurchase()
                    } else {
                        startDownload()
                    }
                },
            )
            PhaseCard(
                phase = phase,
                progress = progress,
                elapsedSeconds = elapsedSeconds,
                error = error,
                note = phaseNote ?: creditMessage,
                isDirectUrlMode = allowDirectUrlDownload,
                displayedUrl = cleanedUrl,
            )
            TodaySummaryCard(stats = downloadStats)
            SupportedDownloadCard()
            FooterNote()
        }
    }
    }

    error?.let { message ->
        DownloadFailureDialog(message = message, onDismiss = { error = null })
    }

    if (phase == DownloadPhase.FETCHING) {
        VideoFetchingOverlay(
            statusText = "正在提取视频信息...",
            url = cleanedUrl.ifBlank { urlInput },
        )
    }

    if (showNonDouyinTip) {
        NonDouyinTipDialog(
            platformName = nonDouyinPlatform,
            onDismiss = { showNonDouyinTip = false },
        )
    }

    if (showSuccessDialog) {
        DownloadSuccessDialog(
            platform = videoInfo?.platform?.takeIf { it.isNotBlank() } ?: if (allowDirectUrlDownload) "视频广场" else "在线视频",
            costCredits = if (allowDirectUrlDownload) DOWNLOAD_COST_CREDITS else startResult?.costCredits,
            isOnlineMemo = videoInfo?.downloadUrl.isNullOrBlank() || phaseNote?.contains("在线") == true,
            showCreateMemoButton = !allowDirectUrlDownload,
            onOpenMemo = {
                showSuccessDialog = false
                createdMemoId?.let(onMemoCreated)
            },
            onContinue = {
                if (allowDirectUrlDownload) {
                    showSuccessDialog = false
                    onBack()
                } else {
                    resetAfterSuccess()
                }
            },
        )
    }

    startResult?.let { start ->
        if (phase == DownloadPhase.READY && !showAsyncDownloadTip && error == null) {
            VideoInfoConfirmDialog(
                platform = start.platform?.takeIf(String::isNotBlank) ?: videoInfo?.platform.orEmpty(),
                info = videoInfo,
                creditsCost = start.costCredits,
                onConfirm = {
                    asyncTipAcceptedJobId = start.jobId
                    confirmDownload(asyncMode = false)
                },
                onAsyncDownload = { confirmDownload(asyncMode = true) },
                onCancel = ::cancelDownload,
            )
        }
        if (showAsyncDownloadTip) {
            AsyncDownloadTipDialog(
                info = videoInfo,
                onAsyncDownload = {
                    showAsyncDownloadTip = false
                    confirmDownload(asyncMode = true)
                },
                onContinueDownload = {
                    asyncTipAcceptedJobId = start.jobId
                    showAsyncDownloadTip = false
                    confirmDownload(asyncMode = false)
                },
                onCancel = { showAsyncDownloadTip = false },
            )
        }
    }
}

@Composable
internal fun VideoFetchingOverlay(
    statusText: String,
    url: String,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = DSSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .testTag("video-fetching-overlay")
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp))
                    .padding(DSSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                CircularProgressIndicator(
                    color = DSColors.fairway,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Text(
                        statusText.ifBlank { "正在识别视频" },
                        style = DSTypography.titleMedium,
                        color = DSColors.inkGreen,
                    )
                    Text(
                        url,
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun NonDouyinTipDialog(
    platformName: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = DSSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .testTag("non-douyin-tip-dialog")
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp))
                    .padding(DSSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(DSColors.sandBeige),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = DSColors.trophyGold, modifier = Modifier.size(30.dp))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Text("暂不支持直接下载", style = DSTypography.titleMedium, color = DSColors.inkGreen)
                    Text(
                        "当前识别为 $platformName。请先使用第三方方式保存，再回到挥呗创建练球 Memo。",
                        style = DSTypography.body,
                        color = DSColors.shadowGray,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().testTag("non-douyin-tip-confirm")) {
                    PrimaryAction("知道了", enabled = true, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
internal fun OnlineDownloadTopBar(
    isDirectUrlMode: Boolean,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(DSColors.warmWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .v2StatusBarPadding()
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = DSColors.fairway,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.testTag("online-download-top-bar"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    if (isDirectUrlMode) "保存广场视频" else "抖音视频下载",
                    style = DSTypography.titleMedium,
                    color = DSColors.inkGreen,
                )
                Text(
                    if (isDirectUrlMode) "下载到本地并保存相册" else "粘贴链接，下载练球素材",
                    style = DSTypography.label,
                    color = DSColors.shadowGray,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, CircleShape)
                    .clickable(onClick = onOpenHistory),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = "异步下载",
                    tint = DSColors.deepGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
    }
}

@Composable
fun V2DownloadHistoryScreen(
    onBack: () -> Unit,
    isLoggedIn: Boolean = true,
    onRequestLogin: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val allTasks by ServiceLocator.asyncDownloadQueue.tasks.collectAsState()
    val ownerUserId = profile.userId?.takeIf { isLoggedIn && it.isNotBlank() }
    val tasks = remember(allTasks, ownerUserId) {
        if (ownerUserId == null) emptyList() else allTasks.filter { it.ownerUserId == ownerUserId }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isLoggedIn) {
                AsyncDownloadManagerTopBar(
                    completedCount = 0,
                    onClose = onBack,
                    onClearCompleted = {},
                )
                DownloadHistoryLoginRequired(onLogin = onRequestLogin)
            } else {
                V2AsyncDownloadManagerContent(
                    tasks = tasks,
                    onClose = onBack,
                    onClearCompleted = { ownerUserId?.let(ServiceLocator.asyncDownloadQueue::clearCompleted) },
                    onPause = { taskId -> ownerUserId?.let { ServiceLocator.asyncDownloadQueue.pauseTask(it, taskId) } },
                    onResume = { taskId -> ownerUserId?.let { ServiceLocator.asyncDownloadQueue.resumeTask(it, taskId) } },
                    onRemove = { taskId -> ownerUserId?.let { ServiceLocator.asyncDownloadQueue.removeTask(it, taskId) } },
                )
            }
        }
    }
}

@Composable
internal fun V2AsyncDownloadManagerContent(
    tasks: List<V2AsyncDownloadTask>,
    onClose: () -> Unit,
    onClearCompleted: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val activeCount = tasks.count {
        it.status == V2AsyncDownloadStatus.PENDING || it.status == V2AsyncDownloadStatus.DOWNLOADING
    }
    val completedCount = tasks.count { it.status == V2AsyncDownloadStatus.COMPLETED }
    val failedCount = tasks.count { it.status == V2AsyncDownloadStatus.FAILED }
    Column(
        modifier = Modifier.fillMaxSize().testTag("async-download-manager"),
    ) {
        AsyncDownloadManagerTopBar(
            completedCount = completedCount,
            onClose = onClose,
            onClearCompleted = onClearCompleted,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().v2NavigationBarPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = DSSpacing.lg,
                end = DSSpacing.lg,
                top = DSSpacing.md,
                bottom = DSSpacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
        ) {
            item {
                AsyncDownloadSummaryCard(
                    activeCount = activeCount,
                    completedCount = completedCount,
                    failedCount = failedCount,
                )
            }
            item {
                AsyncDownloadTaskListCard(
                    tasks = tasks,
                    onPause = onPause,
                    onResume = onResume,
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
private fun AsyncDownloadManagerTopBar(
    completedCount: Int,
    onClose: () -> Unit,
    onClearCompleted: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().v2StatusBarPadding().height(44.dp)
            .testTag("async-download-top-bar"),
    ) {
        Text(
            "异步下载",
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "关闭",
            style = DSTypography.body,
            color = DSColors.fairway,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onClose)
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.sm)
                .testTag("async-download-close"),
        )
        Text(
            "清空完成",
            style = DSTypography.body,
            color = if (completedCount > 0) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(enabled = completedCount > 0, onClick = onClearCompleted)
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.sm)
                .testTag("async-download-clear"),
        )
    }
}

@Composable
private fun AsyncDownloadSummaryCard(activeCount: Int, completedCount: Int, failedCount: Int) {
    AsyncDownloadCard {
        Text("任务概览", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg)) {
            AsyncDownloadMetric("进行中", activeCount, Modifier.weight(1f))
            AsyncDownloadMetric("已完成", completedCount, Modifier.weight(1f))
            AsyncDownloadMetric("失败", failedCount, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AsyncDownloadMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        Text(label, style = DSTypography.caption, color = DSColors.shadowGray)
    }
}

@Composable
private fun AsyncDownloadTaskListCard(
    tasks: List<V2AsyncDownloadTask>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    AsyncDownloadCard {
        Text("下载任务", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = DSSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
            ) {
                Icon(
                    Icons.Outlined.Inbox,
                    contentDescription = null,
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(32.dp).testTag("async-download-empty-tray"),
                )
                Text("暂无异步任务", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                Text("大文件视频可在确认时加入异步下载。", style = DSTypography.caption, color = DSColors.shadowGray)
            }
        } else {
            tasks.forEach { task ->
                AsyncDownloadTaskRow(
                    task = task,
                    onPause = { onPause(task.id) },
                    onResume = { onResume(task.id) },
                    onRemove = { onRemove(task.id) },
                )
            }
        }
    }
}

@Composable
private fun AsyncDownloadCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
        content = content,
    )
}

@Composable
private fun AsyncDownloadTaskRow(
    task: V2AsyncDownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.listMutedBg)
            .padding(DSSpacing.md),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    task.title,
                    style = DSTypography.bodyBold,
                    color = DSColors.inkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(task.status.displayName, style = DSTypography.caption, color = DSColors.shadowGray)
            }
            Text(task.fileSizeText, style = DSTypography.label, color = DSColors.shadowGray)
        }
        if (task.status == V2AsyncDownloadStatus.DOWNLOADING) {
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(50)).background(DSColors.mistGreen),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(task.progress.toFloat().coerceIn(0f, 1f))
                        .height(4.dp).background(DSColors.fairway),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            when (task.status) {
                V2AsyncDownloadStatus.DOWNLOADING -> {
                    AsyncTaskAction(label = "暂停", primary = false, onClick = onPause)
                }
                V2AsyncDownloadStatus.PAUSED, V2AsyncDownloadStatus.FAILED -> {
                    AsyncTaskAction(label = "继续", primary = true, onClick = onResume)
                }
                else -> Unit
            }
            Spacer(Modifier.weight(1f))
            if (task.status != V2AsyncDownloadStatus.DOWNLOADING) {
                AsyncTaskAction(label = "删除", primary = false, destructive = true, onClick = onRemove)
            }
        }
    }
}

@Composable
private fun AsyncTaskAction(
    label: String,
    primary: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) DSColors.flagRed else DSColors.fairway
    Text(
        label,
        style = DSTypography.caption.copy(fontWeight = FontWeight.Bold),
        color = if (primary) Color.White else tint,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) DSColors.fairway else Color.Transparent)
            .border(1.dp, tint, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
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
            Text("登录后查看异步下载", style = DSTypography.titleMedium, color = DSColors.inkGreen)
            Text(
                "后台下载任务按账号隔离保存，登录后再继续查看。",
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

internal enum class OnlineDownloadStartStrategy {
    VIDEO_INFO_CONFIRMATION,
    DIRECT_PLAZA_GALLERY,
}

internal fun onlineDownloadStartStrategy(allowDirectUrlDownload: Boolean): OnlineDownloadStartStrategy =
    if (allowDirectUrlDownload) {
        OnlineDownloadStartStrategy.DIRECT_PLAZA_GALLERY
    } else {
        OnlineDownloadStartStrategy.VIDEO_INFO_CONFIRMATION
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

internal fun downloadDraftKey(userId: String?, directMode: Boolean): String =
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

internal fun persistDownloadDraftUrl(context: Context, key: String, url: String) {
    val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(url)
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE).edit {
        if (cleaned.isBlank()) {
            remove(key)
        } else {
            putString(key, cleaned)
        }
    }
}

internal fun restoreDownloadDraftUrl(context: Context, key: String): String? =
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .getString(key, null)
        ?.takeIf { it.isNotBlank() }

internal fun clearDownloadDraftUrl(context: Context, key: String) {
    context.getSharedPreferences(PENDING_DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        .edit {
            remove(key)
        }
}

internal fun persistDownloadDraftForHandoff(
    context: Context,
    userId: String?,
    directMode: Boolean,
    url: String,
    phase: DownloadPhase,
    startResult: V2DownloadStartResult?,
    createdMemoId: UUID?,
): Boolean {
    if (!shouldPersistDownloadDraft(phase, startResult, createdMemoId)) return false
    val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(url)
    if (cleaned.isBlank()) return false
    persistDownloadDraftUrl(context, downloadDraftKey(userId, directMode), cleaned)
    return true
}

internal fun restoreDownloadDraftForScope(
    context: Context,
    userId: String?,
    directMode: Boolean,
): String? {
    val scopedKey = downloadDraftKey(userId, directMode)
    restoreDownloadDraftUrl(context, scopedKey)?.let { return it }
    val accountId = userId?.takeIf { it.isNotBlank() } ?: return null
    val anonymousKey = downloadDraftKey(userId = null, directMode = directMode)
    val anonymousDraft = restoreDownloadDraftUrl(context, anonymousKey) ?: return null
    persistDownloadDraftUrl(context, downloadDraftKey(accountId, directMode), anonymousDraft)
    clearDownloadDraftUrl(context, anonymousKey)
    return anonymousDraft
}

private fun jsonStringOrNull(value: String?): JsonElement =
    value?.takeIf { it.isNotBlank() }?.let { JsonPrimitive(it) } ?: JsonNull

internal data class DownloadVideoInfo(
    val title: String,
    val author: String?,
    val thumbnailUrl: String?,
    val durationSec: Double?,
    val fileSizeBytes: Long?,
    val downloadUrl: String?,
    val playUrl: String?,
    val platform: String?,
) {
    fun mergeWith(old: DownloadVideoInfo?): DownloadVideoInfo = DownloadVideoInfo(
        title = title.takeIf { it.isNotBlank() } ?: old?.title.orEmpty(),
        author = author ?: old?.author,
        thumbnailUrl = thumbnailUrl ?: old?.thumbnailUrl,
        durationSec = durationSec ?: old?.durationSec,
        fileSizeBytes = fileSizeBytes ?: old?.fileSizeBytes,
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
            val fileSize = findNumber(element, "file_size", "fileSize", "filesize", "size_bytes", "content_length")
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?.toLong()
            return DownloadVideoInfo(
                title = title,
                author = author,
                thumbnailUrl = thumb,
                durationSec = duration,
                fileSizeBytes = fileSize,
                downloadUrl = download,
                playUrl = play,
                platform = platform,
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
internal fun DownloadHero(isDirectUrlMode: Boolean) {
    val heroShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = heroShape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .clip(heroShape)
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
                            DSColors.deepGreen.copy(alpha = 0.88f),
                            DSColors.deepGreen.copy(alpha = 0.54f),
                            DSColors.fairway.copy(alpha = 0.34f),
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
                            Color.Black.copy(alpha = 0.48f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.36f),
                        ),
                    ),
                ),
        )
        Icon(
            Icons.Filled.SportsGolf,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 16.dp)
                .size(86.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.26f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (isDirectUrlMode) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.online_download_douyin_logo),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Text(
                            if (isDirectUrlMode) "广场视频保存" else "抖音视频下载",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        if (isDirectUrlMode) "把广场视频留在本地" else "把好内容变成练球笔记素材",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 29.sp,
                    )
                    Text(
                        if (isDirectUrlMode) {
                            "用于慢放、对比和复盘，下载后会消耗积分。"
                        } else {
                            "支持抖音分享文案、短链和视频页链接，下载后可以直接创建 Memo。"
                        },
                        color = Color.White.copy(alpha = 0.78f),
                        style = DSTypography.caption,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDirectUrlMode) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.online_download_douyin_logo),
                            contentDescription = "抖音",
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DownloadMetricPill(Icons.Filled.Bolt, "10 积分/次")
                DownloadMetricPill(Icons.Filled.PhotoLibrary, "保存相册")
                DownloadMetricPill(Icons.Filled.Description, "可建 Memo")
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
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = DSSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun CreditCard(
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onOpenCreditsPurchase: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .downloadCardSurface()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DSColors.sandBeige),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CreditCard,
                contentDescription = null,
                tint = DSColors.trophyGold,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("下载积分", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(
                "抖音与广场下载均会在确认后扣除积分",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(DSColors.fairway)
                .clickable(onClick = if (isLoggedIn) onOpenCreditsPurchase else onLogin)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("充值", style = DSTypography.caption, color = Color.White)
        }
    }
}

internal enum class DownloadServiceConnectionState {
    CHECKING,
    ONLINE,
    WAITING,
}

@Composable
internal fun DownloadConnectionBadge(state: DownloadServiceConnectionState) {
    val online = state == DownloadServiceConnectionState.ONLINE
    val tint = if (online) DSColors.fairway else DSColors.trophyGold
    val background = if (online) DSColors.mistGreen else DSColors.sandBeige
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = DSSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (online) DSColors.leafGreen else DSColors.trophyGold),
        )
        Text(if (online) "服务在线" else "等待检测", color = tint, style = DSTypography.label)
    }
}

@Composable
private fun CreditDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(42.dp)
            .background(DSColors.borderLine),
    )
}

@Composable
internal fun UrlCard(
    url: String,
    enabled: Boolean,
    isDirectUrlMode: Boolean,
    connectionState: DownloadServiceConnectionState = DownloadServiceConnectionState.ONLINE,
    placeholder: String,
    actionText: String,
    actionEnabled: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .downloadCardSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isDirectUrlMode) "视频地址" else "抖音链接",
                style = DSTypography.bodyBold,
                color = DSColors.inkGreen,
            )
            Spacer(Modifier.weight(1f))
            DownloadConnectionBadge(connectionState)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("online-download-url-input")
                .height(116.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.listMutedBg),
        ) {
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                enabled = enabled,
                textStyle = DSTypography.body.copy(color = DSColors.inkGreen),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxSize().padding(16.dp),
                decorationBox = { inner ->
                    if (url.isBlank()) Text(placeholder, style = DSTypography.body, color = DSColors.shadowGray)
                    inner()
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm),
        ) {
            DownloadInputAction(
                text = "粘贴",
                icon = Icons.Filled.ContentPaste,
                prominent = false,
                enabled = enabled,
                onClick = onPaste,
                modifier = Modifier.weight(1f),
            )
            DownloadInputAction(
                text = actionText,
                icon = Icons.Filled.DownloadForOffline,
                prominent = true,
                enabled = actionEnabled,
                onClick = onStart,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DownloadInputAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    prominent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (prominent) {
                    Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))
                } else {
                    Brush.linearGradient(listOf(DSColors.mistGreen, DSColors.mistGreen))
                },
            )
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (prominent) Color.White else DSColors.fairway,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            color = if (prominent) Color.White else DSColors.fairway,
            style = DSTypography.bodyBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun DownloadSuccessDialog(
    platform: String,
    costCredits: Int?,
    isOnlineMemo: Boolean,
    showCreateMemoButton: Boolean,
    onOpenMemo: () -> Unit,
    onContinue: () -> Unit,
) {
    val costText = costCredits?.takeIf { it > 0 }?.let { "，并消耗 $it 积分" }.orEmpty()
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = DSSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .testTag("download-success-dialog")
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp))
                    .padding(DSSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DSColors.leafGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DSColors.leafGreen,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Text("下载完成", style = DSTypography.titleMedium, color = DSColors.inkGreen)
                    val savedObject = if (isOnlineMemo) "保存为在线视频 Memo" else "保存视频"
                    Text(
                        "已从 $platform $savedObject$costText。",
                        style = DSTypography.body,
                        color = DSColors.shadowGray,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                    if (showCreateMemoButton) {
                        Box(Modifier.fillMaxWidth().testTag("download-success-create-memo")) {
                            PrimaryAction("创建练球 Memo", enabled = true, onClick = onOpenMemo)
                        }
                        Box(Modifier.fillMaxWidth().testTag("download-success-continue")) {
                            VideoInfoConfirmSecondaryAction("继续下载", onContinue)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().testTag("download-success-complete")) {
                            PrimaryAction("完成", enabled = true, onClick = onContinue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PhaseCard(
    phase: DownloadPhase,
    progress: Double,
    elapsedSeconds: Int,
    error: String?,
    note: String?,
    isDirectUrlMode: Boolean,
    displayedUrl: String = "",
) {
    if (phase == DownloadPhase.IDLE && error == null && note == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .downloadCardSurface()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(DSColors.sandBeige),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = DSColors.trophyGold, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("开始前会先识别视频信息", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                Text("确认标题、大小和积分后再下载，避免误扣。", style = DSTypography.caption, color = DSColors.shadowGray)
            }
        }
        return
    }
    val (title, subtitle, tint) = when (phase) {
        DownloadPhase.IDLE -> arrayOf("操作失败", error ?: note ?: "请检查后重试。", DSColors.flagRed)
        DownloadPhase.FETCHING -> arrayOf("正在获取信息", "正在请求下载服务...", DSColors.fairway)
        DownloadPhase.READY -> arrayOf("可确认下载", "积分已冻结，取消会返还。", DSColors.fairway)
        DownloadPhase.CONFIRMING -> arrayOf("正在确认", "确认扣除积分并获取下载链接。", DSColors.fairway)
        DownloadPhase.DOWNLOADING -> arrayOf("正在下载", "请保持页面打开。", DSColors.fairway)
        DownloadPhase.CANCELLING -> arrayOf("正在取消", "正在返还冻结积分。", DSColors.shadowGray)
        DownloadPhase.COMPLETED -> if (isDirectUrlMode) {
            arrayOf("已保存到相册", "视频已经保存到系统相册。", DSColors.fairway)
        } else {
            arrayOf("已创建 Memo", "视频素材已经进入视频笔记。", DSColors.fairway)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .downloadCardSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (error == null) title as String else "操作失败", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                Text(
                    error ?: displayedUrl.takeIf {
                        it.isNotBlank() && phase != DownloadPhase.IDLE && phase != DownloadPhase.COMPLETED
                    } ?: note ?: subtitle as String,
                    style = DSTypography.caption,
                    color = if (error != null) DSColors.flagRed else DSColors.shadowGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${(progress * 100).toInt().coerceIn(0, 100)}%",
                color = if (error == null) tint as Color else DSColors.flagRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { progress.toFloat().coerceIn(0f, 1f) },
            color = DSColors.fairway,
            trackColor = DSColors.mistGreen,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◷ ${elapsedSeconds.coerceAtLeast(0)}s", style = DSTypography.label, color = DSColors.shadowGray)
            Spacer(Modifier.weight(1f))
            Text(
                if (phase == DownloadPhase.CANCELLING) "正在取消" else if (error != null) "需要处理" else "进行中",
                style = DSTypography.label,
                color = DSColors.shadowGray,
            )
        }
    }
}

@Composable
internal fun TodaySummaryCard(stats: V2DownloadStatisticsSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .downloadCardSurface()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadStatCell(
            title = "今日下载",
            value = stats.todayDownloadCount.toString(),
            subtitle = "次",
            modifier = Modifier.weight(1f),
        )
        CreditDivider()
        DownloadStatCell(
            title = "近 30 天",
            value = stats.last30DaysDownloadCount.toString(),
            subtitle = "次",
            modifier = Modifier.weight(1f),
        )
        CreditDivider()
        DownloadStatCell(
            title = "累计大小",
            value = stats.formattedTotalSize,
            subtitle = "",
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
internal fun SupportedDownloadCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .downloadCardSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("可接触页面", style = DSTypography.bodyBold, color = DSColors.inkGreen)
        SupportedDownloadRow(
            icon = Icons.Filled.MusicNote,
            title = "抖音视频下载",
            subtitle = "主流程支持直接下载",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.History,
            title = "异步下载管理",
            subtitle = "大文件可放到后台队列",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.CheckCircle,
            title = "视频信息确认",
            subtitle = "确认大小、积分和标题后再下载",
        )
        SupportedDownloadRow(
            icon = Icons.Filled.ShoppingBag,
            title = "积分充值",
            subtitle = "仍沿用现有充值页",
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
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DSSpacing.sm, bottom = 16.dp),
    )
}

private fun Modifier.downloadCardSurface(): Modifier {
    val shape = RoundedCornerShape(14.dp)
    return shadow(
        elevation = 8.dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.06f),
        spotColor = Color.Black.copy(alpha = 0.06f),
    )
        .clip(shape)
        .background(DSColors.cardWhite)
        .border(1.dp, DSColors.borderLine, shape)
}

@Composable
internal fun DownloadFailureDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提示") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
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

private const val ASYNC_DOWNLOAD_TIP_MIN_FILE_SIZE_BYTES = 10L * 1024L * 1024L

internal fun shouldSuggestAsyncDownload(
    info: DownloadVideoInfo?,
    jobId: String,
    acceptedJobId: String?,
): Boolean =
    acceptedJobId != jobId && (info?.fileSizeBytes ?: 0L) >= ASYNC_DOWNLOAD_TIP_MIN_FILE_SIZE_BYTES

internal fun formatAsyncDownloadFileSize(fileSizeBytes: Long?): String =
    "${((fileSizeBytes ?: 0L).coerceAtLeast(1L) / (1024L * 1024L)).coerceAtLeast(1L)}MB"

internal fun formatVideoInfoConfirmSize(fileSizeBytes: Long?): String {
    val bytes = fileSizeBytes ?: 0L
    return if (bytes <= 0L) "待确认" else String.format("%.1fMB", bytes.toDouble() / 1024.0 / 1024.0)
}

internal fun formatVideoInfoConfirmDuration(durationSeconds: Double?): String {
    val total = durationSeconds?.toInt() ?: 0
    return if (total <= 0) "待确认" else "%d:%02d".format(total / 60, total % 60)
}

@Composable
internal fun VideoInfoConfirmDialog(
    platform: String,
    info: DownloadVideoInfo?,
    creditsCost: Int,
    onConfirm: () -> Unit,
    onAsyncDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = DSSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .testTag("video-info-confirm-dialog")
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp))
                    .padding(DSSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DSColors.fairway.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DSColors.fairway,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Text("确认下载", style = DSTypography.titleMedium, color = DSColors.inkGreen)
                    Text(
                        info?.title?.takeIf(String::isNotBlank) ?: "未命名视频",
                        style = DSTypography.bodyBold,
                        color = DSColors.inkGreen,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                    VideoInfoConfirmRow("平台", platform.ifBlank { "待确认" })
                    VideoInfoConfirmRow("大小", formatVideoInfoConfirmSize(info?.fileSizeBytes))
                    VideoInfoConfirmRow("时长", formatVideoInfoConfirmDuration(info?.durationSec))
                    VideoInfoConfirmRow("积分", "$creditsCost 积分")
                }
                Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                    Box(Modifier.fillMaxWidth().testTag("video-info-confirm-primary")) {
                        PrimaryAction("确认下载", enabled = true, onClick = onConfirm)
                    }
                    Box(Modifier.fillMaxWidth().testTag("video-info-confirm-async")) {
                        VideoInfoConfirmSecondaryAction("异步下载", onAsyncDownload)
                    }
                    Box(Modifier.fillMaxWidth().testTag("video-info-confirm-cancel")) {
                        SecondaryAction("取消", onCancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoInfoConfirmRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.listMutedBg)
            .padding(horizontal = DSSpacing.md, vertical = DSSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = DSTypography.caption, color = DSColors.shadowGray)
        Text(value, style = DSTypography.bodyBold, color = DSColors.inkGreen)
    }
}

@Composable
private fun VideoInfoConfirmSecondaryAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(DSColors.mistGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = DSColors.fairway, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AsyncDownloadTipDialog(
    info: DownloadVideoInfo?,
    onAsyncDownload: () -> Unit,
    onContinueDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = DSSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .testTag("async-download-tip-dialog")
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(24.dp))
                    .padding(DSSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DSColors.fairway.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = DSColors.fairway,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
                ) {
                    Text("检测到大文件", style = DSTypography.titleMedium, color = DSColors.inkGreen)
                    Text(
                        "视频约 ${formatAsyncDownloadFileSize(info?.fileSizeBytes)}。可以放入后台任务，也可以继续等待当前下载完成。",
                        style = DSTypography.body,
                        color = DSColors.shadowGray,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                    Box(Modifier.fillMaxWidth().testTag("async-download-tip-background")) {
                        PrimaryAction("异步下载", enabled = true, onClick = onAsyncDownload)
                    }
                    Box(Modifier.fillMaxWidth().testTag("async-download-tip-foreground")) {
                        VideoInfoConfirmSecondaryAction("继续当前下载", onContinueDownload)
                    }
                    Box(Modifier.fillMaxWidth().testTag("async-download-tip-cancel")) {
                        SecondaryAction("取消", onCancel)
                    }
                }
            }
        }
    }
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
