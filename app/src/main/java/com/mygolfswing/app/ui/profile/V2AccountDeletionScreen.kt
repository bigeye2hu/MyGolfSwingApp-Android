package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.repo.V2AccountDeletionRepository
import com.mygolfswing.app.data.repo.V2AccountDeletionStep
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Muted = DSColors.shadowGray
private val Danger = DSColors.flagRed
private val Success = DSColors.fairway
private val Accent = DSColors.fairway

/** 删除账号流程（对齐 iOS `AccountDeletionView`）。 */
@Composable
fun V2AccountDeletionScreen(
    onBack: () -> Unit,
    onDeletionComplete: () -> Unit,
    onRequestLogin: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val scope = rememberCoroutineScope()
    val repo = remember { V2AccountDeletionRepository() }

    var isDeleting by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var stepLabel by remember { mutableStateOf(V2AccountDeletionStep.PREPARING.label) }
    var showFinalConfirm by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDeletion() {
        if (isDeleting || completed) return
        if (!profile.isLoggedIn) {
            errorMessage = "请先登录后再删除账号"
            showError = true
            return
        }
        scope.launch {
            isDeleting = true
            showFinalConfirm = false
            showError = false
            errorMessage = null
            try {
                val result = repo.deleteAccount { step ->
                    stepLabel = step.label
                    progress = step.progress
                }
                result.onSuccess {
                    completed = true
                }.onFailure {
                    errorMessage = accountDeletionError("删除失败，请稍后重试；如多次失败，请联系客服协助处理", it.message)
                    showError = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                errorMessage = accountDeletionThrowableError("删除失败，请稍后重试；如多次失败，请联系客服协助处理", e)
                showError = true
            } finally {
                isDeleting = false
            }
        }
    }

    val uiState = when {
        completed -> V2AccountDeletionUiState.COMPLETED
        isDeleting -> V2AccountDeletionUiState.PROGRESS
        else -> V2AccountDeletionUiState.CONFIRMATION
    }
    V2AccountDeletionContent(
        state = uiState,
        isLoggedIn = profile.isLoggedIn,
        progress = progress,
        stepLabel = stepLabel,
        onCancel = onBack,
        onConfirm = { showFinalConfirm = true },
        onLogin = onRequestLogin,
        onDeletionComplete = onDeletionComplete,
    )

    if (showFinalConfirm) {
        AccountDeletionDecisionDialog(
            title = "最后确认",
            message = "删除后无法恢复账号和相关数据。确定要永久删除吗？",
            secondaryLabel = "再想想",
            primaryLabel = "永久删除",
            onSecondary = { showFinalConfirm = false },
            onPrimary = ::startDeletion,
        )
    }

    if (showError) {
        AccountDeletionDecisionDialog(
            title = "删除未完成",
            message = errorMessage ?: "未知错误，请稍后重试",
            secondaryLabel = "取消",
            primaryLabel = "重试",
            errorStyle = true,
            onSecondary = { showError = false },
            onPrimary = {
                showError = false
                startDeletion()
            },
        )
    }
}

internal enum class V2AccountDeletionUiState {
    CONFIRMATION,
    PROGRESS,
    COMPLETED,
}

@Composable
internal fun V2AccountDeletionContent(
    state: V2AccountDeletionUiState,
    isLoggedIn: Boolean,
    progress: Float,
    stepLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onLogin: () -> Unit,
    onDeletionComplete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            AccountDeletionTopBar(
                showCancel = state == V2AccountDeletionUiState.CONFIRMATION,
                onCancel = onCancel,
            )
            when (state) {
                V2AccountDeletionUiState.CONFIRMATION -> ConfirmationView(
                    isLoggedIn = isLoggedIn,
                    onConfirm = onConfirm,
                    onLogin = onLogin,
                    onCancel = onCancel,
                )
                V2AccountDeletionUiState.PROGRESS -> ProgressView(progress = progress, stepLabel = stepLabel)
                V2AccountDeletionUiState.COMPLETED -> CompletedView(onDeletionComplete = onDeletionComplete)
            }
        }
    }
}

@Composable
private fun AccountDeletionTopBar(showCancel: Boolean, onCancel: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(DSColors.warmWhite).v2StatusBarPadding().height(44.dp),
    ) {
        Text(
            "删除账号",
            color = DSColors.inkGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        if (showCancel) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
            ) {
                Text("取消", color = DSColors.fairway, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ConfirmationView(
    isLoggedIn: Boolean,
    onConfirm: () -> Unit,
    onLogin: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .v2NavigationBarPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        V2PageArtworkBackground(
            artwork = V2PageArtwork.FAIRWAY,
            tone = V2PageArtworkTone.DARK_GREEN,
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Text("删除挥呗账号", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("请在继续前确认需要清除的数据", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.fairway.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("删除后将永久清除", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            DeletionItem("账号信息及登录凭证")
            DeletionItem("好友关系及聊天记录")
            DeletionItem("AI 对话记录与训练心得")
            DeletionItem("视频备忘录及挥杆记录")
            Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = Success, modifier = Modifier.size(12.dp))
                Text("已单独下载到设备的视频不会被删除", color = Success, fontSize = 13.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Danger.copy(alpha = 0.07f))
                .border(1.dp, Danger.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(17.dp))
            Text("此操作不可撤销，账号及上述数据删除后无法恢复。", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp, bottom = 22.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "取消",
                    color = DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        .background(DSColors.mistGreen).clickable(onClick = onCancel)
                        .padding(top = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                if (isLoggedIn) "继续" else "登录后继续",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isLoggedIn) Danger else Accent)
                    .clickable(onClick = { if (isLoggedIn) onConfirm() else onLogin() })
                    .padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun DeletionItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Cancel, contentDescription = null, tint = Danger, modifier = Modifier.size(14.dp))
        Text(text, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProgressView(progress: Float, stepLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).padding(horizontal = 24.dp)
                .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(24.dp)).background(DSColors.cardWhite)
                .border(1.dp, DSColors.fairway.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = Danger,
                    trackColor = DSColors.mistGreen,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    color = DSColors.inkGreen,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("正在安全删除账号", color = DSColors.inkGreen, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(stepLabel, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.HourglassBottom, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                Text("请保持应用开启，完成前不要退出", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CompletedView(onDeletionComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).padding(horizontal = 24.dp)
                .shadow(22.dp, RoundedCornerShape(24.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.16f))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DSColors.fairway.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            V2PageArtworkBackground(
                artwork = V2PageArtwork.FAIRWAY,
                tone = V2PageArtworkTone.DARK_GREEN,
                modifier = Modifier.fillMaxWidth().height(142.dp),
            ) {
                Box(
                    Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Column(
                Modifier.fillMaxWidth().background(DSColors.cardWhite).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("账号已删除", color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "账号、本机登录状态及相关数据已完成清理，感谢你使用挥呗。",
                        color = Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    "返回首页",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                        .background(Accent).clickable(onClick = onDeletionComplete).padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun AccountDeletionDecisionDialog(
    title: String,
    message: String,
    secondaryLabel: String,
    primaryLabel: String,
    onSecondary: () -> Unit,
    onPrimary: () -> Unit,
    errorStyle: Boolean = false,
) {
    val headerBrush = if (errorStyle) {
        Brush.linearGradient(listOf(Danger.copy(alpha = 0.88f), DSColors.deepGreen.copy(alpha = 0.82f)))
    } else {
        Brush.linearGradient(listOf(Danger, Danger.copy(alpha = 0.72f)))
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (errorStyle) 0.40f else 0.46f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 340.dp).padding(horizontal = 24.dp)
                .shadow(22.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.24f))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Danger.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
        ) {
            Column(
                Modifier.fillMaxWidth().height(104.dp).background(headerBrush),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    if (errorStyle) Icons.Filled.Warning else Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.height(7.dp))
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(
                Modifier.fillMaxWidth().background(DSColors.cardWhite).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    message,
                    color = Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        secondaryLabel,
                        color = DSColors.inkGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(DSColors.mistGreen).clickable(onClick = onSecondary).padding(top = 12.dp),
                    )
                    Text(
                        primaryLabel,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(Danger).clickable(onClick = onPrimary).padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

internal fun accountDeletionError(base: String, debugDetail: String?): String {
    return accountDeletionError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun accountDeletionError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun accountDeletionThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return accountDeletionError(base, error.message)
}
