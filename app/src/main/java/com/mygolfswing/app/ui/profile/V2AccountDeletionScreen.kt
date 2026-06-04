package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.repo.V2AccountDeletionRepository
import com.mygolfswing.app.data.repo.V2AccountDeletionStep
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0A0A0A)
private val CardBg = Color(0xFF1F2937).copy(alpha = 0.6f)
private val Muted = Color(0xFF9CA3AF)
private val Danger = Color(0xFFEF4444)
private val Success = Color(0xFF10B981)
private val Accent = Color(0xFF8B5CF6)

/** 删除账号流程（对齐 iOS `AccountDeletionView`）。 */
@Composable
fun V2AccountDeletionScreen(
    onBack: () -> Unit,
    onDeletionComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { V2AccountDeletionRepository(context.applicationContext) }

    var isDeleting by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var stepLabel by remember { mutableStateOf(V2AccountDeletionStep.PREPARING.label) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDeletion() {
        scope.launch {
            isDeleting = true
            showError = false
            errorMessage = null
            val result = repo.deleteAccount { step ->
                stepLabel = step.label
                progress = step.progress
            }
            result.onSuccess {
                completed = true
                isDeleting = false
            }.onFailure {
                isDeleting = false
                errorMessage = it.message ?: "未知错误，请稍后重试"
                showError = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        if (!isDeleting && !completed) {
            V2NavTopBarCentered(
                title = "删除账号",
                onBack = onBack,
                trailing = {
                    TextButton(onClick = onBack) {
                        Text("取消", color = Muted, fontSize = 15.sp)
                    }
                },
            )
        }

        when {
            completed -> CompletedView(onDeletionComplete = onDeletionComplete)
            isDeleting -> ProgressView(progress = progress, stepLabel = stepLabel)
            else -> ConfirmationView(onConfirm = { startDeletion() }, onCancel = onBack)
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("删除失败") },
            text = { Text(errorMessage ?: "未知错误，请稍后重试") },
            confirmButton = {
                TextButton(onClick = {
                    showError = false
                    startDeletion()
                }) {
                    Text("重试", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showError = false }) {
                    Text("取消", color = Muted)
                }
            },
        )
    }
}

@Composable
private fun ConfirmationView(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("确认删除账号？", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("删除后，以下数据将被永久清除：", color = Muted, fontSize = 15.sp)
            DeletionItem("账号信息及登录凭证")
            DeletionItem("AI 对话记录")
            DeletionItem("训练目标与视频备忘录")
            DeletionItem("挥杆记录及视频文件")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = Success, modifier = Modifier.size(12.dp))
                Text("已下载的本地视频将保留在设备上", color = Success, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("此操作不可撤销，请谨慎操作。", color = Danger, fontSize = 14.sp)

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 40.dp)) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
            ) {
                Text("取消", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DeletionItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = Danger, modifier = Modifier.size(14.dp))
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun ProgressView(progress: Float, stepLabel: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = Danger,
                trackColor = Color(0xFF374151),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            Text("${(progress * 100).toInt()}%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Text("正在删除账号数据...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(stepLabel, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(80.dp))
        Text("请勿关闭应用", color = Color(0xFF6B7280), fontSize = 14.sp)
    }
}

@Composable
private fun CompletedView(onDeletionComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Success.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("账号已删除", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("感谢您使用，期待再次相遇！", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDeletionComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("返回登录界面", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
