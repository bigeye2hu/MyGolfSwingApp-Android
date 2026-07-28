package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.repo.V2ProfileCloudConsistencyIssue
import com.mygolfswing.app.data.repo.V2ProfileCloudConsistencyIssueTone
import com.mygolfswing.app.data.repo.V2ProfileCloudConsistencyResult
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun V2ProfileCloudConsistencyAuditScreen(onBack: () -> Unit) {
    val profile by V2ProfileStore.profile.collectAsState()
    if (!V2DeveloperToolsAccess.isCloudAuditEnabled(profile)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DSColors.warmWhite),
        ) {
            V2NavTopBarCentered(title = "云端一致性", onBack = onBack)
            Box(
                modifier = Modifier.fillMaxSize().padding(DSSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "此功能仅在调试包对内部测试账号开放",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkedAt by remember { mutableStateOf<Long?>(null) }
    var results by remember { mutableStateOf<List<V2ProfileCloudConsistencyResult>>(emptyList()) }

    fun runAudit() {
        scope.launch {
            loading = true
            error = null
            runCatching { ServiceLocator.cloudConsistencyAuditRepo.runAudit() }
                .onSuccess {
                    results = it
                    checkedAt = System.currentTimeMillis()
                }
                .onFailure { error = it.message ?: "检查失败" }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "云端一致性", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderCard(
                loading = loading,
                hasResult = results.isNotEmpty(),
                onRun = { runAudit() },
            )

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(22.dp))
                    Text(
                        text = "正在检查本机与云端数据...",
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            error?.let { ErrorCard(it) }

            checkedAt?.let {
                Text(
                    text = "最后检查：${formatCheckedAt(it)}",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }

            results.forEach { result ->
                ResultCard(result = result)
            }
        }
    }
}

@Composable
private fun HeaderCard(
    loading: Boolean,
    hasResult: Boolean,
    onRun: () -> Unit,
) {
    AuditCard {
        Text("云端一致性检查", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Text(
            "对比当前账号在本机的实际数据和云端 dataSync 数据，重点发现会导致多端复活的差异。",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
        Button(
            onClick = onRun,
            enabled = !loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (hasResult) "重新检查" else "开始检查",
                style = DSTypography.bodyBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ResultCard(result: V2ProfileCloudConsistencyResult) {
    AuditCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (result.isHealthy) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = if (result.isHealthy) DSColors.fairway else DSColors.trophyGold,
            )
            Text(result.name, style = DSTypography.titleMedium, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            Text(
                result.statusText,
                style = DSTypography.label,
                color = if (result.isHealthy) DSColors.fairway else DSColors.flagRed,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatPill(label = "本机", value = result.localCount.toString(), modifier = Modifier.weight(1f))
            StatPill(label = "云端有效", value = result.cloudActiveCount.toString(), modifier = Modifier.weight(1f))
            StatPill(label = "云端删除", value = result.cloudDeletedCount.toString(), modifier = Modifier.weight(1f))
        }
        Text(
            result.summary,
            style = DSTypography.caption,
            color = if (result.isHealthy) DSColors.fairway else DSColors.flagRed,
        )
        result.issues.forEach { issue -> IssueRow(issue) }
    }
}

@Composable
private fun IssueRow(issue: V2ProfileCloudConsistencyIssue) {
    val color = issue.tone.color()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (issue.tone == V2ProfileCloudConsistencyIssueTone.DANGER) {
                Icons.Filled.ErrorOutline
            } else {
                Icons.Filled.WarningAmber
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(issue.title, style = DSTypography.caption.copy(fontWeight = FontWeight.SemiBold), color = DSColors.inkGreen)
            Text(issue.detail, style = DSTypography.label, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.mistGreen)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Text(label, style = DSTypography.label, color = DSColors.shadowGray)
    }
}

@Composable
private fun ErrorCard(message: String) {
    AuditCard {
        Text("检查失败", style = DSTypography.titleMedium, color = DSColors.flagRed)
        Text(message, style = DSTypography.caption, color = DSColors.shadowGray)
    }
}

@Composable
private fun AuditCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DSColors.cardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun V2ProfileCloudConsistencyIssueTone.color(): Color = when (this) {
    V2ProfileCloudConsistencyIssueTone.INFO -> DSColors.fairway
    V2ProfileCloudConsistencyIssueTone.DANGER -> DSColors.flagRed
    V2ProfileCloudConsistencyIssueTone.WARNING -> DSColors.trophyGold
    V2ProfileCloudConsistencyIssueTone.MUTED -> DSColors.shadowGray
}

private fun formatCheckedAt(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(ms))
