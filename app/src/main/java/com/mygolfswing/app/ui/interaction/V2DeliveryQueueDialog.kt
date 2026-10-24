package com.mygolfswing.app.ui.interaction

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CellularUploadConfirmationRequired
import com.mygolfswing.app.data.coach.V2CoachDeliveryJob
import com.mygolfswing.app.data.coach.V2CoachDeliveryListFilter
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.data.coach.V2_DELIVERY_LIBRARY_IMPORT
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.coach.filterForQueue
import com.mygolfswing.app.data.coach.isFailedQueueJob
import com.mygolfswing.app.data.coach.jobsForOwner
import com.mygolfswing.app.data.coach.requiresCellularUploadConfirmation
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun V2DeliveryQueueDialog(
    ownerUserId: String,
    onDismiss: () -> Unit,
    onOpenConnection: (connectionId: String, parentItemId: String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val jobs = state.jobsForOwner(ownerUserId)
    val activeJobId = state.activeJobId?.takeIf { id -> jobs.any { it.jobId == id } }
    val failedCount = jobs.count(V2CoachDeliveryJob::isFailedQueueJob)
    val activeCount = jobs.size - failedCount
    val networkConnected = rememberQueueNetworkConnected()
    var selectedFilterName by rememberSaveable { mutableStateOf(V2CoachDeliveryListFilter.ACTIVE.name) }
    val selectedFilter = V2CoachDeliveryListFilter.entries.firstOrNull { it.name == selectedFilterName }
        ?: V2CoachDeliveryListFilter.ACTIVE
    val visibleJobs = jobs.filterForQueue(selectedFilter)
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var stoppingJobId by remember { mutableStateOf<String?>(null) }
    var cellularCandidate by remember { mutableStateOf<V2CellularUploadConfirmationRequired?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun retry(job: V2CoachDeliveryJob) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).onFailure { throwable ->
                    if (throwable is V2CellularUploadConfirmationRequired) {
                        cellularCandidate = throwable
                    } else {
                        feedback = coachThrowableError("重新发送失败，请稍后重试", throwable)
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize().testTag("delivery-queue-screen")) {
            Column(Modifier.fillMaxSize()) {
                DeliveryQueueNavigationBar(
                    failedCount = failedCount,
                    busy = busy,
                    onClose = onDismiss,
                    onRetryAll = {
                        busy = true
                        feedback = null
                        scope.launch {
                            val delivered = ServiceLocator.coachDeliveryQueue.retryAll(
                                ownerUserId,
                                ServiceLocator.coachWorkspaceRepo,
                            )
                            feedback = if (delivered > 0) {
                                "已送达 $delivered 条内容"
                            } else {
                                "暂时仍无法发送，请检查网络后重试"
                            }
                            busy = false
                        }
                    },
                )
                if (jobs.isEmpty()) {
                    DeliveryQueueEmptyState(Modifier.weight(1f).v2NavigationBarPadding())
                } else {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            if (!refreshing && !busy) {
                                refreshing = true
                                feedback = null
                                scope.launch {
                                    ServiceLocator.coachDeliveryQueue.recover(
                                        ownerUserId,
                                        ServiceLocator.coachWorkspaceRepo,
                                    )
                                    refreshing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("delivery-queue-pull-refresh"),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                DeliveryQueueFilterBar(
                                    activeCount = activeCount,
                                    failedCount = failedCount,
                                    selected = selectedFilter,
                                    onSelected = { selectedFilterName = it.name },
                                )
                            }
                            item {
                                DeliveryQueueSummaryCard(
                                    totalCount = jobs.size,
                                    failedCount = failedCount,
                                    networkConnected = networkConnected,
                                )
                            }
                            if (visibleJobs.isEmpty()) {
                                item {
                                    Text(
                                        if (selectedFilter == V2CoachDeliveryListFilter.ACTIVE) {
                                            "当前没有进行中的发送"
                                        } else {
                                            "当前没有失败记录"
                                        },
                                        color = DSColors.shadowGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                    )
                                }
                            } else {
                                items(visibleJobs, key = V2CoachDeliveryJob::jobId) { job ->
                                    DeliveryJobCard(
                                        job = job,
                                        active = activeJobId == job.jobId,
                                        activePhase = state.activePhase.takeIf { activeJobId == job.jobId },
                                        activeProgress = state.activeProgress,
                                        busy = busy,
                                        stopping = stoppingJobId == job.jobId,
                                        onRetry = { retry(job) },
                                        onOpen = if (job.deliveryType == V2_DELIVERY_LIBRARY_IMPORT) null else ({
                                            onOpenConnection(job.connectionId, job.parentItemId)
                                            onDismiss()
                                        }),
                                        onStopAndRemove = {
                                            stoppingJobId = job.jobId
                                            feedback = null
                                            scope.launch {
                                                ServiceLocator.coachDeliveryQueue.stop(job.jobId)
                                                val removed = ServiceLocator.coachDeliveryQueue.cancel(job.jobId)
                                                feedback = if (removed) {
                                                    "已停止发送并移出队列"
                                                } else {
                                                    "这条内容已经完成或不再发送"
                                                }
                                                stoppingJobId = null
                                            }
                                        },
                                        onRemove = {
                                            if (!busy) {
                                                busy = true
                                                feedback = null
                                                scope.launch {
                                                    val removed = ServiceLocator.coachDeliveryQueue.cancel(job.jobId)
                                                    feedback = if (removed) "已从发送队列移除" else "当前任务正在发送，请稍后再试"
                                                    busy = false
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                            feedback?.let { message ->
                                item {
                                    Text(
                                        message,
                                        color = if (message.contains("失败") || message.contains("无法")) DSColors.flagRed else DSColors.fairway,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                    )
                                }
                            }
                            item { DeliveryQueueRetentionNote() }
                            item { Spacer(Modifier.v2NavigationBarPadding()) }
                        }
                    }
                }
            }
        }
    }

    cellularCandidate?.let { candidate ->
        V2CellularUploadConfirmationDialog(
            fileSizeBytes = candidate.fileSizeBytes,
            busy = busy,
            onConfirm = {
                busy = true
                feedback = null
                scope.launch {
                    val approved = ServiceLocator.coachDeliveryQueue.approveCellularUpload(candidate.jobId)
                    if (approved) {
                        ServiceLocator.coachDeliveryQueue.deliver(
                            candidate.jobId,
                            ServiceLocator.coachWorkspaceRepo,
                        ).onSuccess {
                            feedback = "内容已送达"
                            cellularCandidate = null
                        }.onFailure { throwable ->
                            feedback = coachThrowableError("重新发送失败，请稍后重试", throwable)
                            cellularCandidate = null
                        }
                    } else {
                        feedback = "这条内容已经完成或失效"
                        cellularCandidate = null
                    }
                    busy = false
                }
            },
            onDismiss = {
                cellularCandidate = null
                feedback = "内容已保留，连接 Wi-Fi 后会自动发送"
            },
        )
    }
}

@Composable
private fun DeliveryQueueNavigationBar(
    failedCount: Int,
    busy: Boolean,
    onClose: () -> Unit,
    onRetryAll: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f))
            .v2StatusBarPadding().height(44.dp).testTag("delivery-queue-top-bar"),
    ) {
        Text(
            "关闭",
            color = DSColors.fairway,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.CenterStart).clickable(enabled = !busy, onClick = onClose)
                .padding(horizontal = 18.dp, vertical = 16.dp).testTag("delivery-queue-close"),
        )
        Text(
            "发送队列",
            color = DSColors.inkGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        if (failedCount > 1) {
            Text(
                if (busy) "处理中…" else "全部重试",
                color = if (busy) DSColors.shadowGray.copy(alpha = 0.45f) else DSColors.fairway,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).clickable(enabled = !busy, onClick = onRetryAll)
                    .padding(horizontal = 18.dp, vertical = 16.dp).testTag("delivery-queue-retry-all"),
            )
        }
    }
}

@Composable
private fun DeliveryQueueEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(DSColors.fairway.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            "没有待发送内容",
            color = DSColors.inkGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "从学员交流中发送回复后，可以继续处理下一条",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun DeliveryQueueFilterBar(
    activeCount: Int,
    failedCount: Int,
    selected: V2CoachDeliveryListFilter,
    onSelected: (V2CoachDeliveryListFilter) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            V2CoachDeliveryListFilter.ACTIVE to "进行中 $activeCount",
            V2CoachDeliveryListFilter.FAILED to "失败 $failedCount",
        ).forEach { (filter, title) ->
            val isSelected = selected == filter
            Box(
                Modifier.weight(1f).height(34.dp).clip(CircleShape)
                    .background(if (isSelected) DSColors.deepGreen else DSColors.cardWhite)
                    .clickable { onSelected(filter) }.testTag("delivery-queue-filter-${filter.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    color = if (isSelected) Color.White else DSColors.inkGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DeliveryQueueSummaryCard(totalCount: Int, failedCount: Int, networkConnected: Boolean) {
    val color = if (failedCount > 0) DSColors.trophyGold else DSColors.fairway
    val title = when {
        failedCount > 0 -> "有${failedCount}条需要处理"
        !networkConnected -> "等待网络恢复"
        else -> "正在依次发送 $totalCount 条回复"
    }
    val detail = when {
        failedCount > 0 -> "已成功上传的部分不会重复上传，可以直接重试"
        !networkConnected -> "文字和视频保留在本机，联网后会自动继续"
        else -> "可以关闭此页并继续点评其他学员"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
            Icon(
                if (failedCount > 0) Icons.Filled.Error else Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DeliveryJobCard(
    job: V2CoachDeliveryJob,
    active: Boolean,
    activePhase: V2CoachDeliveryPhase?,
    activeProgress: Double,
    busy: Boolean,
    stopping: Boolean,
    onRetry: () -> Unit,
    onOpen: (() -> Unit)?,
    onStopAndRemove: () -> Unit,
    onRemove: () -> Unit,
) {
    val failed = job.isFailedQueueJob()
    val color = if (failed) DSColors.trophyGold else DSColors.fairway
    val videoCount = job.deliveryVideoCount()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
            .border(
                width = if (active) 1.3.dp else 1.dp,
                color = if (active) DSColors.fairway.copy(alpha = 0.42f) else DSColors.borderLine,
                shape = RoundedCornerShape(20.dp),
            ).padding(15.dp).testTag("delivery-job-${job.jobId}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(
                    deliveryStateIcon(job, active, activePhase),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (job.deliveryType == V2_DELIVERY_LIBRARY_IMPORT) "保存到私有素材" else "发给 ${job.peerName.ifBlank { "学员" }}",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    job.text.trim().ifBlank { job.title.ifBlank { deliveryFallbackPreview(job) } },
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                deliveryStateText(job, active, activePhase, activeProgress),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(27.dp).clip(CircleShape).background(color.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
        if (active) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                LinearProgressIndicator(
                    progress = { activeProgress.toFloat().coerceIn(0.02f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = DSColors.fairway,
                )
                Text(
                    deliveryProgressDetail(activePhase, activeProgress),
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(
                if (videoCount > 0) Icons.Filled.VideoLibrary else Icons.Filled.Textsms,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(13.dp),
            )
            Text(
                if (videoCount > 0) "${videoCount}段视频" else "文字回复",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Text("·", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(
                deliveryRelativeTime(job.createdAtMillis),
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            if (job.attemptCount > 1) {
                Text(
                    "· 已尝试${job.attemptCount}次",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        job.lastError?.takeIf(String::isNotBlank)?.let { error ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Error, null, tint = DSColors.trophyGold, modifier = Modifier.size(13.dp))
                Text(
                    error,
                    color = DSColors.trophyGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (failed) {
                DeliveryQueueAction(
                    title = "重新发送",
                    foreground = Color.White,
                    background = DSColors.fairway,
                    enabled = !busy && !active,
                    modifier = Modifier.weight(1f),
                    onClick = onRetry,
                )
            }
            onOpen?.let { open ->
                DeliveryQueueAction(
                    title = if (job.parentItemId == null) "查看空间" else "查看交流",
                    foreground = DSColors.fairway,
                    background = DSColors.mistGreen.copy(alpha = 0.7f),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = open,
                )
            }
            DeliveryQueueAction(
                title = if (active) if (stopping) "停止中…" else "停止并移除" else "移出队列",
                foreground = DSColors.flagRed,
                background = DSColors.flagRed.copy(alpha = 0.08f),
                enabled = !busy && !stopping,
                modifier = Modifier.weight(1f),
                onClick = if (active) onStopAndRemove else onRemove,
            )
        }
    }
}

@Composable
private fun DeliveryQueueAction(
    title: String,
    foreground: Color,
    background: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.height(38.dp).clip(RoundedCornerShape(12.dp)).background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            color = if (enabled) foreground else foreground.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DeliveryQueueRetentionNote() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Lock, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
        Text(
            "未完成任务和本地视频最多保留24小时；成功发送后会自动清理",
            color = DSColors.shadowGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun deliveryStateIcon(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
): ImageVector = when {
    job.isFailedQueueJob() -> Icons.Filled.Error
    active && phase == V2CoachDeliveryPhase.PREPARING -> Icons.Filled.AutoAwesome
    active && phase == V2CoachDeliveryPhase.UPLOADING -> Icons.Filled.CloudUpload
    active && phase == V2CoachDeliveryPhase.PUBLISHING -> Icons.AutoMirrored.Filled.Send
    job.requiresCellularUploadConfirmation(activeNetworkMetered = true) -> Icons.Filled.WifiOff
    else -> Icons.Filled.Schedule
}

private fun deliveryStateText(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    progress: Double,
): String = when {
    active && phase == V2CoachDeliveryPhase.UPLOADING -> "上传 ${(progress * 100).toInt()}%"
    active -> phase?.displayText ?: "发送中"
    job.requiresCellularUploadConfirmation(activeNetworkMetered = true) -> "等待 Wi-Fi"
    job.pausedByUser -> "已停止"
    !job.lastError.isNullOrBlank() -> "发送暂停"
    else -> "排队中"
}

private fun deliveryProgressDetail(phase: V2CoachDeliveryPhase?, progress: Double): String = when (phase) {
    V2CoachDeliveryPhase.PREPARING -> "正在处理视频 ${(progress * 100).toInt()}%"
    V2CoachDeliveryPhase.UPLOADING -> "正在后台上传 ${(progress * 100).toInt()}%"
    V2CoachDeliveryPhase.PUBLISHING -> "文件已准备好，正在发布到学员交流"
    null -> "正在准备发送"
}

private fun deliveryFallbackPreview(job: V2CoachDeliveryJob): String = when {
    job.deliveryType == V2_DELIVERY_LIBRARY_IMPORT -> "私有教学素材"
    job.sourceKind == "library_asset" -> "私有教学素材引用（无需重复上传）"
    job.sourceKind == "voice_recording" -> "语音消息"
    else -> "视频内容"
}

private fun V2CoachDeliveryJob.deliveryVideoCount(): Int {
    val primary = if (sourceKind !in setOf("text", "voice_recording")) 1 else 0
    val related = attachments.count { it.sourceKind != "voice_recording" }
    return primary + related
}

internal fun deliveryRelativeTime(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds((nowMillis - createdAtMillis).coerceAtLeast(0L))
    return when {
        seconds < 60 -> "刚刚加入"
        seconds < 3_600 -> "${seconds / 60}分钟前加入"
        else -> "${seconds / 3_600}小时前加入"
    }
}

@Composable
internal fun rememberQueueNetworkConnected(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    var connected by remember(manager) { mutableStateOf(manager?.queueNetworkAvailable() ?: true) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connected = manager?.queueNetworkAvailable() ?: true
            }

            override fun onLost(network: Network) {
                connected = manager?.queueNetworkAvailable() ?: false
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val registered = manager?.let { runCatching { it.registerDefaultNetworkCallback(callback) }.isSuccess } == true
        onDispose {
            if (registered) runCatching { manager?.unregisterNetworkCallback(callback) }
        }
    }
    return connected
}

private fun ConnectivityManager.queueNetworkAvailable(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
