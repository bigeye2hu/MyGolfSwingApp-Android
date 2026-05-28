package com.mygolfswing.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * V2 通用顶栏：左返回箭头 + 标题 + 右侧自定义动作（默认空）。
 */
@Composable
fun V2NavTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(56.dp)
            .padding(horizontal = DSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        V2BackIconButton(onClick = onBack)
        Spacer(Modifier.size(8.dp))
        Text(
            text = title,
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    }
}

@Composable
fun V2NavTopBarCentered(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(56.dp),
    ) {
        Text(
            text = title,
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        V2BackIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = DSSpacing.sm),
        )
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = DSSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                trailing()
            }
        }
    }
}

@Composable
fun V2BackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "返回",
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .semantics { role = Role.Button }
            .background(DSColors.cardWhite, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, radius = 20.dp),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = DSColors.inkGreen,
        )
    }
}

@Composable
fun V2MissingEntityScreen(
    message: String,
    onBack: () -> Unit,
    title: String = "返回",
) {
    Column(Modifier.fillMaxSize().v2FullScreenInsets()) {
        V2NavTopBar(title = title, onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(message, color = DSColors.shadowGray)
        }
    }
}
