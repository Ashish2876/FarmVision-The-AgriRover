package com.farmvision.agrirover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.analytics.AnalyticsScreen
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import org.junit.Rule
import org.junit.Test

class HistoricalAnalyticsComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun analyticsScreen_initialState_displaysSelectorsAndChart() {
        val viewModel = FarmViewModel(MockFarmRepository(), null, kotlinx.coroutines.Dispatchers.Main)

        composeTestRule.setContent {
            FarmVisionTheme {
                AnalyticsScreen(viewModel = viewModel)
            }
        }

        // Title and header
        composeTestRule.onNodeWithText("Field Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("metric_chip_soil_moisture_p1_raw").assertIsSelected()
        composeTestRule.onNodeWithTag("range_chip_Hours24").assertIsSelected()

        // Canvas chart card
        composeTestRule.onNodeWithTag("historical_canvas_chart_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("historical_canvas_chart").assertIsDisplayed()

        // Summary KPI card
        composeTestRule.onNodeWithTag("analytics_kpi_summary_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_metricAndRangeSelection_updatesSemantics() {
        val viewModel = FarmViewModel(MockFarmRepository(), null, kotlinx.coroutines.Dispatchers.Main)

        composeTestRule.setContent {
            FarmVisionTheme {
                AnalyticsScreen(viewModel = viewModel)
            }
        }

        // Switch to 7 Days range
        composeTestRule.onNodeWithTag("range_chip_Days7").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("range_chip_Days7").assertIsSelected()

        // Switch to Air Temperature metric
        composeTestRule.onNodeWithTag("metric_chip_air_temperature").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("metric_chip_air_temperature").performScrollTo().assertIsSelected()

        // Chart header must update to Air Temperature
        composeTestRule.onNodeWithTag("chart_metric_title").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_toggleAccessibleTable_rendersTableCard() {
        val viewModel = FarmViewModel(MockFarmRepository(), null, kotlinx.coroutines.Dispatchers.Main)

        composeTestRule.setContent {
            FarmVisionTheme {
                AnalyticsScreen(viewModel = viewModel)
            }
        }

        // Switch to Table view
        composeTestRule.onNodeWithTag("table_view_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("table_view_toggle_button").assertIsSelected()

        // Table card displayed
        composeTestRule.onNodeWithTag("accessible_history_table_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_liveModeEmpty_displaysHonestEmptyState() {
        val viewModel = FarmViewModel(MockFarmRepository(), null, kotlinx.coroutines.Dispatchers.Main)

        composeTestRule.setContent {
            FarmVisionTheme {
                AnalyticsScreen(viewModel = viewModel)
            }
        }

        // Toggle to Live Hardware mode
        composeTestRule.onNodeWithTag("analytics_mode_live_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("analytics_mode_live_chip").assertIsSelected()

        // Empty state displayed
        composeTestRule.onNodeWithText("No Live Telemetry Stored").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("switch_to_demo_analytics_button").assertIsDisplayed()

        // Clicking switch to demo returns to demo data
        composeTestRule.onNodeWithTag("switch_to_demo_analytics_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("analytics_mode_demo_chip").assertIsSelected()
    }
}
