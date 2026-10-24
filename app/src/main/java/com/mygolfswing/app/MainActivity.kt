package com.mygolfswing.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.mygolfswing.app.ui.tabs.V2RootScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import com.mygolfswing.app.data.coach.V2PendingCoachInvite
import com.mygolfswing.app.data.coach.parseCoachInviteLink

class MainActivity : ComponentActivity() {
    private val sharedVideoText = mutableStateOf<String?>(null)
    private val pendingCoachInvite = mutableStateOf<V2PendingCoachInvite?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedVideoText.value = extractSharedText(intent)
        pendingCoachInvite.value = extractCoachInvite(intent)
        val lightScrim = Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(lightScrim, lightScrim),
            navigationBarStyle = SystemBarStyle.light(lightScrim, lightScrim),
        )
        setContent {
            MyGolfSwingAppAndroidTheme {
                V2RootScreen(
                    sharedVideoUrl = sharedVideoText.value,
                    onSharedVideoUrlConsumed = { sharedVideoText.value = null },
                    pendingCoachInvite = pendingCoachInvite.value,
                    onCoachInviteConsumed = { pendingCoachInvite.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedVideoText.value = extractSharedText(intent)
        pendingCoachInvite.value = extractCoachInvite(intent)
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (!intent.type.orEmpty().startsWith("text/")) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { Regex("(https?://[^\\s\\u4e00-\\u9fa5]+)").find(it)?.value }
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractCoachInvite(intent: Intent?): V2PendingCoachInvite? {
        return parseCoachInviteLink(intent?.dataString)
    }
}
