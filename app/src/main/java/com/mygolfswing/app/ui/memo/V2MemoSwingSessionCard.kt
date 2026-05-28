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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
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
            .background(DSColors.cardWhite)
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onTap)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 10.dp)) {
            Text(item.dayNumber, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text(item.monthLabel, fontSize = 10.sp, color = DSColors.shadowGray)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.relativeText, fontSize = 11.sp, color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            item.timeRange?.let { Text(it, fontSize = 10.sp, color = DSColors.shadowGray) }
            item.location?.let { Text(it, fontSize = 10.sp, color = DSColors.shadowGray) }
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
    }
}
