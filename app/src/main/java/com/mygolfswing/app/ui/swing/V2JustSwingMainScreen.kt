package com.mygolfswing.app.ui.swing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.launch

/** 挥杆录制入口（对齐 iOS `JustSwingMainView` 简化：相机/相册导入 → 写入挥杆记录） */
@Composable
fun V2JustSwingMainScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }

    var pendingCameraFile by remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val file = pendingCameraFile
        if (ok && file != null) {
            scope.launch {
                importing = true
                val r = V2VideoFileStore.adoptLocalFile(context, file)
                if (r != null) {
                    V2SwingSessionStore.addClipFromVideoImport(r.absolutePath, r.thumbnailPath, r.durationSec)
                }
                importing = false
                onBack()
            }
        }
    }

    val albumLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            val r = V2VideoFileStore.importVideo(context, uri)
            if (r != null) {
                V2SwingSessionStore.addClipFromVideoImport(r.absolutePath, r.thumbnailPath, r.durationSec)
            }
            importing = false
            onBack()
        }
    }

    fun launchCamera() {
        val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
        pendingCameraFile = file
        cameraLauncher.launch(uri)
    }

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(title = "挥杆录制", onBack = onBack)
        Text(
            if (importing) "正在导入…" else "录制或导入挥杆视频，自动归入今日挥杆记录",
            fontSize = 13.sp,
            color = DSColors.shadowGray,
            modifier = Modifier.padding(DSSpacing.lg),
        )
        SourceBtn("拍视频", Icons.Filled.Videocam, onClick = ::launchCamera)
        SourceBtn("从相册选择", Icons.Filled.Photo, onClick = {
            albumLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
            )
        })
    }
}

@Composable
private fun SourceBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.padding(4.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
    }
}
