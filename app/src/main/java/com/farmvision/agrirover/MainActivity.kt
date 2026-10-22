package com.farmvision.agrirover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.farmvision.agrirover.ui.entry.EntryScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

/**
 * Main application entry activity for FarmVision AgriRover.
 * Bootstraps the Jetpack Compose environment with edge-to-edge support and the branded theme.
 * Preserves the Phase 1 entry screen as the default, uncompromised launch experience.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarmVisionTheme {
                EntryScreen()
            }
        }
    }
}
