package com.farmvision.agrirover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.farmvision.agrirover.ui.app.FarmVisionApp
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

/**
 * Main application entry activity for FarmVision AgriRover.
 * Launches directly into the professional Home Dashboard and bottom navigation shell.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarmVisionTheme {
                FarmVisionApp()
            }
        }
    }
}
