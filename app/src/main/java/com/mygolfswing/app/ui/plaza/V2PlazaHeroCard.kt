package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2PlazaHeroCard(video: V2PlazaVideo, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 8.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)))
            .clickable(onClick = onTap)
            .padding(16.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("今日精选", fontSize = 11.sp, color = Color.White.copy(0.8f))
            Text(video.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
            Text(
                "${video.coach.displayName} · ${video.viewCount} 次观看",
                fontSize = 11.sp,
                color = Color.White.copy(0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
