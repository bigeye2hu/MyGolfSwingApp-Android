package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.store.V2UserProfile
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2ProfileUserCard(
    profile: V2UserProfile,
    modifier: Modifier = Modifier,
    onAvatarTap: () -> Unit = {},
    onEditTap: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.08f),
                spotColor = DSColors.fairway.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg),
    ) {
        AvatarBlock(avatarUrl = profile.avatar, showVerified = profile.isLoggedIn, onTap = onAvatarTap)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.nickname,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DSColors.inkGreen,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                EditButton(onTap = onEditTap)
            }
            if (profile.username.isNotBlank()) {
                Text(
                    text = profile.username,
                    fontSize = 13.sp,
                    color = DSColors.shadowGray,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                MembershipChip(text = profile.membershipTier)
                CreditsChip(credits = profile.credits)
            }
        }
    }
}

@Composable
private fun AvatarBlock(avatarUrl: String?, showVerified: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(85.dp)
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(85.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFE0A82E).copy(alpha = 0.3f),
                    spotColor = Color(0xFFE0A82E).copy(alpha = 0.3f),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFE0A82E), Color(0xFFF5C842))),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "头像",
                        tint = DSColors.shadowGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }
        if (showVerified) {
            VerifiedBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
            )
        }
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                ambientColor = DSColors.fairway.copy(alpha = 0.2f),
                spotColor = DSColors.fairway.copy(alpha = 0.2f),
            )
            .clip(CircleShape)
            .background(DSColors.fairway)
            .border(width = 2.dp, color = DSColors.warmWhite, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
    }
}

@Composable
private fun EditButton(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(DSColors.mistGreen)
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "编辑",
            tint = DSColors.fairway,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun MembershipChip(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFE0A82E), Color(0xFFF5C842))),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun CreditsChip(credits: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.mistGreen)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = DSColors.trophyGold,
            modifier = Modifier.size(10.dp),
        )
        Text(text = "$credits 积分", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DSColors.fairway)
    }
}
