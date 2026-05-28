package com.mygolfswing.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2SplashScreen(message: String = "正在恢复会话…") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("挥呗", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text(message, fontSize = 13.sp, color = DSColors.shadowGray, modifier = Modifier.padding(top = 8.dp))
            CircularProgressIndicator(
                color = DSColors.fairway,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
