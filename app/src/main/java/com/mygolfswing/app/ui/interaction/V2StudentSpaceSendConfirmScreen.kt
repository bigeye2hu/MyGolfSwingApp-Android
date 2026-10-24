package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.R
import java.util.Locale

internal const val STUDENT_SPACE_SEND_NOTE_LIMIT = 300

@Composable
internal fun V2StudentSpaceSendConfirmScreen(
    prepared: V2PreparedWorkspaceVideo,
    isCapture: Boolean,
    friendSpace: Boolean,
    note: String,
    wantsReview: Boolean,
    sending: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onNoteChange: (String) -> Unit,
    onReviewChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    showCellularConfirmation: Boolean = false,
    onConfirmCellular: () -> Unit = {},
    onDismissCellular: () -> Unit = {},
) {
    V2BackHandler(enabled = !sending, onBack = onBack)

    Box(
        Modifier.fillMaxSize().background(DSColors.warmWhite).v2FullScreenInsets()
            .testTag("student-space-send-confirm"),
    ) {
        V2PageArtworkBackground(
            artwork = V2PageArtwork.GOLFER,
            tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
            modifier = Modifier.fillMaxSize(),
        ) {}
        Image(
            painter = painterResource(R.drawable.room_background),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        )

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clickable(enabled = !sending, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = DSColors.deepGreen,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (isCapture) "发送练习视频" else "确认发送",
                    color = DSColors.deepGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(36.dp))
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(18.dp))
                        .background(DSColors.deepGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = prepared.cover,
                        contentDescription = "待发送视频预览",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                            .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(
                            formatStudentSpaceSendDuration(prepared.durationSeconds),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (friendSpace) "内容说明" else "练习说明或问题",
                            color = DSColors.deepGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("必填", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    BasicTextField(
                        value = note,
                        onValueChange = { onNoteChange(it.take(STUDENT_SPACE_SEND_NOTE_LIMIT)) },
                        enabled = !sending,
                        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
                        cursorBrush = SolidColor(DSColors.fairway),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DSColors.cardWhite)
                            .border(
                                1.dp,
                                DSColors.borderLine.copy(alpha = DSColors.borderLine.alpha * 0.8f),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(12.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.TopStart) {
                                if (note.isEmpty()) {
                                    Text(
                                        if (friendSpace) "想让对方先看哪里" else "这次主要练什么？或者有什么想请教教练？",
                                        color = DSColors.shadowGray.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 2.dp),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Text(
                        "写一句话就行，大约 10～40 字，也可以写得更具体。",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${note.length}/$STUDENT_SPACE_SEND_NOTE_LIMIT",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!friendSpace) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(DSColors.cardWhite).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("需要教练点评", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "关闭后只记在这个空间，不进入教练的点评队列",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Switch(
                            checked = wantsReview,
                            onCheckedChange = onReviewChange,
                            enabled = !sending,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DSColors.fairway,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = DSColors.borderLine,
                            ),
                        )
                    }
                }

                Text(
                    if (friendSpace) "只在这段好友空间里可见，不会出现在其他空间。"
                    else "只对你和这位教练可见，不会出现在其他空间。",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                error?.let {
                    Text(it, color = DSColors.flagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(DSColors.warmWhite.copy(alpha = 0.94f))
                    .padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isCapture) {
                    OutlinedButton(
                        onClick = onRetake,
                        enabled = !sending,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DSColors.deepGreen),
                        border = BorderStroke(1.dp, DSColors.borderLine),
                    ) {
                        Text("重拍", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onSend,
                    enabled = !sending && note.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DSColors.deepGreen,
                        disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.45f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp).testTag("student-space-send-progress"),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        if (isCapture) "发送" else "发送到这个空间",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showCellularConfirmation) {
        V2StudentSendCellularConfirmationSheet(
            busy = sending,
            onConfirm = onConfirmCellular,
            onDismiss = onDismissCellular,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2StudentSendCellularConfirmationSheet(
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        dragHandle = null,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("student-send-cellular-sheet"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                .padding(horizontal = 12.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "使用蜂窝网络发送？",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "这条视频较大，继续发送会消耗手机流量。暂不发送会保留这段视频。",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable(enabled = !busy, onClick = onConfirm)
                        .testTag("student-send-cellular-confirm"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (busy) "发送中…" else "继续发送",
                        color = DSColors.fairway,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable(enabled = !busy, onClick = onDismiss)
                    .testTag("student-send-cellular-cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "暂不发送",
                    color = DSColors.fairway,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal fun formatStudentSpaceSendDuration(seconds: Double?): String {
    val value = seconds?.takeIf { it.isFinite() && it > 0.0 }?.toInt() ?: 0
    return String.format(Locale.CHINA, "%d:%02d", value / 60, value % 60)
}
