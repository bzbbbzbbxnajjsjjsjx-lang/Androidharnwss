package com.vardiya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vardiya.app.ui.VardiyaApp
import com.vardiya.app.ui.theme.VardiyaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VardiyaTheme {
                VardiyaApp()
            }
        }
    }
}
