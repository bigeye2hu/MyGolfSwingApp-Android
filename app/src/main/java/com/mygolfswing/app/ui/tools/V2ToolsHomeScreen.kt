package com.mygolfswing.app.ui.tools

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

/** 底部「工具」页，对齐 iOS `V2ToolsHomeView`. */
@Composable
fun V2ToolsHomeScreen(
    onRecord: () -> Unit,
    onAnalysis: () -> Unit,
    onTrajectory: () -> Unit,
    onAI: () -> Unit,
    onDownload: () -> Unit,
    applyStatusBarPadding: Boolean = true,
) {
    V2RoomPage(Modifier.fillMaxSize().testTag("tools-home-screen")) {
        V2ToolsHomeContent(
            onRecord = onRecord,
            onAnalysis = onAnalysis,
            onTrajectory = onTrajectory,
            onAI = onAI,
            onDownload = onDownload,
            applyStatusBarPadding = applyStatusBarPadding,
        )
    }
}

@Composable
internal fun V2ToolsHomeContent(
    onRecord: () -> Unit,
    onAnalysis: () -> Unit,
    onTrajectory: () -> Unit,
    onAI: () -> Unit,
    onDownload: () -> Unit,
    applyStatusBarPadding: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxSize().then(
            if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("tools-home-top-bar"),
            contentAlignment = Alignment.Center,
        ) {
            Text("工具", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .v2NavigationBarPadding().padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)),
                    ).testTag("tools-home-hero").padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "练得明白\n看得更清楚",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                )
                Text(
                    "录制、分析、轨迹、AI。功能固定，设置不放在这里。",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
            }

            ToolsSectionTitle("练习与分析")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolCard(
                        title = "录制挥杆",
                        detail = "记录一次真实练习",
                        icon = Icons.Filled.Videocam,
                        onClick = onRecord,
                        modifier = Modifier.weight(1f),
                    )
                    ToolCard(
                        title = "图像分析",
                        detail = "慢放、逐帧与画线",
                        icon = Icons.Filled.CenterFocusStrong,
                        onClick = onAnalysis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolCard(
                        title = "轨迹绘制",
                        detail = "手动点位，不自动识别",
                        icon = Icons.Filled.Edit,
                        onClick = onTrajectory,
                        modifier = Modifier.weight(1f),
                    )
                    ToolCard(
                        title = "AI 教练",
                        detail = "把问题讲清楚",
                        icon = Icons.Filled.Forum,
                        onClick = onAI,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ToolsSectionTitle("更多工具")
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(DSColors.cardWhite).clickable(onClick = onDownload)
                    .testTag("tools-video-download").padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("视频下载", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("从公开视频开始", color = DSColors.shadowGray, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ToolsSectionTitle(title: String) {
    Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ToolCard(
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(112.dp).clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite).clickable(onClick = onClick)
            .testTag("tool-card-$title").padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
        Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            detail,
            color = DSColors.shadowGray,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
