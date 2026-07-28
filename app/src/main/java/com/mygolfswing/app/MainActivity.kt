package com.mygolfswing.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.mygolfswing.app.ui.tabs.V2RootScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme

class MainActivity : ComponentActivity() {
    private val sharedVideoText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedVideoText.value = extractSharedText(intent)
        enableEdgeToEdge()
        setContent {
            MyGolfSwingAppAndroidTheme {
                V2RootScreen(
                    sharedVideoUrl = sharedVideoText.value,
                    onSharedVideoUrlConsumed = { sharedVideoText.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedVideoText.value = extractSharedText(intent)
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (!intent.type.orEmpty().startsWith("text/")) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { Regex("(https?://[^\\s\\u4e00-\\u9fa5]+)").find(it)?.value }
            ?.takeIf { it.isNotBlank() }
    }
}
