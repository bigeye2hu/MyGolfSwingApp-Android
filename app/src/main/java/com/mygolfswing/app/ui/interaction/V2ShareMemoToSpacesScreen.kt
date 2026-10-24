package com.mygolfswing.app.ui.interaction

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachShareRequestIds
import com.mygolfswing.app.data.coach.V2CoachUserFacingException
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.V2RoomPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun V2ShareMemoToSpacesScreen(
    memo: V2Memo,
    onBack: () -> Unit,
    loadSpaces: suspend () -> Result<List<V2CoachConnection>> = {
        ServiceLocator.coachWorkspaceRepo.studentConnections()
    },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var spaces by remember { mutableStateOf<List<V2CoachConnection>>(emptyList()) }
    var loadingSpaces by remember { mutableStateOf(true) }
    var selectedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var frozenIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var successfulIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var failedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var uploadedAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    val batchId = rememberSaveable { UUID.randomUUID().toString() }
    val uploadId = rememberSaveable { V2CoachShareRequestIds.upload(memo.id.toString(), batchId) }
    var sharing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var completion by rememberSaveable { mutableStateOf<String?>(null) }
    val hasShareableSource = remember(memo.id, memo.videoUrl) {
        memoHasShareableVideoSource(memo)
    }

    LaunchedEffect(Unit) {
        loadSpaces()
            .onSuccess { connections ->
                spaces = connections.filter { it.status == "active" }
                    .sortedWith(compareByDescending<V2CoachConnection> { it.pinned }.thenByDescending { it.lastActivityAt.orEmpty() })
            }
            .onFailure { error = coachThrowableError("暂时无法读取挥呗空间，请稍后重试", it) }
        loadingSpaces = false
    }

    val allDelivered = frozenIds.isNotEmpty() && successfulIds.toSet().containsAll(frozenIds)
    val sendTargetIds = if (failedIds.isNotEmpty()) failedIds else selectedIds
    val title = memo.displayTitle?.takeIf(String::isNotBlank)
        ?: memo.title?.takeIf(String::isNotBlank)
        ?: "练习记录"

    fun share() {
        if (allDelivered) {
            onBack()
            return
        }
        if (sendTargetIds.isEmpty() || sharing) return
        if (frozenIds.isEmpty()) frozenIds = selectedIds
        sharing = true
        error = null
        completion = null
        scope.launch {
            var prepared: PreparedShareVideo? = null
            var cover: File? = null
            try {
                var assetId = uploadedAssetId
                if (assetId == null) {
                    progress = "正在准备视频"
                    prepared = prepareShareVideo(context, memo)
                    cover = createShareCover(context, prepared.file)
                    progress = "正在上传一次原视频"
                    var lastUploadError: Throwable? = null
                    val uploadCandidates = frozenIds.ifEmpty { sendTargetIds }
                    for (connectionId in uploadCandidates) {
                        try {
                            assetId = ServiceLocator.coachWorkspaceRepo.uploadWorkspaceVideo(
                                connectionId = connectionId,
                                videoFile = prepared.file,
                                coverFile = cover,
                                clientUploadId = uploadId,
                                title = title,
                                tags = memo.tags,
                                durationSeconds = memo.durationSec,
                            ).getOrThrow()
                            break
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (throwable: Throwable) {
                            lastUploadError = throwable
                        }
                    }
                    if (assetId == null) {
                        throw lastUploadError
                            ?: V2CoachUserFacingException("当前所选空间暂时不可用，请刷新后重试")
                    }
                    uploadedAssetId = assetId
                }

                val nextFailed = failedIds.toMutableSet().apply { removeAll(sendTargetIds.toSet()) }
                val nextSuccessful = successfulIds.toMutableSet()
                sendTargetIds.forEachIndexed { index, connectionId ->
                    progress = "正在分享 ${index + 1}/${sendTargetIds.size}"
                    val result = ServiceLocator.coachWorkspaceRepo.createAssetPost(
                        connectionId = connectionId,
                        title = title,
                        text = note,
                        assetId = assetId,
                        clientRequestId = V2CoachShareRequestIds.post(memo.id.toString(), batchId, connectionId),
                        requiresCoachReview = false,
                        sourceRecordId = memo.id.toString(),
                    )
                    if (result.isSuccess) {
                        nextSuccessful += connectionId
                        nextFailed -= connectionId
                    } else {
                        nextFailed += connectionId
                    }
                    successfulIds = nextSuccessful.toList()
                    failedIds = nextFailed.toList()
                }
                val successNames = spaces.filter { it.connectionId in nextSuccessful }
                    .map { it.otherPersonName() }
                if (nextFailed.isEmpty()) {
                    completion = if (successNames.isEmpty()) "分享完成"
                    else "已分别分享到：${successNames.joinToString("、")}"
                } else {
                    completion = successNames.takeIf(List<String>::isNotEmpty)
                        ?.let { "已分享到：${it.joinToString("、")}" }
                    error = "还有 ${nextFailed.size} 个空间未送达；再次点击只会重试失败项。"
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                error = coachThrowableError("分享暂时未完成，视频仍保留，可以重试", throwable)
            } finally {
                if (prepared?.temporary == true) prepared.file.delete()
                cover?.delete()
                progress = ""
                sharing = false
            }
        }
    }

    V2ShareMemoToSpacesPage(
        memo = memo,
        title = title,
        spaces = spaces,
        loadingSpaces = loadingSpaces,
        selectedIds = selectedIds,
        frozenIds = frozenIds,
        successfulIds = successfulIds,
        failedIds = failedIds,
        note = note,
        sharing = sharing,
        progress = progress,
        error = error,
        completion = completion,
        actionTitle = when {
            sharing -> progress.ifBlank { "正在准备视频" }
            allDelivered -> "完成"
            failedIds.isNotEmpty() -> "重试 ${failedIds.size} 个失败空间"
            else -> "分享到 ${selectedIds.size} 个空间"
        },
        actionEnabled = allDelivered || (hasShareableSource && !sharing && sendTargetIds.isNotEmpty()),
        onBack = onBack,
        onToggleSpace = { connectionId ->
            if (!sharing && frozenIds.isEmpty()) {
                selectedIds = if (connectionId in selectedIds) selectedIds - connectionId
                else selectedIds + connectionId
            }
        },
        onNoteChange = { if (!sharing && frozenIds.isEmpty()) note = it.take(1_000) },
        onAction = ::share,
    )
}

@Composable
internal fun V2ShareMemoToSpacesPage(
    memo: V2Memo,
    title: String,
    spaces: List<V2CoachConnection>,
    loadingSpaces: Boolean,
    selectedIds: List<String>,
    frozenIds: List<String>,
    successfulIds: List<String>,
    failedIds: List<String>,
    note: String,
    sharing: Boolean,
    progress: String,
    error: String?,
    completion: String?,
    actionTitle: String,
    actionEnabled: Boolean,
    onBack: () -> Unit,
    onToggleSpace: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAction: () -> Unit,
) {
    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
        ) {
            ShareSpacesTopBar(enabled = !sharing, onBack = onBack)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { ShareMemoPreview(memo, title) }
                item { SharePrivacyHint() }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("选择接收对象", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (spaces.isNotEmpty()) {
                            Text("已选 ${selectedIds.size}", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                when {
                    loadingSpaces -> item {
                        Box(
                            Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(17.dp)).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = DSColors.fairway, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) }
                    }
                    spaces.isEmpty() -> item { ShareSpacesEmptyState() }
                    else -> items(spaces, key = { it.connectionId }) { connection ->
                        ShareSpaceRow(
                            connection = connection,
                            selected = connection.connectionId in selectedIds,
                            succeeded = connection.connectionId in successfulIds,
                            failed = connection.connectionId in failedIds,
                            enabled = !sharing && connection.connectionId !in successfulIds && frozenIds.isEmpty(),
                            onClick = { onToggleSpace(connection.connectionId) },
                        )
                    }
                }
                item {
                    ShareNoteSection(
                        note = note,
                        enabled = !sharing && frozenIds.isEmpty(),
                        onNoteChange = onNoteChange,
                    )
                }
                error?.let { message -> item { ShareResultBanner(message, true) } }
                completion?.let { message -> item { ShareResultBanner(message, false) } }
            }
            ShareSpacesBottomAction(
                sharing = sharing,
                progress = progress,
                title = actionTitle,
                enabled = actionEnabled,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ShareSpacesTopBar(enabled: Boolean, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).background(DSColors.warmWhite.copy(alpha = 0.96f)),
    ) {
        Text(
            "取消",
            color = if (enabled) DSColors.deepGreen else DSColors.shadowGray.copy(alpha = 0.45f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, onClick = onBack).padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            "分享到空间",
            color = DSColors.deepGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SharePrivacyHint() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.70f)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.Security, null, tint = DSColors.deepGreen, modifier = Modifier.size(18.dp))
        Text(
            "将分别分享到所选空间，不同空间的留言互不可见。",
            color = DSColors.deepGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ShareSpacesEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White).padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.PersonOff, null, tint = DSColors.shadowGray, modifier = Modifier.size(22.dp))
        Text("还没有可分享的空间", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("先到“挥呗空间”连接教练或添加好友", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ShareNoteSection(
    note: String,
    enabled: Boolean,
    onNoteChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("附言（可选）", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            enabled = enabled,
            textStyle = androidx.compose.ui.text.TextStyle(color = DSColors.deepGreen, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(15.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(15.dp)).padding(13.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (note.isBlank()) Text("例如：帮我看看下杆时的重心", color = DSColors.shadowGray, fontSize = 13.sp)
                    innerTextField()
                }
            },
        )
        Text(
            "同一空间再次分享会回到原来的讨论。附言只进入所选空间，不会带走其他空间的回复。",
            color = DSColors.shadowGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShareSpacesBottomAction(
    sharing: Boolean,
    progress: String,
    title: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (sharing && progress.isNotBlank()) {
            Text(progress, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onAction,
            enabled = enabled && !sharing,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DSColors.fairway,
                disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.45f),
                contentColor = Color.White,
                disabledContentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            if (sharing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ShareMemoPreview(memo: V2Memo, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(19.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(92.dp, 68.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))),
            contentAlignment = Alignment.Center,
        ) {
            if (!memo.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = memo.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Videocam, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                Text(
                    memo.durationSec?.let { seconds -> "%d:%02d".format((seconds / 60).toInt(), seconds.toInt() % 60) } ?: "视频记录",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ShareSpaceRow(
    connection: V2CoachConnection,
    selected: Boolean,
    succeeded: Boolean,
    failed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val person = connection.coach ?: connection.peer
    val ringColor = if (connection.relationshipType == "friend") DSColors.fairway.copy(alpha = 0.45f)
    else Color(0xFFC5A64A)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(
                1.dp,
                if (selected) DSColors.fairway.copy(alpha = 0.45f) else DSColors.borderLine,
                RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, ringColor, CircleShape).padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            V2UserAvatar(
                avatar = person?.avatar,
                modifier = Modifier.fillMaxSize(),
                fallbackFontSize = 16.sp,
                placeholderIconSize = 25.dp,
                fallbackText = person?.nickname ?: connection.otherPersonName(),
                fallbackColor = DSColors.sandBeige,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(connection.otherPersonName(), color = DSColors.deepGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (connection.relationshipType == "friend") "好友空间" else "教学空间",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        when {
            succeeded -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.CheckCircle, "已分享", tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                Text("已分享", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            failed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.ErrorOutline, "待重试", tint = Color(0xFFD95C4F), modifier = Modifier.size(16.dp))
                Text("待重试", color = Color(0xFFD95C4F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            else -> Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().border(1.5.dp, DSColors.shadowGray.copy(alpha = 0.55f), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ShareResultBanner(message: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(
                if (isError) Color(0xFFD95C4F).copy(alpha = 0.07f)
                else DSColors.mistGreen.copy(alpha = 0.72f),
            ).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (isError) Color(0xFFD95C4F) else DSColors.deepGreen,
            modifier = Modifier.size(15.dp),
        )
        Text(
            message,
            color = if (isError) Color(0xFFD95C4F) else DSColors.deepGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal data class PreparedShareVideo(val file: File, val temporary: Boolean)

internal suspend fun prepareShareVideo(context: Context, memo: V2Memo): PreparedShareVideo = withContext(Dispatchers.IO) {
    V2MemoBridge.localVideoAbsolutePath(memo.videoUrl)?.let(::File)
        ?.takeIf { it.isFile && it.length() > 0L }
        ?.let { return@withContext PreparedShareVideo(it, false) }
    val url = memo.videoUrl?.takeIf(V2MemoBridge::isRemoteUrl)
        ?: throw V2CoachUserFacingException("这条记录没有可分享的视频")
    val extension = url.substringBefore('?').substringAfterLast('.', "mp4").lowercase()
        .takeIf { it in setOf("mp4", "mov", "m4v") } ?: "mp4"
    val target = File.createTempFile("space-share-", ".$extension", context.cacheDir)
    val request = Request.Builder().url(url).get().build()
    val client = ServiceLocator.net.httpClient.newBuilder()
        .readTimeout(30, TimeUnit.MINUTES).callTimeout(30, TimeUnit.MINUTES).build()
    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw V2CoachUserFacingException("在线视频暂时无法准备，请稍后重试")
            val body = response.body ?: throw V2CoachUserFacingException("在线视频暂时无法准备，请稍后重试")
            target.outputStream().buffered().use { output -> body.byteStream().use { it.copyTo(output) } }
        }
        if (target.length() <= 0L) throw V2CoachUserFacingException("在线视频暂时无法准备，请稍后重试")
        PreparedShareVideo(target, true)
    } catch (throwable: Throwable) {
        target.delete()
        throw throwable
    }
}

internal suspend fun createShareCover(context: Context, video: File): File = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(video.absolutePath)
        val bitmap = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: throw V2CoachUserFacingException("无法生成视频封面")
        val target = File.createTempFile("space-cover-", ".jpg", context.cacheDir)
        target.outputStream().buffered().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) {
                throw V2CoachUserFacingException("无法生成视频封面")
            }
        }
        bitmap.recycle()
        target
    } finally {
        retriever.release()
    }
}

private fun V2CoachConnection.otherPersonName(): String =
    (coach ?: peer)?.nickname ?: if (relationshipType == "friend") "挥呗好友" else "挥呗教练"

internal fun memoHasShareableVideoSource(memo: V2Memo): Boolean {
    val source = memo.videoUrl?.trim()?.takeIf(String::isNotEmpty) ?: return false
    if (V2MemoBridge.isRemoteUrl(source)) return true
    val localPath = V2MemoBridge.localVideoAbsolutePath(source) ?: return false
    return File(localPath).let { it.isFile && it.length() > 0L }
}
