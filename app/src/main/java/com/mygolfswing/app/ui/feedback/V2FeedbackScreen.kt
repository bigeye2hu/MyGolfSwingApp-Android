package com.mygolfswing.app.ui.feedback

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2RoomPage
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
    OTHER("其他", Icons.Filled.Sms, DSColors.deepGreen),
}

data class V2FeedbackDraft(
    val typeLabel: String = "功能建议",
    val content: String = "",
    val contact: String = "",
)

/** 意见反馈（对齐 iOS `FeedbackView`） */
@Composable
fun V2FeedbackScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit = {},
    initialDraft: V2FeedbackDraft = V2FeedbackDraft(),
    onDraftChange: (V2FeedbackDraft) -> Unit = {},
    isLoggedIn: suspend () -> Boolean = { ServiceLocator.session.current().isLoggedIn },
    submitFeedback: suspend (String, String, String?) -> Result<Unit> = ServiceLocator.feedbackRepo::submit,
) {
    var typeLabel by rememberSaveable { mutableStateOf(initialDraft.typeLabel) }
    var content by rememberSaveable { mutableStateOf(initialDraft.content) }
    var contact by rememberSaveable { mutableStateOf(initialDraft.contact) }
    val type = FeedbackType.entries.firstOrNull { it.label == typeLabel } ?: FeedbackType.SUGGESTION
    var loading by remember { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var showSuccess by rememberSaveable { mutableStateOf(false) }
    val canSubmit by remember {
        derivedStateOf {
            content.trim().isNotEmpty() && content.length <= 500
        }
    }
    val scope = rememberCoroutineScope()

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("反馈类型")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeedbackType.entries.forEach { t ->
                            val selected = t == type
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) t.color else DSColors.mistGreen)
                                    .clickable {
                                        typeLabel = t.label
                                        onDraftChange(V2FeedbackDraft(t.label, content, contact))
                                    }
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("反馈内容")
                        Text("*", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DSColors.flagRed)
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            onDraftChange(V2FeedbackDraft(typeLabel, it, contact))
                        },
                        placeholder = { Text("请详细描述您的问题或建议，以便我们更好地为您服务…") },
                        modifier = Modifier.fillMaxWidth().height(150.dp).testTag("feedback-content"),
                        textStyle = TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Text(
                        "${content.length}/500",
                        fontSize = 11.sp,
                        color = if (content.length > 500) DSColors.flagRed else DSColors.shadowGray,
                        modifier = Modifier.align(Alignment.End),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("联系方式")
                        Text("（选填）", fontSize = 12.sp, color = DSColors.shadowGray.copy(alpha = 0.7f))
                    }
                    OutlinedTextField(
                        value = contact,
                        onValueChange = {
                            contact = it
                            onDraftChange(V2FeedbackDraft(typeLabel, content, it))
                        },
                        placeholder = { Text("邮箱或手机号，方便我们回复您") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("feedback-contact"),
                        textStyle = TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            try {
                                if (!isLoggedIn()) {
                                    onRequestLogin()
                                    return@launch
                                }
                                submitError = null
                                val r = try {
                                    submitFeedback(type.label, content, contact.takeIf { it.isNotBlank() })
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Result.failure(e)
                                }
                                r.onSuccess { showSuccess = true }
                                    .onFailure {
                                        submitError = feedbackSubmitThrowableError("提交失败，反馈通道暂不可用，请稍后重试", it)
                                    }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                submitError = feedbackSubmitThrowableError("提交失败，反馈通道暂不可用，请稍后重试", e)
                            } finally {
                                loading = false
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
                        .height(40.dp)
                        .testTag("feedback-submit")
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
                        Text("提交反馈", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DSColors.warmWhite)
                    }
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
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DSColors.shadowGray)
}

@Composable
private fun FeedbackTopBar(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .background(DSColors.warmWhite)
            .padding(horizontal = DSSpacing.lg)
            .testTag("feedback-top-bar"),
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
