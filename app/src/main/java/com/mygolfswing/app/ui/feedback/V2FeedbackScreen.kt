package com.mygolfswing.app.ui.feedback

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class FeedbackType(
    val label: String,
    val icon: ImageVector,
    val color: Color,
) {
    SUGGESTION("功能建议", Icons.Filled.Lightbulb, DSColors.fairway),
    BUG("问题反馈", Icons.Filled.BugReport, DSColors.flagRed),
    OTHER("其他", Icons.Filled.MoreHoriz, DSColors.deepGreen),
}

/** 意见反馈（对齐 iOS `FeedbackView`） */
@Composable
fun V2FeedbackScreen(onBack: () -> Unit) {
    var type by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val canSubmit by remember {
        derivedStateOf {
            content.trim().isNotEmpty() && content.length <= 500
        }
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        FeedbackTopBar(onCancel = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .v2NavigationBarPadding()
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("反馈类型", style = DSTypography.bodyBold, color = DSColors.shadowGray)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeedbackType.entries.forEach { t ->
                    val selected = t == type
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) t.color else DSColors.mistGreen)
                            .clickable { type = t }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = t.icon,
                            contentDescription = null,
                            tint = if (selected) DSColors.warmWhite else DSColors.shadowGray,
                            modifier = Modifier.size(14.dp),
                        )
                        Box(modifier = Modifier.size(6.dp))
                        Text(
                            t.label,
                            color = if (selected) DSColors.warmWhite else DSColors.shadowGray,
                            style = DSTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("反馈内容", style = DSTypography.bodyBold, color = DSColors.shadowGray)
                Text("*", style = DSTypography.bodyBold, color = DSColors.flagRed)
            }
            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    error = null
                },
                placeholder = { Text("请详细描述您的问题或建议，以便我们更好地为您服务…") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                "${content.length}/500",
                style = DSTypography.caption,
                color = if (content.length > 500) DSColors.flagRed else DSColors.shadowGray,
                modifier = Modifier.align(Alignment.End),
            )
            if (content.length > 500) {
                Text("反馈内容最多 500 字，请精简后再提交。", color = DSColors.flagRed, style = DSTypography.caption)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("联系方式", style = DSTypography.bodyBold, color = DSColors.shadowGray)
                Text("（选填）", style = DSTypography.caption, color = DSColors.shadowGray.copy(alpha = 0.7f))
            }
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                placeholder = { Text("邮箱或手机号，方便我们回复您") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            if (!error.isNullOrBlank()) {
                Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
            }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        submitError = null
                        val r = try {
                            ServiceLocator.feedbackRepo.submit(type.label, content, contact)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            Result.failure(e)
                        } finally {
                            loading = false
                        }
                        r.onSuccess { showSuccess = true }
                            .onFailure {
                                submitError = feedbackSubmitThrowableError("提交失败，反馈通道暂不可用，请稍后重试", it)
                            }
                    }
                },
                enabled = canSubmit && !loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = DSColors.shadowGray.copy(alpha = 0.4f),
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (canSubmit && !loading) 10.dp else 0.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = type.color.copy(alpha = 0.30f),
                        spotColor = type.color.copy(alpha = 0.30f),
                    )
                    .background(
                        if (canSubmit) {
                            Brush.horizontalGradient(listOf(type.color, type.color.copy(alpha = 0.85f)))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    DSColors.shadowGray.copy(alpha = 0.4f),
                                    DSColors.shadowGray.copy(alpha = 0.4f),
                                ),
                            )
                        },
                        RoundedCornerShape(14.dp),
                    ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = DSColors.warmWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DSColors.warmWhite, modifier = Modifier.size(16.dp))
                    Box(modifier = Modifier.size(8.dp))
                    Text("提交反馈", style = DSTypography.bodyBold, color = DSColors.warmWhite)
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onBack() },
            title = { Text("提交成功") },
            text = { Text("感谢您的反馈！我们会认真阅读并持续改进。") },
            confirmButton = {
                TextButton(onClick = { showSuccess = false; onBack() }) {
                    Text("好的", color = DSColors.fairway)
                }
            },
        )
    }

    submitError?.let { message ->
        AlertDialog(
            onDismissRequest = { submitError = null },
            title = { Text("提交失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { submitError = null }) {
                    Text("好的", color = DSColors.fairway)
                }
            },
        )
    }
}

@Composable
private fun FeedbackTopBar(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(56.dp)
            .background(DSColors.warmWhite)
            .padding(horizontal = DSSpacing.lg),
    ) {
        Text(
            text = "取消",
            style = DSTypography.bodyBold,
            color = DSColors.fairway,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onCancel)
                .padding(horizontal = 2.dp, vertical = 8.dp),
        )
        Text(
            text = "意见反馈",
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

internal fun feedbackSubmitError(base: String, debugDetail: String?): String {
    return feedbackSubmitError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun feedbackSubmitError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun feedbackSubmitThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return feedbackSubmitError(base, error.message)
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DSColors.mistGreen,
    unfocusedContainerColor = DSColors.mistGreen,
    disabledContainerColor = DSColors.mistGreen,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    cursorColor = DSColors.fairway,
    focusedTextColor = DSColors.inkGreen,
    unfocusedTextColor = DSColors.inkGreen,
    focusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
)
