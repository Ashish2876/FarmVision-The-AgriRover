package com.farmvision.agrirover

import com.farmvision.agrirover.data.analytics.DeterministicDemoHistoryGenerator
import com.farmvision.agrirover.data.model.AnalyticsTimeRange
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.data.repository.InMemoryTelemetryHistoryRepository
import com.farmvision.agrirover.data.repository.JsonFileTelemetryHistoryRepository
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TelemetryHistoryRepositoryContractTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fixedNow = 1_700_000_000_000L
    private val timeProvider = FakeTimeProvider(fixedNow)

    private fun runContractTests(createRepo: (writeIntervalSec: Long) -> TelemetryHistoryRepository) = runBlocking {
        val repo = createRepo(60L)

        // 1. Store samples with rate-limiting check
        val s1 = TelemetrySample(
            id = "s-1",
            metric = HistoricalMetric.SoilMoistureProbe1Raw,
            canonicalValue = 2100f,
            rawValue = 2100f,
            timestampEpochMillis = fixedNow - 100_000L,
            origin = DataOrigin.LiveHardware
        )
        val stored1 = repo.recordSample(s1)
        assertTrue("First sample must be stored", stored1)

        // Sub-60s sample must be throttled
        val s2TooSoon = TelemetrySample(
            id = "s-2-soon",
            metric = HistoricalMetric.SoilMoistureProbe1Raw,
            canonicalValue = 2105f,
            rawValue = 2105f,
            timestampEpochMillis = fixedNow - 90_000L, // 10s later (< 60s)
            origin = DataOrigin.LiveHardware
        )
        val stored2 = repo.recordSample(s2TooSoon)
        assertFalse("Sub-60s sample must be rate-throttled", stored2)

        // Sample after 60s must be accepted
        val s3ValidInterval = TelemetrySample(
            id = "s-3-ok",
            metric = HistoricalMetric.SoilMoistureProbe1Raw,
            canonicalValue = 2110f,
            rawValue = 2110f,
            timestampEpochMillis = fixedNow - 30_000L, // 70s later (> 60s)
            origin = DataOrigin.LiveHardware
        )
        val stored3 = repo.recordSample(s3ValidInterval)
        assertTrue("Sample after >= 60s must be accepted", stored3)

        // 2. Range query
        val window = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)
        val samples = repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window)
        assertEquals(2, samples.size)
        assertEquals("s-1", samples[0].id)
        assertEquals("s-3-ok", samples[1].id)
        // Ordering check
        assertTrue(samples[0].timestampEpochMillis < samples[1].timestampEpochMillis)

        // 3. Provenance filtering
        val demoSample = TelemetrySample(
            id = "demo-s",
            metric = HistoricalMetric.SoilMoistureProbe1Raw,
            canonicalValue = 1950f,
            rawValue = 1950f,
            timestampEpochMillis = fixedNow - 10_000L,
            origin = DataOrigin.Simulated
        )
        repo.recordSample(demoSample)

        val liveOnly = repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window, DataOrigin.LiveHardware)
        assertEquals(2, liveOnly.size)
        assertTrue(liveOnly.all { it.origin == DataOrigin.LiveHardware })

        val demoOnly = repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window, DataOrigin.Simulated)
        assertEquals(1, demoOnly.size)
        assertEquals(DataOrigin.Simulated, demoOnly[0].origin)

        // 4. Reverse chronological recent records & pagination
        val recentAll = repo.getRecentRecords(limit = 2, offset = 0)
        assertEquals(2, recentAll.size)
        assertEquals("demo-s", recentAll[0].id) // Most recent
        assertEquals("s-3-ok", recentAll[1].id)

        val recentPage2 = repo.getRecentRecords(limit = 2, offset = 2)
        assertEquals(1, recentPage2.size)
        assertEquals("s-1", recentPage2[0].id)

        // 5. Pruning old records
        val pruned = repo.pruneOldRecords(retentionCutoffMillis = fixedNow - 50_000L)
        assertEquals(1, pruned) // s-1 pruned
        val remaining = repo.getSamples(HistoricalMetric.SoilMoistureProbe1Raw, window)
        assertEquals(2, remaining.size) // s-3-ok and demo-s remain

        // 6. Clear history
        repo.clearHistory()
        assertEquals(0, repo.getCount())
    }

    @Test
    fun testInMemoryRepositoryContract() {
        runContractTests { writeIntervalSec ->
            InMemoryTelemetryHistoryRepository(maxSamplesPerMetric = 100, writeIntervalSeconds = writeIntervalSec)
        }
    }

    @Test
    fun testJsonFileRepositoryContract() {
        val file = File(tempFolder.root, "contract_test_telemetry.json")
        runContractTests { writeIntervalSec ->
            JsonFileTelemetryHistoryRepository(
                storageFile = file,
                timeProvider = timeProvider,
                writeIntervalSeconds = writeIntervalSec,
                retentionDays = 30
            )
        }
    }

    @Test
    fun testJsonFileRepository_restartPersistence() = runBlocking {
        val file = File(tempFolder.root, "restart_persistence_test.json")
        val repo1 = JsonFileTelemetryHistoryRepository(
            storageFile = file,
            timeProvider = timeProvider,
            writeIntervalSeconds = 0L // Disable throttle for quick population
        )

        val sampleA = TelemetrySample(
            id = "persist-A",
            metric = HistoricalMetric.AirTemperature,
            canonicalValue = 26.5f,
            rawValue = null,
            timestampEpochMillis = fixedNow - 20000L,
            origin = DataOrigin.LiveHardware
        )
        val sampleB = TelemetrySample(
            id = "persist-B",
            metric = HistoricalMetric.SoilMoistureProbe2Raw,
            canonicalValue = 1850f,
            rawValue = 1850f,
            timestampEpochMillis = fixedNow - 10000L,
            origin = DataOrigin.LiveHardware
        )
        repo1.recordSample(sampleA)
        repo1.recordSample(sampleB)

        assertEquals(2, repo1.getCount())

        // Simulate app death and restart: new repository instance on the same file
        val repo2 = JsonFileTelemetryHistoryRepository(
            storageFile = file,
            timeProvider = timeProvider,
            writeIntervalSeconds = 0L
        )

        assertEquals(2, repo2.getCount())
        val window = AnalyticsTimeRange.Hours24.calculateWindow(fixedNow)
        val tempSamples = repo2.getSamples(HistoricalMetric.AirTemperature, window)
        assertEquals(1, tempSamples.size)
        assertEquals("persist-A", tempSamples[0].id)
        assertEquals(26.5f, tempSamples[0].canonicalValue, 0.001f)

        val probe2Samples = repo2.getSamples(HistoricalMetric.SoilMoistureProbe2Raw, window)
        assertEquals(1, probe2Samples.size)
        assertEquals("persist-B", probe2Samples[0].id)
        assertEquals(1850f, probe2Samples[0].canonicalValue, 0.001f)
    }

    @Test
    fun testDeterministicDemoHistoryGenerator_repeatability() {
        val demo1 = DeterministicDemoHistoryGenerator.generateDemoHistory(fixedNow, durationDays = 30, stepMinutes = 60)
        val demo2 = DeterministicDemoHistoryGenerator.generateDemoHistory(fixedNow, durationDays = 30, stepMinutes = 60)

        assertEquals(demo1.size, demo2.size)
        for (i in demo1.indices) {
            val s1 = demo1[i]
            val s2 = demo2[i]
            assertEquals(s1.id, s2.id)
            assertEquals(s1.metric, s2.metric)
            assertEquals(s1.canonicalValue, s2.canonicalValue, 0.0001f)
            assertEquals(s1.timestampEpochMillis, s2.timestampEpochMillis)
            assertEquals(DataOrigin.Simulated, s1.origin)
        }
        assertTrue("Must generate samples across all 6 supported metrics", demo1.map { it.metric }.toSet().size == 6)
    }
}
