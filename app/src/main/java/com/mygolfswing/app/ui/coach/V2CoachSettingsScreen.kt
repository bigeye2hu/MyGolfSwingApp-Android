package com.mygolfswing.app.ui.coach

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun V2CoachSettingsDialog(
    session: V2Session,
    roleStatusText: String,
    deliveryPendingCount: Int,
    onDismiss: () -> Unit,
    onRequestRoleSelection: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeDownloads by V2VideoFileStore.workspaceAnalysisDownloadCount.collectAsState()
    var cacheBytes by remember(session.userId) { mutableLongStateOf(0L) }
    var cacheLoading by remember(session.userId) { mutableStateOf(true) }
    var clearingCache by remember(session.userId) { mutableStateOf(false) }

    LaunchedEffect(session.userId) {
        cacheBytes = session.userId?.let { userId ->
            V2VideoFileStore.cleanupWorkspaceAnalysisCache(context, userId).bytes
        } ?: 0L
        cacheLoading = false
    }

    Dialog(
        onDismissRequest = { if (!clearingCache) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2CoachSettingsContent(
            session = session,
            roleStatusText = roleStatusText,
            deliveryPendingCount = deliveryPendingCount,
            cacheBytes = cacheBytes,
            cacheLoading = cacheLoading,
            cacheBusy = activeDownloads > 0 || clearingCache,
            onDone = onDismiss,
            onRequestRoleSelection = {
                onDismiss()
                onRequestRoleSelection()
            },
            onClearCache = {
                val userId = session.userId
                if (userId != null && activeDownloads == 0 && !clearingCache) {
                    clearingCache = true
                    scope.launch {
                        V2VideoFileStore.clearWorkspaceAnalysisCache(context, userId)
                        cacheBytes = 0L
                        clearingCache = false
                    }
                }
            },
        )
    }
}

@Composable
internal fun V2CoachSettingsContent(
    session: V2Session,
    roleStatusText: String,
    deliveryPendingCount: Int,
    cacheBytes: Long,
    cacheLoading: Boolean,
    cacheBusy: Boolean,
    onDone: () -> Unit,
    onRequestRoleSelection: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    val hasActiveDelivery = deliveryPendingCount > 0
    val cacheCanBeCleared = cacheBytes > 0L && !cacheLoading && !cacheBusy
    val cacheDetail = when {
        cacheLoading -> "正在检查本机缓存…"
        cacheBytes <= 0L -> "当前没有缓存，系统最长保留7天"
        else -> "已占用 ${formatCoachCacheBytes(cacheBytes)}，点击可清理；系统最长保留7天"
    }
    val displayName = session.nickname ?: session.username ?: "挥呗教练"

    Box(modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(DSColors.cardWhite),
            ) {
                Text(
                    "教练版设置",
                    color = DSColors.deepGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).testTag("coach-settings-title"),
                )
                Text(
                    "完成",
                    color = DSColors.fairway,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDone).padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("coach-settings-done"),
                )
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(DSColors.cardWhite).padding(16.dp)
                        .testTag("coach-settings-identity"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(DSColors.mistGreen)
                            .border(2.dp, DSColors.trophyGold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        V2UserAvatar(
                            avatar = session.avatar,
                            modifier = Modifier.fillMaxSize(),
                            fallbackFontSize = 21.sp,
                            placeholderIconSize = 29.dp,
                            fallbackText = displayName,
                            fallbackColor = DSColors.sandBeige,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            displayName,
                            color = DSColors.inkGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
                            Text(roleStatusText, color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                CoachSettingsSectionLabel("身份")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite),
                ) {
                    CoachSettingsRow(
                        icon = Icons.Filled.ManageAccounts,
                        title = "教练版",
                        detail = roleStatusText,
                        accent = DSColors.trophyGold,
                        showsChevron = false,
                    )
                    Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(1.dp).background(DSColors.borderLine))
                    CoachSettingsRow(
                        icon = Icons.Filled.SwapHoriz,
                        title = "切换身份",
                        detail = "返回身份选择页，不退出当前账号",
                        accent = DSColors.fairway,
                        enabled = !hasActiveDelivery,
                        onClick = onRequestRoleSelection,
                        modifier = Modifier.testTag("coach-settings-role-switch"),
                    )
                }

                CoachSettingsSectionLabel("本机空间")
                CoachSettingsRow(
                    icon = Icons.Filled.Storage,
                    title = "分析视频缓存",
                    detail = cacheDetail,
                    accent = DSColors.fairway,
                    showsChevron = cacheBytes > 0L,
                    enabled = cacheCanBeCleared,
                    trailingProgress = cacheLoading || cacheBusy,
                    onClick = { showClearCacheConfirmation = true },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(DSColors.cardWhite).testTag("coach-settings-cache"),
                )

                Text(
                    if (hasActiveDelivery) {
                        "还有${deliveryPendingCount}条内容待发送，完成后即可切换身份；当前内容不会丢失。"
                    } else {
                        "教练端与练习者端使用独立的首页和操作路径，切换不会删除任何数据。"
                    },
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 1.dp).testTag("coach-settings-delivery-copy"),
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    if (showClearCacheConfirmation) {
        Dialog(
            onDismissRequest = { showClearCacheConfirmation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f))
                    .clickable { showClearCacheConfirmation = false }.padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 330.dp).clip(RoundedCornerShape(22.dp))
                        .background(DSColors.cardWhite).clickable { }.testTag("coach-settings-cache-dialog"),
                ) {
                    Column(
                        Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "清理分析视频缓存？",
                            color = DSColors.deepGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "只删除为了逐帧和口播分析下载到本机的临时视频，不会删除云端教学内容或素材库。",
                            color = DSColors.shadowGray,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                    Row(Modifier.fillMaxWidth().height(52.dp)) {
                        Box(
                            Modifier.weight(1f).fillMaxSize().clickable { showClearCacheConfirmation = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("取消", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.size(width = 1.dp, height = 52.dp).background(DSColors.borderLine))
                        Box(
                            Modifier.weight(1f).fillMaxSize().clickable {
                                showClearCacheConfirmation = false
                                onClearCache()
                            }.testTag("coach-settings-cache-confirm"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("清理缓存", color = DSColors.flagRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachSettingsSectionLabel(text: String) {
    Text(text, color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CoachSettingsRow(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier,
    showsChevron: Boolean = true,
    enabled: Boolean = true,
    trailingProgress: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier.heightIn(min = 68.dp).alpha(if (enabled) 1f else 0.62f)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                detail,
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            trailingProgress -> CircularProgressIndicator(Modifier.size(16.dp), color = DSColors.fairway, strokeWidth = 2.dp)
            showsChevron -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = DSColors.shadowGray.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

internal fun formatCoachCacheBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    bytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
