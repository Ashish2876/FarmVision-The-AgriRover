package com.farmvision.agrirover.ui.debug

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.debug.DebugShowcaseActivity

/**
 * Debug implementation: launches the Phase 2 component showcase activity.
 * Bound to the debug source set; excluded from release builds.
 */
@Composable
fun DebugShowcaseLauncher(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FarmVisionPrimaryButton(
        text = "View Phase 2 Component Showcase",
        onClick = {
            val intent = Intent(context, DebugShowcaseActivity::class.java)
            context.startActivity(intent)
        },
        leadingIcon = Icons.Rounded.Palette,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
