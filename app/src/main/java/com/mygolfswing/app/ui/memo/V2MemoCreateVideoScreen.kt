package com.mygolfswing.app.ui.memo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.mygolfswing.app.data.store.V2GoalStore
import java.util.UUID
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.isPermissionPermanentlyDenied
import com.mygolfswing.app.ui.common.openAppSettings
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * 视频 Memo 创建页（对齐 iOS `V2MemoCreateVideoView`，3 个视频源）。
 *
 * 视频源（互斥三选一）：
 * - 拍视频 → 系统相机 `CaptureVideo` → 录完移到 App 私有目录 + 抽缩略图
 * - 选相册 → `PickVisualMedia.VideoOnly` → 复制到 App 私有目录 + 抽缩略图
 * - 在线视频 → 弹出"粘贴 URL"对话框（http/https）
 *
 * 选好后立即在卡片中央用 ExoPlayer 做小预览，可点击全屏播放。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2MemoCreateVideoScreen(
    onClose: () -> Unit = {},
    onSaved: ((UUID) -> Unit)? = null,
    onOpenOnlinePicker: (() -> Unit)? = null,
    pendingOnline: V2OnlineVideoSelection? = null,
    onPendingOnlineConsumed: () -> Unit = {},
    editingMemoId: UUID? = null,
    launchAlbumPicker: (((Uri?) -> Unit) -> Unit)? = null,
    cameraPermissionGranted: (() -> Boolean)? = null,
    launchCameraCapture: ((File, Uri, (Boolean) -> Unit) -> Unit)? = null,
    requestCameraPermission: ((String) -> Unit)? = null,
    adoptCameraVideo: (suspend (File) -> V2VideoFileStore.ImportResult?)? = null,
    importAlbumVideo: (suspend (Uri) -> V2VideoFileStore.ImportResult?)? = null,
    persistCreatedMemo: (suspend (V2Memo) -> Unit)? = null,
    persistUpdatedMemo: (suspend (V2Memo) -> Unit)? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val allMemos by V2MemoStore.allMemos.collectAsState()
    val editingMemo = remember(allMemos, editingMemoId) {
        editingMemoId?.let { id -> allMemos.firstOrNull { it.id == id } }
    }
    val isEditing = editingMemoId != null

    if (isEditing && editingMemo == null) {
        V2MissingEntityScreen(message = "视频 Memo 不存在或已被删除", onBack = onClose, title = "编辑记录")
        return
    }

    // 视频源状态
    var sourceKind by rememberSaveable(editingMemo?.id) { mutableStateOf(initialSourceKind(editingMemo)) }
    var localAbsolutePath by rememberSaveable(editingMemo?.id) { mutableStateOf(initialLocalPath(editingMemo)) }
    var localThumbPath by rememberSaveable(editingMemo?.id) { mutableStateOf(initialLocalThumbPath(editingMemo)) }
    var localDurationSec by rememberSaveable(editingMemo?.id) { mutableStateOf(editingMemo?.durationSec) }
    var onlineUrl by rememberSaveable(editingMemo?.id) {
        mutableStateOf(if (editingMemo?.isOnlineVideo == true) editingMemo.videoUrl else null)
    }
    var onlinePlatform by rememberSaveable(editingMemo?.id) { mutableStateOf(editingMemo?.platform) }
    var onlineThumbUrl by rememberSaveable(editingMemo?.id) {
        mutableStateOf(if (editingMemo?.isOnlineVideo == true) editingMemo.thumbnailUrl else null)
    }
    var onlineSourceTitle by rememberSaveable(editingMemo?.id) {
        mutableStateOf(
            if (editingMemo?.isOnlineVideo == true) {
                editingMemo.displayTitle ?: editingMemo.title
            } else {
                null
            },
        )
    }
    val originalLocalPath = remember(editingMemo?.id) { initialLocalPath(editingMemo) }
    val originalLocalThumbPath = remember(editingMemo?.id) { initialLocalThumbPath(editingMemo) }
    var showOnlineDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTagIds by rememberSaveable(
        editingMemo?.id,
        stateSaver = listSaver<Set<String>, String>(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(editingMemo?.tagIds.orEmpty().toSet()) }
    var selectedGoalIds by rememberSaveable(
        editingMemo?.id,
        stateSaver = listSaver<Set<UUID>, String>(
            save = { ids -> ids.map(UUID::toString) },
            restore = { values -> values.mapTo(mutableSetOf(), UUID::fromString) },
        ),
    ) { mutableStateOf(editingMemo?.linkedGoalIds.orEmpty().toSet()) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraPermissionDeniedPermanently by rememberSaveable { mutableStateOf(false) }

    fun protectedOriginalLocalMedia(): Set<String> =
        if (isEditing) setOfNotNull(originalLocalPath, originalLocalThumbPath) else emptySet()

    fun cleanupReplacedOriginalLocalMedia() {
        if (!isEditing) return
        deleteDraftLocalMedia(
            videoPath = originalLocalPath,
            thumbnailPath = originalLocalThumbPath,
            protectedPaths = setOfNotNull(localAbsolutePath, localThumbPath),
        )
    }

    // 系统相机
    fun handleCameraCaptureResult(ok: Boolean) {
        val file = pendingCameraPath?.let(::File)
        pendingCameraPath = null
        if (ok && file != null) {
            scope.launch {
                importing = true
                importError = null
                val r = try {
                    if (adoptCameraVideo != null) {
                        adoptCameraVideo(file)
                    } else {
                        V2VideoFileStore.adoptLocalFile(context, file)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    importError = memoVideoSaveError("录制失败，请重试", e.message)
                    null
                } finally {
                    importing = false
                }
                if (r == null) {
                    deleteFileCountingFreedBytes(file)
                    if (importError == null) importError = "录制失败，请重试"
                } else {
                    val previousVideoPath = localAbsolutePath
                    val previousThumbPath = localThumbPath
                    sourceKind = SourceKind.CAMERA
                    localAbsolutePath = r.absolutePath
                    localThumbPath = r.thumbnailPath
                    localDurationSec = r.durationSec
                    onlineUrl = null
                    onlineSourceTitle = null
                    deleteDraftLocalMedia(
                        videoPath = previousVideoPath,
                        thumbnailPath = previousThumbPath,
                        protectedPaths = setOfNotNull(r.absolutePath, r.thumbnailPath) + protectedOriginalLocalMedia(),
                    )
                }
            }
        } else {
            deleteFileCountingFreedBytes(file)
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = ::handleCameraCaptureResult,
    )

    fun startCameraCapture() {
        val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
        pendingCameraPath = file.absolutePath
        runCatching {
            if (launchCameraCapture != null) {
                launchCameraCapture(file, uri, ::handleCameraCaptureResult)
            } else {
                captureLauncher.launch(uri)
            }
        }.onFailure {
            pendingCameraPath = null
            deleteFileCountingFreedBytes(file)
            importError = memoVideoSaveError("无法打开相机，请稍后重试或改从相册选择", it.message)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            cameraPermissionDeniedPermanently = false
            startCameraCapture()
        } else {
            cameraPermissionDeniedPermanently = context.isPermissionPermanentlyDenied(Manifest.permission.CAMERA)
            importError = if (cameraPermissionDeniedPermanently) {
                "请在系统设置中开启相机权限后再录制视频"
            } else {
                "需要相机权限才能录制视频"
            }
        }
    }

    fun startCameraPermissionRequest() {
        runCatching {
            if (requestCameraPermission != null) {
                requestCameraPermission(Manifest.permission.CAMERA)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }.onFailure {
            importError = memoVideoSaveError("无法请求相机权限，请稍后重试或改从相册选择", it.message)
        }
    }

    // 表单状态
    var title by rememberSaveable(editingMemo?.id) { mutableStateOf(editingMemo?.displayTitle ?: editingMemo?.title ?: "") }
    var note by rememberSaveable(editingMemo?.id) { mutableStateOf(editingMemo?.text ?: "") }
    var noteExpanded by rememberSaveable(editingMemo?.id) {
        mutableStateOf(!editingMemo?.text.isNullOrBlank())
    }

    androidx.compose.runtime.LaunchedEffect(pendingOnline) {
        val pick = pendingOnline ?: return@LaunchedEffect
        val previousVideoPath = localAbsolutePath
        val previousThumbPath = localThumbPath
        onlineUrl = pick.url
        onlinePlatform = pick.platform
        onlineThumbUrl = pick.thumbnailUrl
        onlineSourceTitle = pick.title.trim().ifBlank { null }
        localDurationSec = pick.durationSec
        if (title.isBlank()) {
            title = prefilledOnlineVideoMemoTitle(pick).take(VideoMemoTitleMaxLength)
        }
        if (note.isBlank()) {
            val draftNote = prefilledOnlineVideoMemoNote(pick)
            if (draftNote.isNotBlank()) {
                note = draftNote.take(VideoMemoNoteMaxLength)
                noteExpanded = true
            }
        }
        sourceKind = SourceKind.ONLINE
        localAbsolutePath = null
        localThumbPath = null
        deleteDraftLocalMedia(previousVideoPath, previousThumbPath, protectedPaths = protectedOriginalLocalMedia())
        onPendingOnlineConsumed()
    }

    // 系统相册
    fun handleAlbumPickerResult(uri: Uri?) {
        if (uri != null) {
            scope.launch {
                importing = true
                importError = null
                val r = try {
                    if (importAlbumVideo != null) {
                        importAlbumVideo(uri)
                    } else {
                        V2VideoFileStore.importVideo(context, uri)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    importError = memoVideoSaveError("无法读取视频，请检查权限或换一个视频", e.message)
                    null
                } finally {
                    importing = false
                }
                if (r == null) {
                    if (importError == null) importError = "无法读取视频，请检查权限或换一个视频"
                } else {
                    val previousVideoPath = localAbsolutePath
                    val previousThumbPath = localThumbPath
                    sourceKind = SourceKind.ALBUM
                    localAbsolutePath = r.absolutePath
                    localThumbPath = r.thumbnailPath
                    localDurationSec = r.durationSec
                    onlineUrl = null
                    onlineSourceTitle = null
                    deleteDraftLocalMedia(
                        videoPath = previousVideoPath,
                        thumbnailPath = previousThumbPath,
                        protectedPaths = setOfNotNull(r.absolutePath, r.thumbnailPath) + protectedOriginalLocalMedia(),
                    )
                }
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = ::handleAlbumPickerResult,
    )

    val hasVideo = localAbsolutePath != null || onlineUrl != null
    val isOnline = onlineUrl != null
    val videoStringForStore = onlineUrl ?: localAbsolutePath?.let { "file://$it" }
    val thumbStringForStore = localThumbPath?.let { "file://$it" }
    val training = when (sourceKind) {
        SourceKind.CAMERA -> "拍摄"
        SourceKind.ALBUM -> "本地上传"
        SourceKind.ONLINE -> "在线视频"
        null -> null
    }
    val canSave = hasVideo && title.isNotBlank() && !importing && !saving
    val hasDraft = if (isEditing) {
        title != (editingMemo?.displayTitle ?: editingMemo?.title ?: "") ||
            note != (editingMemo?.text ?: "") ||
            selectedTagIds != editingMemo?.tagIds.orEmpty().toSet() ||
            selectedGoalIds != editingMemo?.linkedGoalIds.orEmpty().toSet() ||
            videoStringForStore != editingMemo?.videoUrl ||
            (if (isOnline) onlineThumbUrl else thumbStringForStore) != editingMemo?.thumbnailUrl ||
            localDurationSec != editingMemo?.durationSec ||
            isOnline != (editingMemo?.isOnlineVideo == true) ||
            onlinePlatform != editingMemo?.platform ||
            training != editingMemo?.trainingMethod
    } else {
        title.isNotBlank() || note.isNotBlank() || hasVideo ||
            selectedTagIds.isNotEmpty() || selectedGoalIds.isNotEmpty()
    }

    fun attemptClose() {
        if (saving) return
        if (hasDraft) {
            focusManager.clearFocus()
            keyboardController?.hide()
            showDiscardConfirm = true
        } else {
            onClose()
        }
    }

    fun buildAndSave() {
        if (saving || !canSave) return
        if (!isOnline && !memoVideoPlayable(videoStringForStore)) {
            importError = "视频文件已不可用，请重新选择或录制"
            localAbsolutePath = null
            localThumbPath = null
            localDurationSec = null
            sourceKind = null
            return
        }
        saving = true
        saveError = null
        val draftTitle = title.trim().ifBlank { "视频 Memo" }
        val draftNote = note.trim()
        val draftTagIds = selectedTagIds.toList()
        val draftGoalIds = selectedGoalIds.toSet()
        val draftVideoUrl = videoStringForStore
        val draftThumbnailUrl = if (isOnline) onlineThumbUrl else thumbStringForStore
        val draftDurationSec = localDurationSec
        val draftTraining = training
        val draftIsOnline = isOnline
        val draftPlatform = if (isOnline) onlinePlatform else null
        val editingSnapshot = editingMemo
        scope.launch {
            try {
                val tagNames = V2CategoryStore.tagNamesFor(draftTagIds.toSet())
                val topic = V2MemoBridge.inferTopic(
                    category = V2CategoryStore.groupForItemId(draftTagIds.firstOrNull() ?: "")?.name,
                    tags = tagNames,
                    title = draftTitle,
                )
                if (editingSnapshot != null) {
                    val updatedMemo = editingSnapshot.copy(
                        title = draftTitle,
                        displayTitle = draftTitle,
                        text = draftNote,
                        videoUrl = draftVideoUrl,
                        durationSec = draftDurationSec,
                        topic = topic,
                        tags = tagNames,
                        tagIds = draftTagIds,
                        linkedGoalIds = draftGoalIds.toList(),
                        thumbnailUrl = draftThumbnailUrl,
                        trainingMethod = draftTraining,
                        isOnlineVideo = draftIsOnline,
                        platform = draftPlatform,
                        metaText = buildMetaText(isOnline = draftIsOnline, durationSec = draftDurationSec),
                        thumbnailKind = V2MemoBridge.thumbnailKindFor(topic),
                    )
                    if (persistUpdatedMemo != null) {
                        persistUpdatedMemo(updatedMemo)
                    } else {
                        V2MemoStore.updateAndWait(updatedMemo)
                    }
                    V2GoalStore.replaceMemoLinks(editingSnapshot.id, draftGoalIds)
                    cleanupReplacedOriginalLocalMedia()
                    onSaved?.invoke(editingSnapshot.id) ?: onClose()
                } else {
                    val memo = V2Memo.mixedMemo(
                        displayTitle = draftTitle,
                        body = draftNote,
                        videoUrl = draftVideoUrl,
                        durationSec = draftDurationSec,
                        topic = topic,
                        tags = tagNames,
                        metaText = buildMetaText(isOnline = draftIsOnline, durationSec = draftDurationSec),
                    ).apply {
                        tagIds = draftTagIds
                        thumbnailUrl = draftThumbnailUrl
                        trainingMethod = draftTraining
                        isOnlineVideo = draftIsOnline
                        platform = draftPlatform
                        linkedGoalIds = draftGoalIds.toList()
                        thumbnailKind = V2MemoBridge.thumbnailKindFor(topic)
                    }
                    if (persistCreatedMemo != null) {
                        persistCreatedMemo(memo)
                    } else {
                        V2MemoStore.createAndWait(memo)
                    }
                    draftGoalIds.forEach { goalId -> V2GoalStore.linkMemo(memo.id, goalId) }
                    onSaved?.invoke(memo.id) ?: onClose()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = memoVideoThrowableError("保存失败，请稍后重试", e)
            } finally {
                saving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F0)),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            NavBar(
                title = if (isEditing) "编辑记录" else "新建记录",
                onClose = ::attemptClose,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                VideoSourceBlock(
                    sourceKind = sourceKind,
                    localPath = localAbsolutePath,
                    localThumbPath = localThumbPath,
                    onlineUrl = onlineUrl,
                    onlineThumbUrl = onlineThumbUrl,
                    onlineSourceTitle = onlineSourceTitle,
                    durationSec = localDurationSec,
                    importing = importing,
                    editing = isEditing,
                    onPickCamera = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val permissionGranted = cameraPermissionGranted?.invoke()
                            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        if (permissionGranted) {
                            cameraPermissionDeniedPermanently = false
                            startCameraCapture()
                        } else if (cameraPermissionDeniedPermanently) {
                            importError = "请在系统设置中开启相机权限后再录制视频"
                        } else {
                            startCameraPermissionRequest()
                        }
                    },
                    onPickAlbum = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        runCatching {
                            if (launchAlbumPicker != null) {
                                launchAlbumPicker(::handleAlbumPickerResult)
                            } else {
                                pickLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly,
                                    ),
                                )
                            }
                        }.onFailure {
                            importError = memoVideoSaveError("无法打开相册，请稍后重试", it.message)
                        }
                    },
                    onPickOnline = {
                        if (onOpenOnlinePicker != null) onOpenOnlinePicker()
                        else showOnlineDialog = true
                    },
                )
                TitleSection(title = title, onChange = { if (it.length <= VideoMemoTitleMaxLength) title = it })
                NoteSection(
                    note = note,
                    expanded = noteExpanded,
                    onToggleExpand = { noteExpanded = !noteExpanded },
                    onChange = { if (it.length <= VideoMemoNoteMaxLength) note = it },
                )
                V2MemoTagPicker(
                    selectedTagIds = selectedTagIds,
                    onSelectedTagIdsChange = { selectedTagIds = it },
                )
            }
        }
        BottomSaveBar(
            canSave = canSave,
            saving = saving,
            editing = isEditing,
            onSave = ::buildAndSave,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showOnlineDialog && onOpenOnlinePicker == null) {
        OnlineUrlDialog(
            onConfirm = { url ->
                showOnlineDialog = false
                deleteDraftLocalMedia(localAbsolutePath, localThumbPath, protectedPaths = protectedOriginalLocalMedia())
                onlineUrl = url
                onlinePlatform = detectPlatform(url)
                onlineSourceTitle = null
                sourceKind = SourceKind.ONLINE
                localAbsolutePath = null
                localThumbPath = null
                localDurationSec = null
            },
            onDismiss = { showOnlineDialog = false },
        )
    }

    if (importError != null) {
        VideoMemoOperationFailureDialog(
            message = importError.orEmpty(),
            primaryLabel = if (cameraPermissionDeniedPermanently) "打开设置" else "好",
            secondaryLabel = if (cameraPermissionDeniedPermanently) "取消" else null,
            onPrimary = {
                val openSettings = cameraPermissionDeniedPermanently
                importError = null
                if (openSettings && !context.openAppSettings()) {
                    Toast.makeText(context, "系统设置暂时无法打开，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { importError = null },
        )
    }

    saveError?.let { message ->
        VideoMemoOperationFailureDialog(
            message = message,
            primaryLabel = "好",
            onPrimary = { saveError = null },
            onDismiss = { saveError = null },
        )
    }

    if (showDiscardConfirm) {
        VideoMemoDiscardSheet(
            editing = isEditing,
            onContinue = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                if (!isEditing) {
                    deleteDraftLocalMedia(localAbsolutePath, localThumbPath)
                    deleteFileCountingFreedBytes(pendingCameraPath?.let(::File))
                } else {
                    deleteDraftLocalMedia(localAbsolutePath, localThumbPath, protectedPaths = protectedOriginalLocalMedia())
                }
                onClose()
            },
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoMemoDiscardSheet(
    editing: Boolean,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onContinue,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (editing) "放弃未保存的修改？" else "放弃这条 Memo？",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (editing) "已修改的内容将丢失" else "未保存的内容将丢失",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onDiscard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("放弃", color = Color(0xFFC0392B), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(onClick = onContinue),
                contentAlignment = Alignment.Center,
            ) {
                Text("继续编辑", color = Color(0xFF2D6A4F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal enum class SourceKind { CAMERA, ALBUM, ONLINE }

private const val VideoMemoTitleMaxLength = 24
private const val VideoMemoNoteMaxLength = 240

private fun initialSourceKind(memo: V2Memo?): SourceKind? = when {
    memo == null -> null
    memo.isOnlineVideo -> SourceKind.ONLINE
    memo.trainingMethod == "拍摄" -> SourceKind.CAMERA
    else -> SourceKind.ALBUM
}

private fun initialLocalPath(memo: V2Memo?): String? =
    memo?.videoUrl?.takeUnless { memo.isOnlineVideo }?.removePrefix("file://")

private fun initialLocalThumbPath(memo: V2Memo?): String? =
    memo?.thumbnailUrl?.takeUnless { memo.isOnlineVideo }?.removePrefix("file://")

private fun detectPlatform(url: String): String = when {
    url.contains("douyin.com", ignoreCase = true) || url.contains("iesdouyin.com", ignoreCase = true) -> "抖音"
    url.contains("bilibili.com", ignoreCase = true) || url.contains("b23.tv", ignoreCase = true) -> "B站"
    url.contains("xiaohongshu.com", ignoreCase = true) || url.contains("xhslink.com", ignoreCase = true) -> "小红书"
    url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
    else -> "在线平台"
}

private fun videoMemoPlatformDisplayName(platform: String, url: String): String {
    val trimmed = platform.trim()
    return when {
        trimmed.equals("哔哩哔哩", ignoreCase = true) || trimmed.equals("bilibili", ignoreCase = true) -> "B站"
        trimmed.equals("douyin", ignoreCase = true) -> "抖音"
        trimmed.equals("xiaohongshu", ignoreCase = true) -> "小红书"
        trimmed.equals("youtube", ignoreCase = true) -> "YouTube"
        trimmed.equals("其他", ignoreCase = true) || trimmed.equals("online", ignoreCase = true) || trimmed.isBlank() -> detectPlatform(url)
        else -> trimmed
    }
}

private fun prefilledOnlineVideoMemoTitle(selection: V2OnlineVideoSelection): String {
    val cleanedTitle = selection.title.trim()
    if (cleanedTitle.isNotEmpty() && cleanedTitle != selection.url.trim()) return cleanedTitle
    val platform = videoMemoPlatformDisplayName(selection.platform, selection.url)
    return "${platform}视频"
}

private fun prefilledOnlineVideoMemoNote(selection: V2OnlineVideoSelection): String {
    val lines = mutableListOf<String>()
    if (selection.durationSec != null && selection.durationSec > 0) {
        lines.add("时长：${formatVideoMemoDuration(selection.durationSec)}")
    }
    val url = selection.url.trim()
    if (url.isNotEmpty()) {
        val platform = videoMemoPlatformDisplayName(selection.platform, url)
        lines.add("来源：$platform $url")
    }
    return lines.joinToString("\n")
}

private fun buildMetaText(isOnline: Boolean, durationSec: Double?): String {
    val parts = mutableListOf<String>()
    if (isOnline) parts.add("在线视频") else parts.add("本地视频")
    if (durationSec != null && durationSec > 0) {
        parts.add(formatVideoMemoDuration(durationSec))
    }
    return parts.joinToString(" · ")
}

private fun formatVideoMemoDuration(durationSec: Double): String {
    val total = durationSec.toInt()
    val m = total / 60
    val s = total % 60
    return String.format(Locale.ROOT, "%d:%02d", m, s)
}

internal fun deleteDraftLocalMedia(
    videoPath: String?,
    thumbnailPath: String?,
    protectedPaths: Set<String> = emptySet(),
): Long {
    val protectedCanonicalPaths = protectedPaths.mapNotNullTo(mutableSetOf()) { path ->
        runCatching { File(path).canonicalPath }.getOrNull()
    }
    return listOfNotNull(videoPath, thumbnailPath).sumOf { path ->
        val file = File(path.removePrefix("file://"))
        val canonicalPath = runCatching { file.canonicalPath }.getOrNull()
        if (canonicalPath != null && canonicalPath in protectedCanonicalPaths) {
            0L
        } else {
            deleteFileCountingFreedBytes(file)
        }
    }
}

// =================================================================
// 标题 / 笔记 / 底部栏
// =================================================================

@Composable
private fun TitleSection(title: String, onChange: (String) -> Unit) {
    var titleFocused by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("标题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = title,
                onValueChange = onChange,
                textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1B2A21)),
                cursorBrush = SolidColor(Color(0xFF2D6A4F)),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { titleFocused = it.isFocused }
                    .testTag("video-memo-title-field"),
                singleLine = true,
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text("给这条记录起个标题…", fontSize = 15.sp, color = Color(0xFF9CA3AF))
                    }
                    inner()
                },
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (titleFocused) Color(0xFF2D6A4F) else Color(0xFFE5E7EB)),
        )
    }
}

@Composable
internal fun VideoMemoOperationFailureDialog(
    message: String,
    title: String = "操作失败",
    tagPrefix: String = if (title == "保存失败") {
        "text-memo-save-failure"
    } else {
        "video-memo-operation-failure"
    },
    primaryLabel: String,
    secondaryLabel: String? = null,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogTag = "$tagPrefix-dialog"
    val secondaryTag = "$tagPrefix-secondary"
    val primaryTag = "$tagPrefix-primary"
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier.width(270.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F7).copy(alpha = 0.98f))
                    .testTag(dialogTag),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        title,
                        color = Color(0xFF1C1C1E),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        message,
                        color = Color(0xFF3A3A3C),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD1D1D6)))
                Row(Modifier.fillMaxWidth().height(44.dp)) {
                    if (secondaryLabel != null) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxSize().clickable(onClick = onDismiss)
                                .testTag(secondaryTag),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(secondaryLabel, color = Color(0xFF007AFF), fontSize = 17.sp)
                        }
                        Box(Modifier.width(1.dp).fillMaxSize().background(Color(0xFFD1D1D6)))
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize().clickable(onClick = onPrimary)
                            .testTag(primaryTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            primaryLabel,
                            color = Color(0xFF007AFF),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteSection(
    note: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (String) -> Unit,
) {
    val headerShape = if (expanded) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    } else {
        RoundedCornerShape(12.dp)
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(headerShape)
                .background(Color(0xFFFAF7F0))
                .border(1.dp, Color(0xFFE5E7EB), headerShape)
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (note.isEmpty()) "加点笔记…" else "笔记",
                fontSize = 15.sp,
                color = if (note.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF1B2A21),
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(13.dp),
            )
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .offset(y = (-1).dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFFFAF7F0))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(16.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = onChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1B2A21)),
                    cursorBrush = SolidColor(Color(0xFF2D6A4F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                )
                if (note.isEmpty()) {
                    Text(
                        "今天练习推杆时注意力不集中…",
                        fontSize = 15.sp,
                        color = Color(0xFFC5C5C5),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSaveBar(
    canSave: Boolean,
    saving: Boolean,
    editing: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF7F0))
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (editing) "保存修改，不会自动分享" else "先保存。分享到空间是另一步",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (canSave) Color(0xFF2D6A4F) else Color(0xFF2D6A4F).copy(alpha = 0.4f))
                    .testTag("video-memo-save")
                    .clickable(enabled = canSave, onClick = onSave),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (saving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (saving) {
                        if (editing) "更新中…" else "保存中…"
                    } else if (editing) {
                        "保存修改"
                    } else {
                        "保存"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// =================================================================
// NavBar
// =================================================================

@Composable
private fun NavBar(
    title: String,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(Color(0xFFFAF7F0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color(0xFF1B2A21), modifier = Modifier.size(13.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(Modifier.size(32.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB)),
        )
    }
}

// =================================================================
// 视频源选择卡片 + 预览
// =================================================================

@OptIn(UnstableApi::class)
@Composable
internal fun VideoSourceBlock(
    sourceKind: SourceKind?,
    localPath: String?,
    localThumbPath: String?,
    onlineUrl: String?,
    onlineThumbUrl: String?,
    onlineSourceTitle: String?,
    durationSec: Double?,
    importing: Boolean,
    editing: Boolean,
    onPickCamera: () -> Unit,
    onPickAlbum: () -> Unit,
    onPickOnline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("视频来源", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))

        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    "视频源已锁定，编辑模式下不可更换",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (!editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SourceChip(
                    icon = Icons.Filled.Videocam,
                    label = "拍视频",
                    selected = sourceKind == SourceKind.CAMERA,
                    modifier = Modifier.weight(1f),
                    onClick = onPickCamera,
                )
                SourceChip(
                    icon = Icons.Filled.Photo,
                    label = "选相册",
                    selected = sourceKind == SourceKind.ALBUM,
                    modifier = Modifier.weight(1f),
                    onClick = onPickAlbum,
                )
                SourceChip(
                    icon = Icons.Filled.Link,
                    label = "在线视频",
                    selected = sourceKind == SourceKind.ONLINE,
                    modifier = Modifier.weight(1f),
                    onClick = onPickOnline,
                )
            }
        }

        PreviewArea(
            sourceKind = sourceKind,
            localPath = localPath,
            localThumbPath = localThumbPath,
            onlineUrl = onlineUrl,
            onlineThumbUrl = onlineThumbUrl,
            onlineSourceTitle = onlineSourceTitle,
            durationSec = durationSec,
            importing = importing,
        )
    }
}

@Composable
private fun SourceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFEBF5EC) else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF2D6A4F) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PreviewArea(
    sourceKind: SourceKind?,
    localPath: String?,
    localThumbPath: String?,
    onlineUrl: String?,
    onlineThumbUrl: String?,
    onlineSourceTitle: String?,
    durationSec: Double?,
    importing: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(202.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2E22)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            importing -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("处理视频中…", color = Color.White, fontSize = 12.sp)
                }
            }
            localPath != null -> LocalPreview(localPath)
            onlineUrl != null -> OnlinePreview(
                thumbnailUrl = onlineThumbUrl,
                sourceTitle = onlineSourceTitle,
            )
            else -> EmptyPlaceholder()
        }

        if (!importing && onlineUrl != null) {
            VideoPreviewPlayButton(modifier = Modifier.align(Alignment.Center))
        }

        if (!importing && (localPath != null || onlineUrl != null)) {
            Icon(
                imageVector = Icons.Filled.OpenInFull,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(12.dp),
            )
        }

        if (durationSec != null && durationSec > 0) {
            val total = durationSec.toInt()
            val m = total / 60
            val s = total % 60
            Text(
                text = String.format(Locale.ROOT, "%d:%02d", m, s),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LocalPreview(absolutePath: String) {
    val context = LocalContext.current
    var isPlaying by remember(absolutePath) { mutableStateOf(false) }
    val player = remember(absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("file://$absolutePath"))
            volume = 0f
            playWhenReady = false
            prepare()
            seekTo(0)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                    isPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { it.player = player },
        )
        if (!isPlaying) {
            VideoPreviewPlayButton(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun OnlinePreview(thumbnailUrl: String?, sourceTitle: String?) {
    if (!thumbnailUrl.isNullOrBlank()) {
        Box(Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            1.0f to Color.Transparent,
                        ),
                    ),
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = Color(0xFF1A2E22),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = sourceTitle?.takeIf { it.isNotBlank() } ?: "在线视频",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.OndemandVideo,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text("先选择或拍摄一段视频", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

@Composable
private fun VideoPreviewPlayButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(width = 2.dp, color = Color.White.copy(alpha = 0.7f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp).offset(x = 1.5.dp),
        )
    }
}

// =================================================================
// 在线视频 URL 输入对话框
// =================================================================

@Composable
private fun OnlineUrlDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding(),
        onDismissRequest = onDismiss,
        title = { Text("在线视频 URL") },
        text = {
            Column {
                Text(
                    text = "粘贴可以直接播放的视频地址（https 开头，支持 mp4 / m3u8）。",
                    fontSize = 12.sp,
                    color = Color(0xFF7B8A82),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = Color(0xFFC0392B), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = V2OnlineVideoMetadataFetcher.cleanVideoUrl(input)
                if (!trimmed.startsWith("https://", ignoreCase = true)) {
                    error = "URL 必须以 https 开头"
                } else {
                    onConfirm(trimmed)
                }
            }) { Text("使用此地址") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

internal fun memoVideoSaveError(base: String, debugDetail: String?): String {
    return memoVideoSaveError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun memoVideoSaveError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun memoVideoThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return memoVideoSaveError(base, error.message)
}
