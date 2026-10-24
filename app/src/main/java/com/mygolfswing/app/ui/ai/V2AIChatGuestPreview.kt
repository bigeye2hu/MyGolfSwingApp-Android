package com.mygolfswing.app.ui.ai

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors

internal val v2AiGuestQuestions = listOf(
    "我右曲比较严重，应该先检查什么？",
    "如何判断下杆是不是外下杆？",
    "练习场 30 分钟怎么安排更有效？",
)

@Composable
internal fun V2AIChatGuestPreviewCard(
    onSelectQuestion: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().shadow(
            10.dp,
            RoundedCornerShape(22.dp),
            ambientColor = DSColors.fairway.copy(alpha = 0.08f),
            spotColor = DSColors.fairway.copy(alpha = 0.08f),
        ).clip(RoundedCornerShape(22.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Psychology, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("AI 教练预览", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "先看看 AI 会怎么拆解问题；点击示例只会填入草稿，真正发送前再登录。",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AIChatGuestTile(
                R.drawable.v2_quickadd_video,
                Icons.Filled.Visibility,
                "看问题",
                "理解提问方式",
                "预览",
                DSColors.trophyGold,
                Modifier.weight(1f),
            )
            AIChatGuestTile(
                R.drawable.v2_quickadd_swing,
                Icons.Filled.Edit,
                "填草稿",
                "不保存历史",
                "只读",
                DSColors.fairway,
                Modifier.weight(1f),
            )
            AIChatGuestTile(
                R.drawable.homepage_analysis,
                Icons.Filled.Lock,
                "发送前登录",
                "保存训练建议",
                "需登录",
                Color(0xFF5B8FFF),
                Modifier.weight(1f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("可以这样问", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            v2AiGuestQuestions.forEach { question ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(DSColors.warmWhite.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        .clickable { onSelectQuestion(question) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.FormatQuote, null, tint = DSColors.fairway, modifier = Modifier.size(10.dp))
                        Text(
                            question,
                            color = DSColors.inkGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = DSColors.shadowGray, modifier = Modifier.size(10.dp))
                    }
                    Text(
                        "填入草稿",
                        color = DSColors.shadowGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AIChatCapabilityPill(Icons.Filled.Videocam, "登录后结合视频", Modifier.weight(1f))
            AIChatCapabilityPill(Icons.AutoMirrored.Filled.MenuBook, "建议沉淀 Memo", Modifier.weight(1f))
            AIChatCapabilityPill(Icons.Filled.Schedule, "保留对话历史", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50))
                .background(DSColors.deepGreen).clickable(onClick = onLogin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Text("登录/注册后开始对话", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
internal fun V2AIChatGuestDraftNotice(onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().shadow(
            6.dp,
            RoundedCornerShape(16.dp),
            ambientColor = DSColors.fairway.copy(alpha = 0.06f),
            spotColor = DSColors.fairway.copy(alpha = 0.06f),
        ).clip(RoundedCornerShape(16.dp)).background(DSColors.cardWhite)
            .clickable(onClick = onLogin).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(DSColors.fairway), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LockOpen, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("问题已放入草稿", color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("登录后再发送，AI 建议会保存到你的训练历史。", color = DSColors.shadowGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("登录", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).background(DSColors.mistGreen.copy(alpha = 0.84f)).padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun AIChatGuestTile(
    @DrawableRes imageRes: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
    ) {
        Image(painterResource(imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.68f)))))
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(accent.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    badge,
                    color = DSColors.deepGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.86f))
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(horizontal = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AIChatCapabilityPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(28.dp).clip(RoundedCornerShape(50)).background(DSColors.mistGreen.copy(alpha = 0.76f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(10.dp))
        Text(text, color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
    }
}
