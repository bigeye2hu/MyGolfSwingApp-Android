package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.Locale

/** 视频笔记横滑卡（对齐 iOS `V2MemoMixedCard`，210×161） */
@Composable
fun V2MemoMixedCard(memo: V2Memo, modifier: Modifier = Modifier, guestPreview: Boolean = false) {
    val cardW = 210.dp
    val cardH = 161.dp
    val thumbW = 80.dp
    val playable = remember(memo.videoUrl, guestPreview) { guestPreview || memoVideoPlayable(memo.videoUrl) }

    Box(modifier = modifier.width(cardW).height(cardH)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.04f),
                )
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.cardWhite)
                .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp)),
        ) {
            Box(modifier = Modifier.width(thumbW).fillMaxHeight()) {
                V2MemoThumbnail(
                    memo = memo,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(0.55f),
                                0.35f to Color.Transparent,
                            ),
                        ),
                )
                memo.durationSec?.takeIf { playable && it > 0 }?.let { sec ->
                    val t = sec.toInt()
                    Text(
                        text = String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 5.dp, top = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(0.7f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                if (playable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            null,
                            tint = DSColors.inkGreen,
                            modifier = Modifier
                                .size(11.dp)
                                .offset(x = 1.dp),
                        )
                    }
                } else {
                    Text(
                        "文件缺失",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(0.54f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier.height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "视频+笔记",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.fairway,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DSColors.mistGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    if (memo.accent != null) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            memo.accentDisplayLabel(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DSColors.goldBright,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DSColors.goldBright.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = memo.displayTitle ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.inkGreen,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                memo.text?.takeIf { it.isNotBlank() }?.let { body ->
                    Text(
                        text = body,
                        fontSize = 10.sp,
                        color = DSColors.shadowGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    memo.metaText?.let {
                        Text(it, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
                    }
                    Spacer(Modifier.weight(1f))
                    if (memo.isLinkedToAnyGoal) {
                        Text(
                            "◎ 目标${memo.linkedGoalIds.size}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = DSColors.fairway,
                        )
                    }
                }
            }
        }
        if (memo.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-7).dp, y = 7.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(DSColors.goldBright)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp),
                )
            }
        }
    }
}
