package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2GolfProfile
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2AIArchiveScreen(onBack: () -> Unit) {
    var profile by remember { mutableStateOf<V2GolfProfile?>(null) }
    LaunchedEffect(Unit) {
        profile = ServiceLocator.golfProfileRepo.getLocal()
            ?: ServiceLocator.golfProfileRepo.pullFromCloud().getOrNull()
    }

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(title = "AI 训练档案", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
        ) {
            ArchiveSection("训练重点", profile?.focusAreas)
            ArchiveSection("当前痛点", profile?.painPoints)
            ArchiveSection("优势", profile?.strengths)
            ArchiveSection("训练目标", profile?.trainingGoals, comingSoon = true)
            ArchiveSection("教练指导", profile?.coachGuidance, comingSoon = true)
        }
    }
}

@Composable
private fun ArchiveSection(title: String, body: String?, comingSoon: Boolean = false) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen, modifier = Modifier.padding(vertical = 8.dp))
    Text(
        when {
            comingSoon -> "敬请期待"
            body.isNullOrBlank() -> "暂无记录"
            else -> body
        },
        fontSize = 14.sp,
        color = DSColors.shadowGray,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}
