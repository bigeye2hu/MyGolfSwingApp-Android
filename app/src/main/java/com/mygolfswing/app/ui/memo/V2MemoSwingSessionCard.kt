package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2MemoSwingSessionCard(item: V2SwingDayItem, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.warmWhite)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onTap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(72.dp).background(DSColors.listMutedBg).padding(vertical = 12.dp),
        ) {
            Text(item.dayNumber, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DSColors.fairway)
            Text(item.monthLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway.copy(alpha = 0.82f))
            Text(item.relativeText, fontSize = 10.sp, color = DSColors.shadowGray, modifier = Modifier.padding(top = 2.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.isInProgress) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.Red))
                }
                Text(
                    if (item.isInProgress) "进行中 · ${item.title}" else item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.inkGreen,
                )
            }
            item.timeRange?.let { Text(it, fontSize = 10.sp, color = DSColors.shadowGray) }
            item.location?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(it, fontSize = 10.sp, color = DSColors.shadowGray)
                    if (item.extraLocationCount > 0) {
                        Text(
                            "+${item.extraLocationCount}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DSColors.fairway,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DSColors.listMutedBg)
                                .border(0.5.dp, DSColors.listCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            if (item.sessionCount >= 2) {
                Text("${item.sessionCount} 次会话", fontSize = 10.sp, color = DSColors.shadowGray)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            item.clipPreviewUrls.take(3).forEach { url ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DSColors.deepGreen),
                ) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            if (item.extraClipCount > 0) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DSColors.mistGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+${item.extraClipCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DSColors.fairway)
                }
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray,
            modifier = Modifier.padding(end = 8.dp).size(16.dp),
        )
    }
}
