package com.mygolfswing.app.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2SplashScreen(message: String = "正在初始化…") {
    val infinite = rememberInfiniteTransition(label = "v2-splash")
    val logoScale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "logoScale",
    )
    val glow by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Restart),
        label = "rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF10261C),
                        Color(0xFF183D2B),
                        Color(0xFF07130E),
                    ),
                ),
            ),
    ) {
        SplashSunburst(rotation = rotation, alpha = 0.22f * glow)
        SplashParticles(alpha = glow)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .scale(logoScale)
                .blur(18.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF4ADE80).copy(alpha = 0.26f * glow),
                            Color(0xFF22C55E).copy(alpha = 0.08f * glow),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                "朝着更好的一杆",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f),
            )
            Text(
                "挥呗",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4ADE80),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Where EverySwing Matters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp),
        ) {
            LoadingDots()
            Text(
                message,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.48f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SplashSunburst(rotation: Float, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation)
            .blur(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        repeat(12) { index ->
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 420.dp)
                    .offset(y = (-210).dp)
                    .rotate(index * 30f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4ADE80).copy(alpha = alpha), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun SplashParticles(alpha: Float) {
    val particles = listOf(
        Triple(42.dp, 86.dp, 5.dp),
        Triple(310.dp, 132.dp, 3.dp),
        Triple(80.dp, 612.dp, 4.dp),
        Triple(330.dp, 682.dp, 6.dp),
        Triple(220.dp, 520.dp, 3.dp),
        Triple(130.dp, 738.dp, 4.dp),
    )
    Box(Modifier.fillMaxSize()) {
        particles.forEachIndexed { idx, (x, y, size) ->
            Box(
                modifier = Modifier
                    .offset(x, y)
                    .size(size)
                    .blur(if (idx % 2 == 0) 1.dp else 0.dp)
                    .background(
                        if (idx % 3 == 0) Color.White.copy(alpha = 0.28f * alpha)
                        else Color(0xFF4ADE80).copy(alpha = 0.45f * alpha),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun LoadingDots() {
    Row {
        repeat(3) { index ->
            val infinite = rememberInfiniteTransition(label = "dot-$index")
            val dotAlpha by infinite.animateFloat(
                initialValue = 0.30f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, delayMillis = index * 180),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dotAlpha-$index",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .background(Color(0xFF4ADE80).copy(alpha = dotAlpha), CircleShape),
            )
        }
    }
    Spacer(Modifier.height(0.dp))
}
