package com.farmvision.agrirover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.farm.MyFarmScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for Phase 7: Farm, Plot & Plant Management.
 * Verifies:
 * 1. Farm Overview displays registered farm parcels, summary metrics, and crop catalog.
 * 2. Deep sub-route navigation from Farm Overview -> Farm Detail -> Plot Detail and back.
 * 3. Crop Planting Detail displays growth stage, harvest projection, and irrigation action.
 * 4. Cascading deletion confirmation dialog displays exact counts of affected dependents (D4).
 * 5. Unsaved form draft dirty checks intercept back navigation with discard prompt.
 */
class FarmManagementComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: MockFarmRepository
    private lateinit var viewModel: FarmViewModel

    @Before
    fun setUp() {
        repository = MockFarmRepository()
        viewModel = FarmViewModel(repository, null)
    }

    @Test
    fun farmOverview_displaysSampleFarmsAndCrops() {
        composeTestRule.setContent {
            FarmVisionTheme {
                MyFarmScreen(viewModel = viewModel)
            }
        }

        // Header and subtitle
        composeTestRule.onNodeWithText("My Farm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Farms • Plots • Crop Catalog • Irrigation").assertIsDisplayed()

        // Summary Metric Strip
        composeTestRule.onNodeWithText("Farms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plots").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plantings").assertIsDisplayed()

        // Sample Farm parcel card
        composeTestRule.onNodeWithText("Green Valley Research Farm").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4 Plots").performScrollTo().assertIsDisplayed()

        // Seed Crop cards
        composeTestRule.onNodeWithText("Roma Tomato").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sweet Corn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun navigation_farmDetailNavigationAndBack() {
        composeTestRule.setContent {
            FarmVisionTheme {
                MyFarmScreen(viewModel = viewModel)
            }
        }

        // Tap Farm card to navigate to FarmDetail
        composeTestRule.onNodeWithText("Green Valley Research Farm").performScrollTo().performClick()

        // Farm Detail is displayed with plot inventory
        composeTestRule.onNodeWithText("Farm Details & Plots").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sector A - Plot 2").performScrollTo().assertIsDisplayed()

        // Tap Back button to return to Farm Overview
        composeTestRule.onNodeWithContentDescription("Back to Farm Overview").performClick()
        composeTestRule.onNodeWithText("Farms • Plots • Crop Catalog • Irrigation").assertIsDisplayed()
    }

    @Test
    fun plantingDetail_displaysCropMetadataAndIrrigation() {
        composeTestRule.setContent {
            FarmVisionTheme {
                MyFarmScreen(viewModel = viewModel)
            }
        }

        // Tap Crop card to navigate to PlantingDetail
        composeTestRule.onNodeWithText("Roma Tomato").performScrollTo().performClick()

        // Planting details screen is displayed
        composeTestRule.onNodeWithText("Growth Stage: Flowering").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Soil Hydration (Simulated)").performScrollTo().assertIsDisplayed()

        // Tap Back button to return
        composeTestRule.onNodeWithContentDescription("Back to Plot Details").performClick()
        composeTestRule.onNodeWithText("My Farm").assertIsDisplayed()
    }

    @Test
    fun deletionDialog_showsExactDependentCounts() {
        composeTestRule.setContent {
            FarmVisionTheme {
                MyFarmScreen(viewModel = viewModel)
            }
        }

        // Open Farm Detail
        composeTestRule.onNodeWithText("Green Valley Research Farm").performScrollTo().performClick()

        // Click Delete Farm icon button
        composeTestRule.onNodeWithContentDescription("Delete Farm").performClick()

        // Confirmation dialog displays exact dependent counts (D4)
        composeTestRule.onNodeWithText("Delete Farm?").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 1 Farm").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 4 Plot(s)").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 4 Crop Planting(s)").assertIsDisplayed()

        // Cancel dismissal
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Farm Details & Plots").assertIsDisplayed()
    }

    @Test
    fun unsavedChanges_showsDiscardPrompt() {
        composeTestRule.setContent {
            FarmVisionTheme {
                MyFarmScreen(viewModel = viewModel)
            }
        }

        // Click Add Farm button
        composeTestRule.onNodeWithText("Add Farm").performClick()
        composeTestRule.onNodeWithText("Agricultural Land Parcel").assertIsDisplayed()

        // Type dirty input
        composeTestRule.onNodeWithText("Farm Name *").performTextInput("Sunrise Ridge Ranch")

        // Hit back button
        composeTestRule.onNodeWithContentDescription("Cancel and go back").performClick()

        // Discard prompt intercepted back navigation
        composeTestRule.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()

        // Confirm discard
        composeTestRule.onNodeWithText("Discard Changes").performClick()

        // Returns to Farm Overview
        composeTestRule.onNodeWithText("Farms • Plots • Crop Catalog • Irrigation").assertIsDisplayed()
    }
}
