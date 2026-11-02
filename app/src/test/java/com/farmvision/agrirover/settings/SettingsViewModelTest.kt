package com.farmvision.agrirover.settings

import android.content.SharedPreferences
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.model.AppLanguage
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.SpeedUnit
import com.farmvision.agrirover.data.model.TemperatureUnit
import com.farmvision.agrirover.data.model.ThemeMode
import com.farmvision.agrirover.data.network.HttpResponse
import com.farmvision.agrirover.data.network.RoverHttpClient
import com.farmvision.agrirover.data.network.RoverNetworkException
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import com.farmvision.agrirover.domain.assistant.repository.AssistantRepository
import com.farmvision.agrirover.ui.settings.DiagnosticsState
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    // --- ENUM & DOMAIN MODEL TESTS ---

    @Test
    fun themeMode_fromString_handlesValidAndFallbackValues() {
        assertEquals(ThemeMode.Light, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromString("dark"))
        assertEquals(ThemeMode.System, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.System, ThemeMode.fromString("unknown"))
        assertEquals(ThemeMode.System, ThemeMode.fromString(null))
    }

    @Test
    fun appLanguage_fromCode_defaultsToEnglish() {
        assertEquals(AppLanguage.English, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.English, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.English, AppLanguage.fromCode(null))
    }

    @Test
    fun farmSettings_defaultValues_areCorrect() {
        val defaultSettings = FarmSettings()
        assertEquals(TemperatureUnit.Celsius, defaultSettings.temperatureUnit)
        assertEquals(SpeedUnit.Metric, defaultSettings.speedUnit)
        assertEquals("Wi-Fi SoftAP", defaultSettings.transportProtocol)
        assertTrue(defaultSettings.isSimulatedEngineActive)
        assertEquals(ThemeMode.System, defaultSettings.themeMode)
        assertEquals(AppLanguage.English, defaultSettings.language)
        assertEquals("AgriRover Alpha", defaultSettings.roverName)
        assertEquals("http://10.0.2.2:8000", defaultSettings.backendUrl)
        assertTrue(defaultSettings.isNotificationSoundEnabled)
        assertTrue(defaultSettings.isNotificationVibrationEnabled)
    }

    // --- PREFERENCES MANAGER PERSISTENCE TESTS ---

    @Test
    fun settingsPreferencesManager_updatesAndPersistsAllFields() {
        val fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        val manager = SettingsPreferencesManager(fakeContext)

        // Initial defaults
        val initial = manager.settingsFlow.value
        assertEquals(ThemeMode.System, initial.themeMode)

        // Update theme
        manager.updateThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, manager.settingsFlow.value.themeMode)
        assertEquals("Dark", fakePrefs.getString("key_theme_mode", null))

        // Update units
        manager.updateTemperatureUnit(TemperatureUnit.Fahrenheit)
        assertEquals(TemperatureUnit.Fahrenheit, manager.settingsFlow.value.temperatureUnit)

        manager.updateSpeedUnit(SpeedUnit.Imperial)
        assertEquals(SpeedUnit.Imperial, manager.settingsFlow.value.speedUnit)

        // Update rover name and backend url (with whitespace sanitization)
        manager.updateRoverName("  AgriRover Beta  ")
        assertEquals("AgriRover Beta", manager.settingsFlow.value.roverName)

        manager.updateRoverName("   ")
        assertEquals("AgriRover Alpha", manager.settingsFlow.value.roverName) // Fallback on blank

        manager.updateBackendUrl("http://192.168.1.100:8000")
        assertEquals("http://192.168.1.100:8000", manager.settingsFlow.value.backendUrl)

        // Notifications
        manager.updateNotificationSound(false)
        assertFalse(manager.settingsFlow.value.isNotificationSoundEnabled)

        manager.updateNotificationVibration(false)
        assertFalse(manager.settingsFlow.value.isNotificationVibrationEnabled)

        // Reset
        manager.resetPreferences()
        val reset = manager.settingsFlow.value
        assertEquals(ThemeMode.System, reset.themeMode)
        assertEquals(TemperatureUnit.Celsius, reset.temperatureUnit)
        assertEquals(SpeedUnit.Metric, reset.speedUnit)
        assertTrue(reset.isNotificationSoundEnabled)
        assertTrue(reset.isNotificationVibrationEnabled)
    }

    // --- SETTINGS VIEWMODEL FUNCTIONAL TESTS ---

    @Test
    fun settingsViewModel_runsDiagnostics_handlesSuccess() = runBlocking {
        val fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        val manager = SettingsPreferencesManager(fakeContext)

        val mockHttpClient = object : RoverHttpClient {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
                return HttpResponse(statusCode = 200, body = "{\"status\":\"ok\"}", latencyMs = 25L)
            }
            override suspend fun post(url: String, body: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
                return HttpResponse(statusCode = 200, body = "{}", latencyMs = 25L)
            }
        }

        val viewModel = SettingsViewModel(
            preferencesManager = manager,
            httpClient = mockHttpClient,
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = testScope
        )

        assertEquals(DiagnosticsState.Idle, viewModel.diagnosticsState.value)

        viewModel.testBackendConnection("http://10.0.2.2:8000")

        val state = viewModel.diagnosticsState.value
        assertTrue(state is DiagnosticsState.Success)
        val success = state as DiagnosticsState.Success
        assertTrue(success.message.contains("HTTP 200"))

        viewModel.clearDiagnostics()
        assertEquals(DiagnosticsState.Idle, viewModel.diagnosticsState.value)
    }

    @Test
    fun settingsViewModel_runsDiagnostics_handlesNetworkFailure() = runBlocking {
        val fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        val manager = SettingsPreferencesManager(fakeContext)

        val mockHttpClient = object : RoverHttpClient {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
                throw RoverNetworkException("Connection refused")
            }
            override suspend fun post(url: String, body: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse {
                throw RoverNetworkException("Connection refused")
            }
        }

        val viewModel = SettingsViewModel(
            preferencesManager = manager,
            httpClient = mockHttpClient,
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = testScope
        )

        viewModel.testBackendConnection("http://10.0.2.2:8000")

        val state = viewModel.diagnosticsState.value
        assertTrue(state is DiagnosticsState.Failure)
        val failure = state as DiagnosticsState.Failure
        assertTrue(failure.message.contains("Connection refused"))
    }

    @Test
    fun settingsViewModel_privacyConsent_updatesAssistantRepo() = runBlocking {
        val fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        val manager = SettingsPreferencesManager(fakeContext)

        val fakeAssistantRepo = FakeAssistantRepository()

        val viewModel = SettingsViewModel(
            preferencesManager = manager,
            assistantRepository = fakeAssistantRepo,
            coroutineScope = testScope
        )

        assertFalse(viewModel.consentPreferences.value.consentGranted)

        viewModel.toggleConsentGranted(true)
        assertTrue(fakeAssistantRepo.consentPreferences.value.consentGranted)

        viewModel.toggleShareLiveTelemetry(true)
        assertTrue(fakeAssistantRepo.consentPreferences.value.shareLiveTelemetry)

        viewModel.toggleShareAlerts(true)
        assertTrue(fakeAssistantRepo.consentPreferences.value.shareAlerts)

        viewModel.toggleShareHistoricalAnalytics(true)
        assertTrue(fakeAssistantRepo.consentPreferences.value.shareHistoricalAnalytics)

        viewModel.toggleShareCropProfile(true)
        assertTrue(fakeAssistantRepo.consentPreferences.value.shareCropProfile)
    }

    @Test
    fun settingsViewModel_clearAiConversations_callsRepo() = runBlocking {
        val fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        val manager = SettingsPreferencesManager(fakeContext)

        val fakeAssistantRepo = FakeAssistantRepository()
        fakeAssistantRepo.conversationsList.add(
            AssistantConversation(id = "conv-1", title = "Test Conversation")
        )

        val viewModel = SettingsViewModel(
            preferencesManager = manager,
            assistantRepository = fakeAssistantRepo,
            coroutineScope = testScope
        )

        assertEquals(1, fakeAssistantRepo.conversationsList.size)

        viewModel.clearAiConversations(fakeContext)

        assertEquals(0, fakeAssistantRepo.conversationsList.size)
    }
}

// --- FAKE IN-MEMORY TEST DOUBLES FOR JVM ---

private class FakeContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    override fun getFilesDir(): File = File(System.getProperty("java.io.tmpdir"), "test_farmvision_files")
    override fun getCacheDir(): File = File(System.getProperty("java.io.tmpdir"), "test_farmvision_cache")
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = (values[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (values[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (values[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (values[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(values)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            key?.let { temp[it] = value }
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            key?.let { temp[it] = values }
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { temp[it] = value }
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { temp[it] = value }
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { temp[it] = value }
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { temp[it] = value }
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { temp[it] = null }
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearAll) {
                map.clear()
            }
            temp.forEach { (k, v) ->
                if (v == null) map.remove(k) else map[k] = v
            }
        }
    }
}

private class FakeAssistantRepository : AssistantRepository {
    val consentPreferences = MutableStateFlow(AssistantConsentPreferences())
    val cropProfile = MutableStateFlow(CropProfile())
    val conversationsList = mutableListOf<AssistantConversation>()

    override fun getConversationsFlow(): Flow<List<AssistantConversation>> = MutableStateFlow(conversationsList)
    override fun getConversationFlow(conversationId: String): Flow<AssistantConversation?> =
        MutableStateFlow(conversationsList.find { it.id == conversationId })
    override fun getConsentPreferencesFlow(): Flow<AssistantConsentPreferences> = consentPreferences.asStateFlow()
    override fun getCropProfileFlow(): Flow<CropProfile> = cropProfile.asStateFlow()

    override suspend fun getConversations(): List<AssistantConversation> = conversationsList
    override suspend fun getConversation(conversationId: String): AssistantConversation? =
        conversationsList.find { it.id == conversationId }
    override suspend fun createConversation(title: String): AssistantConversation {
        val conv = AssistantConversation(id = "conv-${System.currentTimeMillis()}", title = title)
        conversationsList.add(conv)
        return conv
    }
    override suspend fun addMessage(conversationId: String, message: AssistantMessage): AssistantConversation {
        val existing = conversationsList.find { it.id == conversationId }
            ?: AssistantConversation(id = conversationId, title = "New")
        val updated = existing.copy(messages = existing.messages + message)
        conversationsList.removeAll { it.id == conversationId }
        conversationsList.add(updated)
        return updated
    }
    override suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus,
        errorMessage: String?
    ) {
        // No-op for fake test double
    }
    override suspend fun deleteConversation(conversationId: String): Boolean =
        conversationsList.removeIf { it.id == conversationId }
    override suspend fun clearAllConversations() {
        conversationsList.clear()
    }
    override suspend fun updateConsentPreferences(preferences: AssistantConsentPreferences) {
        consentPreferences.value = preferences
    }
    override suspend fun updateCropProfile(profile: CropProfile) {
        cropProfile.value = profile
    }
}
