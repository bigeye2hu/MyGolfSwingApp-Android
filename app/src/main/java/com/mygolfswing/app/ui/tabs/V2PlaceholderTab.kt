package com.mygolfswing.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome

/**
 * 还没设计完的 Tab 占位页（对齐 iOS `V2PlaceholderTabView`）。
 */
@Composable
fun V2PlaceholderTab(
    tabName: String,
    icon: ImageVector,
    comingSoonNote: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tabName,
                    tint = DSColors.deepGreen,
                    modifier = Modifier.size(38.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tabName,
                    style = DSTypography.titleLarge,
                    color = DSColors.inkGreen,
                )
                Text(
                    text = "正在设计中",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Text(
                text = comingSoonNote,
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DSSpacing.xl),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun V2PlaceholderTabPreview() {
    MyGolfSwingAppAndroidTheme {
        V2PlaceholderTab(
            tabName = "AI",
            icon = Icons.Outlined.AutoAwesome,
            comingSoonNote = "等 AI 页设计完成后接入",
        )
    }
}
