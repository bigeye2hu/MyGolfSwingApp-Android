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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

private enum class FeedbackType(val label: String) {
    SUGGESTION("功能建议"),
    BUG("问题反馈"),
    OTHER("其他"),
}

/** 意见反馈（对齐 iOS `FeedbackView`） */
@Composable
fun V2FeedbackScreen(onBack: () -> Unit) {
    var type by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "意见反馈", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("反馈类型", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackType.entries.forEach { t ->
                    val selected = t == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) DSColors.fairway else DSColors.mistGreen)
                            .clickable { type = t }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t.label,
                            color = if (selected) DSColors.warmWhite else DSColors.shadowGray,
                            style = DSTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Text("反馈内容 *", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 500) content = it },
                placeholder = { Text("请详细描述您的问题或建议…") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = fieldColors(),
            )
            Text(
                "${content.length}/500",
                style = DSTypography.caption,
                color = if (content.length > 500) DSColors.flagRed else DSColors.shadowGray,
                modifier = Modifier.align(Alignment.End),
            )

            Text("联系方式（选填）", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                placeholder = { Text("手机号 / 邮箱，方便我们回复您") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )

            if (!error.isNullOrBlank()) {
                Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
            }

            Button(
                onClick = {
                    if (content.isBlank()) {
                        error = "请填写反馈内容"
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        val r = ServiceLocator.feedbackRepo.submit(type.label, content, contact)
                        loading = false
                        r.onSuccess { showSuccess = true }
                            .onFailure { error = it.message ?: "提交失败" }
                    }
                },
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(color = DSColors.warmWhite, strokeWidth = 2.dp)
                } else {
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
                androidx.compose.material3.TextButton(onClick = { showSuccess = false; onBack() }) {
                    Text("好的", color = DSColors.fairway)
                }
            },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    cursorColor = DSColors.fairway,
)
