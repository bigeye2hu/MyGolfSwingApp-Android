package com.mygolfswing.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mygolfswing.app.ui.tabs.V2RootScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyGolfSwingAppAndroidTheme {
                V2RootScreen()
            }
        }
    }
}
