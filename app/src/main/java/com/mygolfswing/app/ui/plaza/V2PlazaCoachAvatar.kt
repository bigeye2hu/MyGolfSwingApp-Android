package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2PlazaCoach

/** iOS `V2PlazaCoachRow` 同款头像：远端头像优先，字母渐变作为加载与失败兜底。 */
@Composable
internal fun V2PlazaCoachAvatar(
    coach: V2PlazaCoach,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp,
) {
    val borderModifier = if (borderColor != null && borderWidth > 0.dp) {
        Modifier.border(borderWidth, borderColor, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(coach.avatarTint.gradient))
            .then(borderModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = coach.initialChar,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
        coach.avatarUrl?.trim()?.takeIf(String::isNotEmpty)?.let { avatarUrl ->
            AsyncImage(
                model = avatarUrl,
                contentDescription = "${coach.displayName}头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
