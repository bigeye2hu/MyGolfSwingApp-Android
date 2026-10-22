package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.interaction.workspaceFeedDate
import com.mygolfswing.app.ui.interaction.workspaceThreadSourceTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoachPracticeListSheet(
    studentName: String,
    items: List<V2CoachWorkspaceItem>,
    currentItemId: String,
    onDismiss: () -> Unit,
    onSelect: (V2CoachWorkspaceItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val roots = items.filter { it.parentItemId == null && !it.isRemovedContent }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CoachPaper,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${studentName.ifBlank { "当前学员" }}的练习", color = CoachGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("只切换这位学员的共享内容", color = DSColors.shadowGray, fontSize = 12.sp)
                }
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "关闭", tint = CoachGreen, modifier = Modifier.size(16.dp))
                }
            }
            if (roots.isEmpty()) {
                Text(
                    "还没有可切换的练习",
                    color = DSColors.shadowGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            } else {
                LazyColumn {
                    items(roots, key = { it.itemId }) { item ->
                        val current = item.itemId == currentItemId
                        val cover = item.primaryAsset?.coverUrl ?: item.coverUrl
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (!current) onSelect(item) else onDismiss()
                            }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(width = 72.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).background(DSColors.mistGreen),
                            ) {
                                if (!cover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    workspaceThreadSourceTitle(item, friendSpace = false),
                                    color = DSColors.inkGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    workspaceFeedDate(item.latestActivityAt ?: item.createdAt),
                                    color = DSColors.shadowGray,
                                    fontSize = 12.sp,
                                )
                            }
                            if (current) {
                                Icon(Icons.Filled.Check, "当前练习", tint = CoachGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
