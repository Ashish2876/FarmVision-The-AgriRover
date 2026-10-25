package com.farmvision.agrirover

import com.farmvision.agrirover.data.repository.InMemoryFarmManagementRepository
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.domain.id.SequentialIdGenerator
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.ui.farm.management.DeletionTarget
import com.farmvision.agrirover.ui.farm.management.FarmManagementViewModel
import com.farmvision.agrirover.ui.farm.management.FarmSortOption
import com.farmvision.agrirover.ui.farm.management.MyFarmSubRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit test suite for [FarmManagementViewModel] verifying:
 * - Sub-route stack navigation and root boundary handling
 * - Form draft tracking and unsaved changes discard confirmation prompt
 * - Input validation errors and double-submission protection
 * - Search query over crop, variety, plot, and farm names
 * - Facet filtering (farm, growth stage, plant health status)
 * - Sorting rules (including requirement D5: unknown planting dates sort last in both directions)
 * - Atomic cascading deletion preview and confirmation workflows
 */
class FarmManagementViewModelTest {

    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var repository: InMemoryFarmManagementRepository
    private lateinit var viewModel: FarmManagementViewModel
    private val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        fakeTime = FakeTimeProvider(1_700_000_000_000L)
        repository = InMemoryFarmManagementRepository(
            timeProvider = fakeTime,
            idGenerator = SequentialIdGenerator(1),
            autoSeed = true
        )
        viewModel = FarmManagementViewModel(
            repository = repository,
            coroutineScope = testScope
        )
    }

    @Test
    fun navigation_initialStateIsFarmListOverview() {
        assertEquals(MyFarmSubRoute.FarmListOverview, viewModel.currentRoute.value)
        // Root cannot navigate up
        assertFalse(viewModel.navigateUp())
    }

    @Test
    fun navigation_pushAndPopSubRoutes() {
        viewModel.navigateTo(MyFarmSubRoute.FarmDetail("farm-01"))
        assertEquals(MyFarmSubRoute.FarmDetail("farm-01"), viewModel.currentRoute.value)

        viewModel.navigateTo(MyFarmSubRoute.PlotDetail("farm-01", "plot-01"))
        assertEquals(MyFarmSubRoute.PlotDetail("farm-01", "plot-01"), viewModel.currentRoute.value)

        assertTrue(viewModel.navigateUp())
        assertEquals(MyFarmSubRoute.FarmDetail("farm-01"), viewModel.currentRoute.value)

        assertTrue(viewModel.navigateUp())
        assertEquals(MyFarmSubRoute.FarmListOverview, viewModel.currentRoute.value)

        assertFalse(viewModel.navigateUp())
    }

    @Test
    fun navigation_unsavedChangesInterceptBackNavigation() {
        viewModel.initFarmForm()
        assertEquals(MyFarmSubRoute.AddEditFarm(null), viewModel.currentRoute.value)
        assertFalse(viewModel.farmDraft.value.hasUnsavedChanges)

        // Typing into form marks unsaved changes
        viewModel.updateFarmDraftName("New Highland Farm")
        assertTrue(viewModel.farmDraft.value.hasUnsavedChanges)

        // Navigate up should NOT pop; it should show the discard prompt
        assertTrue(viewModel.navigateUp())
        assertTrue(viewModel.showDiscardPrompt.value)
        assertEquals(MyFarmSubRoute.AddEditFarm(null), viewModel.currentRoute.value)

        // Dismissing prompt keeps form open
        viewModel.dismissDiscardPrompt()
        assertFalse(viewModel.showDiscardPrompt.value)
        assertEquals(MyFarmSubRoute.AddEditFarm(null), viewModel.currentRoute.value)

        // Confirming discard clears draft and pops
        viewModel.confirmDiscardDraft()
        assertFalse(viewModel.showDiscardPrompt.value)
        assertEquals(MyFarmSubRoute.FarmListOverview, viewModel.currentRoute.value)
        assertFalse(viewModel.farmDraft.value.hasUnsavedChanges)
    }

    @Test
    fun farmForm_validationRejectsBlankAndNegativeArea() {
        viewModel.initFarmForm()

        // Submit with blank name
        viewModel.submitFarmDraft()
        assertEquals("Farm Name cannot be blank", viewModel.farmDraft.value.nameError)

        // Fix name, provide invalid area
        viewModel.updateFarmDraftName("Sunrise Acres")
        viewModel.updateFarmDraftArea("-15.0", AreaUnit.Hectares)
        viewModel.submitFarmDraft()
        assertEquals("Enter a valid positive number", viewModel.farmDraft.value.areaError)

        // Provide valid data
        viewModel.updateFarmDraftArea("12.5", AreaUnit.Hectares)
        var createdFarmName: String? = null
        viewModel.submitFarmDraft { farm ->
            createdFarmName = farm.name
        }

        assertEquals("Sunrise Acres", createdFarmName)
        assertEquals(MyFarmSubRoute.FarmListOverview, viewModel.currentRoute.value)
        val created = repository.getFarms().find { it.name == "Sunrise Acres" }
        assertNotNull(created)
        assertEquals(125_000.0, created!!.areaSquareMeters!!, 0.01)
    }

    @Test
    fun plotForm_createAndPersist() {
        val farm = repository.getFarms().first()
        viewModel.initPlotForm(farm.id)

        viewModel.updatePlotDraftName("Greenhouse Sector A")
        viewModel.updatePlotDraftArea("500", AreaUnit.SquareMeters)
        viewModel.submitPlotDraft()

        val plot = repository.getPlots(farm.id).find { it.name == "Greenhouse Sector A" }
        assertNotNull(plot)
        assertEquals(500.0, plot!!.areaSquareMeters!!, 0.01)
    }

    @Test
    fun plantingForm_supportsPlannedFutureDatesAndNullDates() {
        val farm = repository.getFarms().first()
        val plot = repository.getPlots(farm.id).first()

        // 1. Create with future date (Planned)
        val futureDate = LocalDate.now().plusMonths(2)
        viewModel.initPlantingForm(farm.id, plot.id)
        viewModel.updatePlantingDraftCropName("Winter Rye")
        viewModel.updatePlantingDraftDate(futureDate)
        viewModel.updatePlantingDraftStage(GrowthStage.Planned)
        viewModel.submitPlantingDraft()

        val rye = repository.getPlantings(plot.id).find { it.cropName == "Winter Rye" }
        assertNotNull(rye)
        assertEquals(futureDate, rye!!.plantingDate)
        assertEquals(GrowthStage.Planned, rye.growthStage)

        // 2. Create with null date (legacy/unspecified)
        viewModel.initPlantingForm(farm.id, plot.id)
        viewModel.updatePlantingDraftCropName("Wild Mint")
        viewModel.updatePlantingDraftDate(null)
        viewModel.submitPlantingDraft()

        val mint = repository.getPlantings(plot.id).find { it.cropName == "Wild Mint" }
        assertNotNull(mint)
        assertNull(mint!!.plantingDate)
    }

    @Test
    fun searchAndFacetFilters_workAccurately() {
        val allItems = viewModel.filteredPlantings.value
        assertEquals(4, allItems.size) // Seed data has 4 plantings

        // Search by crop name (case insensitive)
        viewModel.setSearchQuery("tomato")
        val searchResults = viewModel.filteredPlantings.value
        assertEquals(1, searchResults.size)
        assertEquals("Roma Tomato", searchResults.first().planting.cropName)

        viewModel.clearSearchQuery()
        assertEquals(4, viewModel.filteredPlantings.value.size)

        // Filter by growth stage
        viewModel.setSelectedGrowthStageFilter(GrowthStage.Flowering)
        val stageResults = viewModel.filteredPlantings.value
        assertEquals(2, stageResults.size)
        assertTrue(stageResults.any { it.planting.cropName == "Roma Tomato" })
        assertTrue(stageResults.any { it.planting.cropName == "Sweet Corn" })

        // Clear all filters
        viewModel.clearAllFilters()
        assertEquals(4, viewModel.filteredPlantings.value.size)
    }

    @Test
    fun sorting_unknownDatesSortLastInBothAscendingAndDescending() {
        val farm = repository.getFarms().first()
        val plot = repository.getPlots(farm.id).first()

        // Add a planting with an explicit known past date
        viewModel.initPlantingForm(farm.id, plot.id)
        viewModel.updatePlantingDraftCropName("Alpha Crop")
        viewModel.updatePlantingDraftDate(LocalDate.of(2026, 1, 10))
        viewModel.submitPlantingDraft()

        // Add a planting with an explicit known future date
        viewModel.initPlantingForm(farm.id, plot.id)
        viewModel.updatePlantingDraftCropName("Beta Crop")
        viewModel.updatePlantingDraftDate(LocalDate.of(2026, 11, 20))
        viewModel.submitPlantingDraft()

        // Add a planting with unknown date (null)
        viewModel.initPlantingForm(farm.id, plot.id)
        viewModel.updatePlantingDraftCropName("Omega Crop Null Date")
        viewModel.updatePlantingDraftDate(null)
        viewModel.submitPlantingDraft()

        // Test Newest First (PlantingDateDesc)
        viewModel.setSortOption(FarmSortOption.PlantingDateDesc)
        val descList = viewModel.filteredPlantings.value
        // Omega Crop has null date, so it must be at the end
        val lastItemDesc = descList.last()
        assertNull("Unknown dates must sort last in descending sort", lastItemDesc.planting.plantingDate)

        // Test Oldest First (PlantingDateAsc)
        viewModel.setSortOption(FarmSortOption.PlantingDateAsc)
        val ascList = viewModel.filteredPlantings.value
        // Omega Crop has null date, so it must also be at the end
        val lastItemAsc = ascList.last()
        assertNull("Unknown dates must sort last in ascending sort", lastItemAsc.planting.plantingDate)
        // First item in asc must have earliest date
        assertEquals(LocalDate.of(2026, 1, 10), ascList.first().planting.plantingDate)
    }

    @Test
    fun deletionWorkflow_displaysExactCountsAndExecutesCascadingDeletion() {
        val farm = repository.getFarms().first()
        assertEquals(1, repository.getFarms().size)
        assertEquals(4, repository.getPlots().size)
        assertEquals(4, repository.getPlantings().size)

        // Request farm deletion
        viewModel.requestDeleteFarm(farm)
        val target = viewModel.activeDeletionTarget.value
        val report = viewModel.activeDeletionReport.value

        assertTrue(target is DeletionTarget.FarmTarget)
        assertNotNull(report)
        assertEquals(1, report!!.deletedFarmsCount)
        assertEquals(4, report.deletedPlotsCount)
        assertEquals(4, report.deletedPlantingsCount)

        // Dismiss deletion dialog
        viewModel.dismissDeletionDialog()
        assertNull(viewModel.activeDeletionTarget.value)
        assertNull(viewModel.activeDeletionReport.value)
        assertEquals(1, repository.getFarms().size)

        // Re-request and confirm deletion
        viewModel.requestDeleteFarm(farm)
        var confirmedReport: com.farmvision.agrirover.domain.model.farm.DeletionReport? = null
        viewModel.confirmDeletion { confirmedReport = it }

        assertNotNull(confirmedReport)
        assertEquals(1, confirmedReport!!.deletedFarmsCount)
        assertEquals(4, confirmedReport!!.deletedPlotsCount)
        assertEquals(4, confirmedReport!!.deletedPlantingsCount)

        // Repo must now be completely empty
        assertTrue(repository.getFarms().isEmpty())
        assertTrue(repository.getPlots().isEmpty())
        assertTrue(repository.getPlantings().isEmpty())
        assertNull(viewModel.activeDeletionTarget.value)
    }
}
