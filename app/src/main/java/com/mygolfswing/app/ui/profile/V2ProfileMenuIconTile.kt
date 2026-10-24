package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A 40pt profile-list icon tile matching the current iOS SF Symbol treatment. */
internal enum class V2ProfileMenuIconStyle {
    STANDARD,
    INTERNAL_DRIVE,
    FILLED_CIRCLE_REFRESH,
    LOCK_SHIELD,
}

@Composable
internal fun V2ProfileMenuIconTile(
    icon: ImageVector,
    background: Color,
    modifier: Modifier = Modifier,
    style: V2ProfileMenuIconStyle = V2ProfileMenuIconStyle.STANDARD,
) {
    Box(
        modifier = modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            V2ProfileMenuIconStyle.INTERNAL_DRIVE -> Canvas(
                Modifier.size(16.dp).testTag("profile-internal-drive-glyph"),
            ) {
                val stroke = size.minDimension * 0.075f
                drawRoundRect(
                    color = Color.White,
                    size = Size(size.width, size.height * 0.82f),
                    cornerRadius = CornerRadius(size.minDimension * 0.20f),
                )
                drawLine(
                    color = background,
                    start = Offset(size.width * 0.16f, size.height * 0.54f),
                    end = Offset(size.width * 0.84f, size.height * 0.54f),
                    strokeWidth = stroke,
                )
                drawCircle(background, size.minDimension * 0.055f, Offset(size.width * 0.72f, size.height * 0.67f))
                drawCircle(background, size.minDimension * 0.055f, Offset(size.width * 0.84f, size.height * 0.67f))
            }
            V2ProfileMenuIconStyle.FILLED_CIRCLE_REFRESH -> V2FilledCircleRefreshGlyph(
                foreground = Color.White,
                cutout = background,
                modifier = Modifier.size(16.dp).testTag("profile-filled-refresh-glyph"),
            )
            V2ProfileMenuIconStyle.LOCK_SHIELD -> V2LockShieldGlyph(
                foreground = Color.White,
                cutout = background,
                modifier = Modifier.size(16.dp),
            )
            V2ProfileMenuIconStyle.STANDARD -> Icon(
                icon,
                null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Android equivalent of SF Symbol `arrow.counterclockwise.circle.fill`. */
@Composable
internal fun V2FilledCircleRefreshGlyph(
    foreground: Color,
    cutout: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(CircleShape).background(foreground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = cutout,
            // Material's refresh arrow is clockwise; mirroring it matches the
            // counterclockwise SF Symbol used by iOS.
            modifier = Modifier.fillMaxSize(0.75f).graphicsLayer(scaleX = -1f),
        )
    }
}

@Composable
internal fun V2LockShieldGlyph(
    foreground: Color,
    cutout: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.testTag("profile-lock-shield-glyph"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Shield, null, tint = foreground, modifier = Modifier.fillMaxSize())
        Icon(Icons.Filled.Lock, null, tint = cutout, modifier = Modifier.fillMaxSize(0.44f))
    }
}
