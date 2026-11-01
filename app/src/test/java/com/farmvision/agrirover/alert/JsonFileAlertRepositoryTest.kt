package com.farmvision.agrirover.alert

import com.farmvision.agrirover.data.alert.repository.JsonFileAlertRepository
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class JsonFileAlertRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var storageFile: File

    private var virtualTime = 1_000_000_000L
    private val fakeTimeProvider = FakeTimeProvider(virtualTime)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("alert_repo_test").toFile()
        storageFile = File(tempDir, "alerts_store.json")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createAlert(id: String, status: AlertStatus = AlertStatus.Active, createdTime: Long = virtualTime) = AlertItem(
        id = id,
        dedupKey = "env:temp:dht22:Simulated",
        category = AlertCategory.Environment,
        ruleId = "env-temp-high-warning",
        severity = AlertSeverity.Warning,
        title = "High Temperature",
        message = "36.5C",
        sourceSensor = "dht22",
        triggerValue = 36.5f,
        origin = DataOrigin.Simulated,
        status = status,
        createdTimestampEpochMillis = createdTime
    )

    @Test
    fun testSaveAndReloadSurvivesRestart() = runBlocking {
        val repo1 = JsonFileAlertRepository(storageFile, fakeTimeProvider)
        val active = listOf(createAlert("alert-1"))
        val resolved = listOf(createAlert("alert-2", AlertStatus.Resolved))

        val saveRes = repo1.saveAlerts(active, resolved)
        assertTrue(saveRes.isSuccess)
        assertTrue(storageFile.exists())

        // Simulate app process restart by creating a new instance
        val repo2 = JsonFileAlertRepository(storageFile, fakeTimeProvider)
        assertEquals(1, repo2.activeAlertsFlow.value.size)
        assertEquals("alert-1", repo2.activeAlertsFlow.value.first().id)
        assertEquals(1, repo2.resolvedAlertsFlow.value.size)
        assertEquals("alert-2", repo2.resolvedAlertsFlow.value.first().id)
    }

    @Test
    fun testAcknowledgeAlertPersistsUpdate() = runBlocking {
        val repo = JsonFileAlertRepository(storageFile, fakeTimeProvider)
        repo.saveAlerts(listOf(createAlert("alert-1")), emptyList())

        val ackRes = repo.acknowledgeAlert("alert-1")
        assertTrue(ackRes.isSuccess)
        assertTrue(ackRes.getOrThrow())

        val alert = repo.getAlertById("alert-1")
        assertNotNull(alert)
        assertEquals(AlertStatus.Acknowledged, alert?.status)
        assertTrue(alert?.isRead == true)

        // Verify reloaded instance reflects acknowledgement
        val reloaded = JsonFileAlertRepository(storageFile, fakeTimeProvider)
        assertEquals(AlertStatus.Acknowledged, reloaded.activeAlertsFlow.value.first().status)
    }

    @Test
    fun testRetentionPruningRemovesExpiredResolvedRecords() = runBlocking {
        val repo = JsonFileAlertRepository(storageFile, fakeTimeProvider, retentionDays = 7L)

        val freshResolved = createAlert("fresh-1", AlertStatus.Resolved, createdTime = virtualTime - (2L * 24 * 3600 * 1000))
        val expiredResolved = createAlert("old-1", AlertStatus.Resolved, createdTime = virtualTime - (10L * 24 * 3600 * 1000)).copy(
            resolvedTimestampEpochMillis = virtualTime - (10L * 24 * 3600 * 1000)
        )

        repo.saveAlerts(emptyList(), listOf(freshResolved, expiredResolved))

        val reloaded = JsonFileAlertRepository(storageFile, fakeTimeProvider, retentionDays = 7L)
        assertEquals(1, reloaded.resolvedAlertsFlow.value.size)
        assertEquals("fresh-1", reloaded.resolvedAlertsFlow.value.first().id)
    }

    @Test
    fun testCorruptStorageRecoversGracefullyWithoutCrash() = runBlocking {
        // Write corrupt non-JSON content
        storageFile.writeText("Corrupted content {{{{ invalid", Charsets.UTF_8)

        val repo = JsonFileAlertRepository(storageFile, fakeTimeProvider)
        assertTrue(repo.activeAlertsFlow.value.isEmpty())
        assertTrue(repo.resolvedAlertsFlow.value.isEmpty())

        // .bak file should exist
        val backupFile = File(storageFile.parentFile, "${storageFile.name}.bak")
        assertTrue(backupFile.exists())
    }
}
