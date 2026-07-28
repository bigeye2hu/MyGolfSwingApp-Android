package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val HOME_AVATAR_DEV_LONG_PRESS_MS = 1_500L

/**
 * V2 主页头部（对齐 iOS `V2HomeHeader`）。
 *
 * - 左：48dp 头像（金色 2.5dp 边）
 * - 中：「问候，用户名」+ 日期 · 今日还有 N 个目标 chip
 * - 右：42dp + 添加按钮（绿描边）
 */
@Composable
fun V2HomeHeader(
    goalCount: Int,
    displayName: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onAddTap: () -> Unit = {},
    onAvatarLongPress: (() -> Unit)? = null,
) {
    val ringYellow = Color(0xFFE0A82E)
    val titleInk = Color(0xFF1B2A21)
    val dateGreen = DSColors.fairway
    val dotGray = Color(0xFF9AAB9F)
    val mutedGray = Color(0xFF6C757D)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = DSColors.fairway.copy(alpha = 0.18f),
                    spotColor = DSColors.fairway.copy(alpha = 0.18f),
                )
                .border(width = 2.5.dp, color = ringYellow, shape = CircleShape)
                .then(
                    if (onAvatarLongPress == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(onAvatarLongPress) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                try {
                                    waitForUpOrCancellationOrTimeout(HOME_AVATAR_DEV_LONG_PRESS_MS)
                                } catch (_: TimeoutCancellationException) {
                                    onAvatarLongPress()
                                    waitForUpOrCancellation()
                                }
                            }
                        }
                    },
                ),
        ) {
            V2UserAvatar(
                avatar = avatarUrl,
                modifier = Modifier.fillMaxSize(),
                emojiFontSize = 22.sp,
                placeholderIconSize = 28.dp,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "${V2Format.greeting()}，$displayName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = V2Format.headerDate(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = dateGreen,
                )
                Text(text = "·", fontSize = 11.sp, color = dotGray)
                Text(text = "今日还有", fontSize = 11.sp, color = mutedGray)
                Text(
                    text = "$goalCount 个目标",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringYellow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ringYellow.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = DSColors.fairway.copy(alpha = 0.22f),
                    spotColor = DSColors.fairway.copy(alpha = 0.22f),
                )
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 1.5.dp, color = DSColors.fairway, shape = CircleShape)
                .clickable { onAddTap() },
            contentAlignment = Alignment.Center,
        ) {
            LightPlusIcon()
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.waitForUpOrCancellationOrTimeout(
    timeoutMillis: Long,
) {
    withTimeout(timeoutMillis) {
        waitForUpOrCancellation()
    }
}

@Composable
private fun LightPlusIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val arm = size.minDimension * 0.34f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLine(
            color = DSColors.fairway,
            start = Offset(center.x - arm, center.y),
            end = Offset(center.x + arm, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DSColors.fairway,
            start = Offset(center.x, center.y - arm),
            end = Offset(center.x, center.y + arm),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
