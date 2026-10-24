package com.mygolfswing.app.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import kotlinx.coroutines.delay

internal const val V2_SPLASH_MIN_DURATION_MS = 800L

internal fun splashRemainingDelayMillis(
    startedAtMillis: Long,
    nowMillis: Long,
    minimumDurationMillis: Long = V2_SPLASH_MIN_DURATION_MS,
): Long = (minimumDurationMillis - (nowMillis - startedAtMillis)).coerceAtLeast(0L)

@Composable
fun V2SplashScreen(message: String = "正在初始化...") {
    var logoVisible by remember { mutableStateOf(false) }
    var glowVisible by remember { mutableStateOf(false) }
    var particlesVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(120)
        glowVisible = true
        delay(100)
        particlesVisible = true
        delay(60)
        textVisible = true
    }

    val infinite = rememberInfiniteTransition(label = "v2-splash")
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "logoScale",
    )
    val logoOpacity by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "logoOpacity",
    )
    val glowOpacity by animateFloatAsState(
        targetValue = if (glowVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "glowOpacity",
    )
    val sunburstOpacity by animateFloatAsState(
        targetValue = if (glowVisible) 0.6f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "sunburstOpacity",
    )
    val particlesOpacity by animateFloatAsState(
        targetValue = if (particlesVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "particlesOpacity",
    )
    val textOpacity by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "textOpacity",
    )
    val textOffset by animateFloatAsState(
        targetValue = if (textVisible) 0f else 30f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "textOffset",
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
            .background(Color.Black)
            .testTag("splash-screen"),
    ) {
        Image(
            painter = painterResource(id = R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        GradientOverlay(glowOpacity = glowOpacity)
        SplashSunburst(rotation = rotation, alpha = sunburstOpacity)
        SplashParticles(alpha = particlesOpacity)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(logoScale)
                .graphicsLayerAlpha(logoOpacity)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF4ADE80).copy(alpha = 0.35f * glowOpacity),
                            Color(0xFF22C55E).copy(alpha = 0.10f * glowOpacity),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
                .padding(horizontal = 34.dp, vertical = 42.dp)
                .testTag("splash-brand"),
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
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF4ADE80), Color(0xFF22C55E)),
                    ),
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Where EverySwing Matters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f),
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = textOffset.dp.roundToPx()) }
                    .graphicsLayerAlpha(textOpacity),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
        ) {
            LoadingDots()
            Text(
                message,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.40f),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayerAlpha(textOpacity)
                    .testTag("splash-loading"),
            )
        }
    }
}

@Composable
private fun GradientOverlay(glowOpacity: Float) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.50f to Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.50f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.70f),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF4ADE80).copy(alpha = 0.25f * glowOpacity),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    ),
                ),
        )
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
                    .size(width = 3.dp, height = 400.dp)
                    .offset(y = (-200).dp)
                    .rotate(index * 30f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4ADE80).copy(alpha = 0.12f * alpha), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun SplashParticles(alpha: Float) {
    val specs = remember {
        listOf(
            ParticleSpec(0.11f, 0.10f, 5f, 0),
            ParticleSpec(0.79f, 0.16f, 3f, 1),
            ParticleSpec(0.23f, 0.26f, 6f, 2),
            ParticleSpec(0.64f, 0.36f, 4f, 0),
            ParticleSpec(0.47f, 0.46f, 2f, 1),
            ParticleSpec(0.84f, 0.52f, 5f, 2),
            ParticleSpec(0.18f, 0.62f, 3f, 0),
            ParticleSpec(0.56f, 0.68f, 6f, 1),
            ParticleSpec(0.33f, 0.74f, 4f, 2),
            ParticleSpec(0.73f, 0.82f, 3f, 0),
            ParticleSpec(0.09f, 0.90f, 5f, 1),
            ParticleSpec(0.91f, 0.92f, 4f, 2),
            ParticleSpec(0.37f, 0.12f, 2f, 0),
            ParticleSpec(0.69f, 0.24f, 6f, 1),
            ParticleSpec(0.15f, 0.42f, 3f, 2),
            ParticleSpec(0.88f, 0.44f, 5f, 0),
            ParticleSpec(0.28f, 0.56f, 4f, 1),
            ParticleSpec(0.61f, 0.78f, 2f, 2),
            ParticleSpec(0.43f, 0.88f, 6f, 0),
            ParticleSpec(0.52f, 0.18f, 3f, 1),
        )
    }
    val drift by rememberInfiniteTransition(label = "particles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart),
        label = "particleDrift",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        specs.forEachIndexed { idx, spec ->
            val color = when (spec.colorIndex) {
                0 -> Color(0xFF4ADE80)
                1 -> Color(0xFF22C55E)
                else -> Color.White
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (maxWidth * spec.x + ((idx % 3) - 1).dp * drift).roundToPx(),
                            y = (maxHeight * spec.y - (18.dp * drift)).roundToPx(),
                        )
                    }
                    .size(spec.size.dp)
                    .blur((idx % 3).dp)
                    .background(
                        color.copy(alpha = (0.30f + (idx % 5) * 0.08f) * alpha),
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

private data class ParticleSpec(
    val x: Float,
    val y: Float,
    val size: Float,
    val colorIndex: Int,
)

private fun Modifier.graphicsLayerAlpha(alpha: Float): Modifier =
    this.then(Modifier.graphicsLayer { this.alpha = alpha })
