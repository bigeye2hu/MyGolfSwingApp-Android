package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

/** 主页底部装饰区（对齐 iOS `V2HomeDecorativeFooter`） */
@Composable
fun V2HomeDecorativeFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DSSpacing.Radius.md))
            .background(
                Brush.verticalGradient(
                    listOf(DSColors.mistGreen.copy(alpha = 0.3f), DSColors.warmWhite),
                ),
            )
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.SportsScore,
            contentDescription = null,
            tint = DSColors.deepGreen.copy(alpha = 0.55f),
            modifier = Modifier
                .size(18.dp)
                .padding(bottom = 4.dp),
        )
        Text("\u201C每一杆都值得被记住。\u201D", style = DSTypography.titleMedium, color = DSColors.inkGreen)
        Text("Every Swing Worth Remembering.", style = DSTypography.caption, color = DSColors.shadowGray)
        Text(
            text = "— 挥呗 Memo —",
            style = DSTypography.caption,
            color = DSColors.trophyGold.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
