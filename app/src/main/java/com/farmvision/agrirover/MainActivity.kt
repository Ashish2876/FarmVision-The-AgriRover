package com.farmvision.agrirover

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farmvision.agrirover.ui.app.FarmVisionApp
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

/**
 * Main application entry activity for FarmVision AgriRover.
 * Launches directly into the professional Home Dashboard and bottom navigation shell.
 * Handles notification deep links via [onNewIntent] and reactive intent binding.
 */
class MainActivity : ComponentActivity() {
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent
        enableEdgeToEdge()
        setContent {
            FarmVisionTheme {
                FarmVisionApp(intent = currentIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }
}
