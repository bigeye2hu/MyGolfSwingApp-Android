package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mygolfswing.app.R

internal val CoachPaper = Color(0xFFFBFAF5)
internal val CoachGreen = Color(0xFF226B53)
internal val CoachInk = Color(0xFF173C30)
internal val CoachBody = Color(0xFF46594F)
internal val CoachMeta = Color(0xFF7D8982)
internal val CoachLine = Color(0xFFE1E8DF)
internal val CoachSendIdle = Color(0xFFD7E6DE)
internal val CoachSendIdleText = Color(0xFF5F7568)

internal enum class CoachCourseHeader {
    STUDENTS,
    SPACE,
    DETAIL,
    COMMENT,
}

@Composable
internal fun CoachCourseHeaderImage(
    kind: CoachCourseHeader,
    modifier: Modifier = Modifier,
) {
    val res = when (kind) {
        CoachCourseHeader.STUDENTS -> R.drawable.coach_header_students
        CoachCourseHeader.SPACE -> R.drawable.space_bg
        CoachCourseHeader.DETAIL -> R.drawable.coach_header_detail
        CoachCourseHeader.COMMENT -> R.drawable.comment_bg
    }
    val fadePhoto = kind == CoachCourseHeader.SPACE || kind == CoachCourseHeader.COMMENT
    Box(modifier) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        if (fadePhoto) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.00f to CoachPaper.copy(alpha = 0.20f),
                        0.42f to Color.Transparent,
                        0.78f to CoachPaper.copy(alpha = 0.55f),
                        1.00f to CoachPaper,
                    ),
                ),
            )
        }
    }
}
