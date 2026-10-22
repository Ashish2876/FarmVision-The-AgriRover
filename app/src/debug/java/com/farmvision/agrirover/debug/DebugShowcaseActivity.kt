package com.farmvision.agrirover.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

/**
 * Debug-only activity for interactive on-device visual inspection of Phase 2 components.
 * Excluded from release builds; does not introduce app navigation destinations.
 */
class DebugShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarmVisionTheme {
                DebugShowcaseScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
