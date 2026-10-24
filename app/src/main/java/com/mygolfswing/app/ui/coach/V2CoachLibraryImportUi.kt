package com.mygolfswing.app.ui.coach

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.coach.V2CoachDeliveryJob
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun defaultCoachLibraryImportTitle(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = "教学视频 · " + DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    .withZone(zoneId)
    .format(now)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2CoachLibraryAddSourceSheet(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DSColors.warmWhite,
        contentColor = DSColors.inkGreen,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DSColors.shadowGray.copy(alpha = 0.45f)) },
    ) {
        V2CoachLibraryAddSourceContent(
            enabled = enabled,
            onClose = onDismiss,
            onCamera = onCamera,
            onAlbum = onAlbum,
        )
    }
}

@Composable
internal fun V2CoachLibraryAddSourceContent(
    enabled: Boolean,
    onClose: () -> Unit,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().background(DSColors.warmWhite)
            .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Eco, null, tint = DSColors.deepGreen, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "记录你的教学灵感",
                    color = DSColors.inkGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("coach-library-source-title"),
                )
                Text("用视频和图片，沉淀更好的教学内容", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, CircleShape).clickable(onClick = onClose)
                    .testTag("coach-library-source-close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "关闭", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
            }
        }
        CoachLibraryAddSourceRow(
            title = "拍摄视频或照片",
            detail = "使用相机拍摄新的教学素材",
            icon = Icons.Filled.CameraAlt,
            enabled = enabled,
            tag = "coach-library-source-camera",
            onClick = onCamera,
        )
        CoachLibraryAddSourceRow(
            title = "从相册添加",
            detail = "选择手机相册中的视频或照片",
            icon = Icons.Filled.Photo,
            enabled = enabled,
            tag = "coach-library-source-album",
            onClick = onAlbum,
        )
        Text(
            "支持视频与图片，可后续编辑封面与分类",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun CoachLibraryAddSourceRow(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.deepGreen, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun V2CoachLibraryImportCard(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    progress: Double,
    actionBusy: Boolean,
    onContinue: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val normalizedProgress = progress.coerceIn(0.0, 1.0).toFloat()
    val size = (job.videoPath?.let(::File)?.length() ?: 0L).let { Formatter.formatFileSize(context, it) }
    val activeStatus = when (phase) {
        V2CoachDeliveryPhase.PREPARING -> "正在处理素材"
        V2CoachDeliveryPhase.UPLOADING -> "正在上传素材"
        V2CoachDeliveryPhase.PUBLISHING -> "正在保存素材"
        null -> "正在准备素材"
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(DSColors.cardWhite)
            .border(
                1.dp,
                if (active) DSColors.fairway.copy(alpha = 0.3f) else V2CoachPalette.gold.copy(alpha = 0.38f),
                RoundedCornerShape(19.dp),
            ).padding(13.dp).testTag("coach-library-import-card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(width = 78.dp, height = 64.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.deepGreen),
            contentAlignment = Alignment.Center,
        ) {
            job.coverPath?.let(::File)?.takeIf(File::isFile)?.let { cover ->
                AsyncImage(cover, null, Modifier.fillMaxWidth().height(64.dp), contentScale = ContentScale.Crop)
            }
            if (active) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (active) "正在加入私有素材" else "上传暂停，视频已经保留",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) Text("${(normalizedProgress * 100).toInt()}%", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (active) "$activeStatus · $size" else job.lastError ?: "从这里继续，不需要重新选择视频",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (active) {
                LinearProgressIndicator(
                    progress = { normalizedProgress.coerceAtLeast(0.02f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = DSColors.fairway,
                    trackColor = DSColors.mistGreen,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (actionBusy) "正在确认…" else "继续",
                        color = DSColors.fairway,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !actionBusy, onClick = onContinue)
                            .padding(vertical = 4.dp).testTag("coach-library-import-continue"),
                    )
                    Text(
                        "放弃",
                        color = DSColors.flagRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !actionBusy, onClick = onDiscard)
                            .padding(vertical = 4.dp).testTag("coach-library-import-discard"),
                    )
                }
            }
        }
        if (active) {
            Text(
                "停止",
                color = DSColors.flagRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !actionBusy, onClick = onStop)
                    .padding(5.dp).testTag("coach-library-import-stop"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2CoachLibraryDiscardImportSheet(
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    V2CoachLibraryDestructiveConfirmationSheet(
        title = "放弃这段未完成素材？",
        message = "放弃后会删除本机保留的视频；已经成功入库的云端素材不受影响。",
        confirmLabel = "删除本机草稿",
        busyLabel = "正在删除…",
        cancelLabel = "继续保留",
        confirmTag = "coach-library-discard-confirm",
        cancelTag = "coach-library-discard-keep",
        busy = busy,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun V2CoachLibraryRemoveConfirmationSheet(
    busy: Boolean,
    confirmTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    V2CoachLibraryDestructiveConfirmationSheet(
        title = "移出私有素材库？",
        message = "已发送给学员的历史内容不会受影响。",
        confirmLabel = "移出素材库",
        busyLabel = "正在移出…",
        cancelLabel = "取消",
        confirmTag = confirmTag,
        cancelTag = "coach-library-remove-cancel",
        busy = busy,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2CoachLibraryDestructiveConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    busyLabel: String,
    cancelLabel: String,
    confirmTag: String,
    cancelTag: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DSColors.shadowGray.copy(alpha = 0.45f)) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)
                .v2NavigationBarPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DSColors.cardWhite),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    color = DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 16.dp),
                )
                Text(
                    message,
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 5.dp, bottom = 14.dp),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                Text(
                    if (busy) busyLabel else confirmLabel,
                    color = DSColors.flagRed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onConfirm)
                        .padding(vertical = 16.dp).testTag(confirmTag),
                )
            }
            Text(
                cancelLabel,
                color = DSColors.fairway,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DSColors.cardWhite)
                    .clickable(enabled = !busy, onClick = onDismiss).padding(vertical = 16.dp)
                    .testTag(cancelTag),
            )
        }
    }
}

@Composable
internal fun V2CoachLibraryImportSuccessCard(
    studentCount: Int,
    singleStudentName: String?,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onNeedStudent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(12.dp)
            .testTag("coach-library-import-success"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = DSColors.deepGreen, modifier = Modifier.size(17.dp))
        Text(
            "素材已保存，并已放到列表顶部",
            color = DSColors.deepGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            when {
                studentCount == 0 -> "添加学员"
                studentCount == 1 && !singleStudentName.isNullOrBlank() -> "发给 $singleStudentName"
                else -> "发给学员"
            },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.height(30.dp).clip(CircleShape).background(DSColors.fairway)
                .clickable(onClick = if (studentCount == 0) onNeedStudent else onShare)
                .padding(horizontal = 10.dp, vertical = 7.dp).testTag("coach-library-import-success-primary"),
        )
        Text(
            "编辑",
            color = DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onEdit).padding(vertical = 7.dp)
                .testTag("coach-library-import-success-edit"),
        )
    }
}
