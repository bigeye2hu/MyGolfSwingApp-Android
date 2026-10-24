package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun V2CoachLibraryEditDialog(
    asset: V2CoachLibraryAsset,
    onDismiss: () -> Unit,
    onAssetLoaded: (V2CoachLibraryAsset) -> Unit,
    onSaved: (V2CoachLibraryAsset) -> Unit,
    onRemoved: (V2CoachLibraryAsset) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var displayedAsset by remember(asset.assetId) { mutableStateOf(asset) }
    var loading by remember(asset.assetId) { mutableStateOf(asset.coverUrl.isNullOrBlank() && asset.videoUrl.isNullOrBlank()) }
    var submitting by remember(asset.assetId) { mutableStateOf(false) }
    var error by remember(asset.assetId) { mutableStateOf<String?>(null) }

    LaunchedEffect(asset.assetId) {
        if (!loading) return@LaunchedEffect
        ServiceLocator.coachWorkspaceRepo.coachAsset(asset.assetId)
            .onSuccess { loaded ->
                displayedAsset = loaded
                onAssetLoaded(loaded)
            }
            .onFailure { error = "素材封面暂时无法加载，仍可继续编辑" }
        loading = false
    }

    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2CoachLibraryEditContent(
            asset = displayedAsset,
            loading = loading,
            submitting = submitting,
            error = error,
            onClose = onDismiss,
            onSave = { title, tags ->
                if (!submitting) {
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            ServiceLocator.coachWorkspaceRepo.updateLibraryAsset(
                                assetId = displayedAsset.assetId,
                                title = title,
                                tags = tags,
                            ).onSuccess { updated ->
                                onSaved(updated)
                                onDismiss()
                            }.onFailure {
                                error = "素材信息还没有保存"
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            submitting = false
                        }
                    }
                }
            },
            onRemove = {
                if (!submitting) {
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            ServiceLocator.coachWorkspaceRepo.updateLibraryAsset(
                                assetId = displayedAsset.assetId,
                                savedToLibrary = false,
                            ).onSuccess {
                                onRemoved(displayedAsset)
                                onDismiss()
                            }.onFailure {
                                error = "暂时无法移出素材，请稍后重试"
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            submitting = false
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V2CoachLibraryEditContent(
    asset: V2CoachLibraryAsset,
    loading: Boolean,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (String, List<String>) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(asset.assetId) { mutableStateOf(asset.title) }
    var tags by remember(asset.assetId) { mutableStateOf(normalizeCoachLibraryTags(asset.tags)) }
    var newTag by remember(asset.assetId) { mutableStateOf("") }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val trimmedTitle = title.trim()
    val newTagValue = newTag.trim()

    fun addTag() {
        if (newTagValue.isNotEmpty() && tags.size < 5 && newTagValue !in tags) {
            tags = tags + newTagValue
            newTag = ""
        }
    }

    Box(modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(48.dp)) {
                Text(
                    "素材编辑",
                    color = DSColors.inkGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).testTag("coach-library-edit-title"),
                )
                Box(
                    Modifier.align(Alignment.CenterEnd).size(36.dp).clip(CircleShape)
                        .background(DSColors.cardWhite).clickable(enabled = !submitting, onClick = onClose)
                        .testTag("coach-library-edit-close"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "关闭", tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                }
            }

            Box(
                Modifier.fillMaxWidth().height(186.dp).clip(RoundedCornerShape(16.dp))
                    .testTag("coach-library-edit-cover"),
                contentAlignment = Alignment.Center,
            ) {
                V2CoachMediaCover(asset, Modifier.fillMaxSize())
                if (loading) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Row(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).height(34.dp)
                        .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.48f))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Filled.Photo, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("当前封面", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("标题", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${title.length}/50", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(DSColors.cardWhite).border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = !submitting,
                        singleLine = true,
                        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(DSColors.fairway),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                if (title.isEmpty()) Text("例如：下杆节奏练习", color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 14.sp)
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("coach-library-edit-title-field"),
                    )
                    if (title.isNotEmpty()) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(DSColors.sandBeige)
                                .clickable(enabled = !submitting) { title = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, "清空标题", tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("标签", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("最多5个标签", color = DSColors.shadowGray, fontSize = 10.sp)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        Row(
                            Modifier.height(34.dp).clip(RoundedCornerShape(50)).background(DSColors.mistGreen)
                                .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(50))
                                .clickable(enabled = !submitting) { tags = tags - tag }
                                .padding(horizontal = 11.dp).testTag("coach-library-edit-tag-$tag"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(tag, color = DSColors.deepGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.Close, "删除标签 $tag", tint = DSColors.deepGreen, modifier = Modifier.size(9.dp))
                        }
                    }
                    if (tags.size < 5) {
                        Row(
                            Modifier.height(34.dp).clip(RoundedCornerShape(50)).background(DSColors.cardWhite)
                                .drawBehind {
                                    drawRoundRect(
                                        color = DSColors.shadowGray.copy(alpha = 0.36f),
                                        cornerRadius = CornerRadius(size.height / 2f),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                                            ),
                                        ),
                                    )
                                }
                                .padding(horizontal = 11.dp).testTag("coach-library-edit-new-tag"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Filled.Add, null, tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
                            BasicTextField(
                                value = newTag,
                                onValueChange = { newTag = it },
                                enabled = !submitting,
                                singleLine = true,
                                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 11.sp),
                                cursorBrush = SolidColor(DSColors.fairway),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { addTag() }),
                                decorationBox = { inner ->
                                    Box {
                                        if (newTag.isEmpty()) Text("添加标签", color = DSColors.shadowGray, fontSize = 11.sp)
                                        inner()
                                    }
                                },
                                modifier = Modifier.width(62.dp).testTag("coach-library-edit-new-tag-field"),
                            )
                        }
                    }
                }
                if (newTagValue.isNotEmpty()) {
                    Text(
                        "添加“$newTagValue”",
                        color = DSColors.fairway,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(enabled = tags.size < 5) { addTag() }
                            .padding(horizontal = 4.dp, vertical = 4.dp).testTag("coach-library-edit-add-tag"),
                    )
                }
                error?.let {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(V2CoachPalette.mutedGold.copy(alpha = 0.5f))
                            .border(1.dp, V2CoachPalette.gold.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                            .padding(11.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = V2CoachPalette.gold, modifier = Modifier.size(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("素材信息还没有保存", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("刚才填写的内容仍在，直接再点一次保存即可", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(50.dp).alpha(if (trimmedTitle.isEmpty()) 0.5f else 1f)
                    .clip(RoundedCornerShape(14.dp)).background(DSColors.fairway)
                    .clickable(enabled = trimmedTitle.isNotEmpty() && !submitting) { onSave(title, tags) }
                    .testTag("coach-library-edit-save"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (submitting) "正在保存…" else "保存素材", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                Modifier.fillMaxWidth().height(44.dp).clickable(enabled = !submitting) { showRemoveConfirmation = true }
                    .testTag("coach-library-edit-remove"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Delete, null, tint = Color(0xFF9A4638), modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(7.dp))
                Text("移出素材库", color = Color(0xFF9A4638), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showRemoveConfirmation) {
        V2CoachLibraryRemoveConfirmationSheet(
            busy = submitting,
            confirmTag = "coach-library-edit-remove-confirm",
            onConfirm = { showRemoveConfirmation = false; onRemove() },
            onDismiss = { showRemoveConfirmation = false },
        )
    }
}

internal fun normalizeCoachLibraryTags(values: List<String>): List<String> = values.asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .take(5)
    .toList()
