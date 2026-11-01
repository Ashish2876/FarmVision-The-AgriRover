package com.farmvision.agrirover.data.assistant.repository

import com.farmvision.agrirover.data.assistant.local.AssistantJsonSerializer
import com.farmvision.agrirover.data.assistant.local.AssistantStorePayload
import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import com.farmvision.agrirover.domain.assistant.repository.AssistantRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Thread-safe, atomic file-backed implementation of [AssistantRepository].
 * Enforces storage limits (max 20 conversations, max 50 messages per conversation).
 */
class JsonFileAssistantRepository(
    private val storageFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AssistantRepository {

    companion object {
        const val MAX_CONVERSATIONS = 20
        const val MAX_MESSAGES_PER_CONVERSATION = 50
    }

    private val mutex = Mutex()
    private val _conversationsFlow = MutableStateFlow<List<AssistantConversation>>(emptyList())
    private val _consentPreferencesFlow = MutableStateFlow(AssistantConsentPreferences())
    private val _cropProfileFlow = MutableStateFlow(CropProfile())

    init {
        // Load initial state synchronously on init thread (or launch on IO)
        loadFromFileSync()
    }

    private fun loadFromFileSync() {
        if (!storageFile.exists()) return
        try {
            val content = storageFile.readText()
            val payload = AssistantJsonSerializer.parseStorePayload(content)
            _conversationsFlow.value = payload.conversations
            _consentPreferencesFlow.value = payload.consentPreferences
            _cropProfileFlow.value = payload.cropProfile
        } catch (_: Exception) {
            // Keep defaults on read corruption
        }
    }

    private fun persistPayloadLocked() {
        val payload = AssistantStorePayload(
            schemaVersion = AssistantJsonSerializer.CURRENT_SCHEMA_VERSION,
            lastSavedEpochMillis = System.currentTimeMillis(),
            consentPreferences = _consentPreferencesFlow.value,
            cropProfile = _cropProfileFlow.value,
            conversations = _conversationsFlow.value
        )
        val json = AssistantJsonSerializer.serializeStorePayload(payload)

        val parentDir = storageFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        tempFile.writeText(json)
        if (!tempFile.renameTo(storageFile)) {
            // Fallback copy if atomic rename fails on some file systems
            tempFile.copyTo(storageFile, overwrite = true)
            tempFile.delete()
        }
    }

    override fun getConversationsFlow(): Flow<List<AssistantConversation>> =
        _conversationsFlow.asStateFlow()

    override fun getConversationFlow(conversationId: String): Flow<AssistantConversation?> =
        _conversationsFlow.map { list -> list.firstOrNull { it.id == conversationId } }

    override fun getConsentPreferencesFlow(): Flow<AssistantConsentPreferences> =
        _consentPreferencesFlow.asStateFlow()

    override fun getCropProfileFlow(): Flow<CropProfile> =
        _cropProfileFlow.asStateFlow()

    override suspend fun getConversations(): List<AssistantConversation> = mutex.withLock {
        _conversationsFlow.value
    }

    override suspend fun getConversation(conversationId: String): AssistantConversation? = mutex.withLock {
        _conversationsFlow.value.firstOrNull { it.id == conversationId }
    }

    override suspend fun createConversation(title: String): AssistantConversation = withContext(ioDispatcher) {
        mutex.withLock {
            val newConv = AssistantConversation(
                title = title.ifBlank { "New Conversation" },
                createdAtMillis = System.currentTimeMillis()
            )
            val currentList = _conversationsFlow.value.toMutableList()
            currentList.add(0, newConv)

            // Enforce max 20 conversations limit
            if (currentList.size > MAX_CONVERSATIONS) {
                currentList.subList(MAX_CONVERSATIONS, currentList.size).clear()
            }

            _conversationsFlow.value = currentList
            persistPayloadLocked()
            newConv
        }
    }

    override suspend fun addMessage(
        conversationId: String,
        message: AssistantMessage
    ): AssistantConversation = withContext(ioDispatcher) {
        mutex.withLock {
            val currentList = _conversationsFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == conversationId }
            val existing = if (index != -1) currentList[index] else {
                AssistantConversation(id = conversationId, title = "New Conversation")
            }

            val updatedMessages = existing.messages.toMutableList()
            updatedMessages.add(message)

            // Enforce max 50 messages limit per conversation (retain newest)
            if (updatedMessages.size > MAX_MESSAGES_PER_CONVERSATION) {
                val excess = updatedMessages.size - MAX_MESSAGES_PER_CONVERSATION
                updatedMessages.subList(0, excess).clear()
            }

            val updatedConv = existing.copy(
                updatedAtMillis = System.currentTimeMillis(),
                messages = updatedMessages
            )

            if (index != -1) {
                currentList.removeAt(index)
            }
            currentList.add(0, updatedConv) // Move to top as most recently updated

            // Enforce max 20 conversations
            if (currentList.size > MAX_CONVERSATIONS) {
                currentList.subList(MAX_CONVERSATIONS, currentList.size).clear()
            }

            _conversationsFlow.value = currentList
            persistPayloadLocked()
            updatedConv
        }
    }

    override suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus,
        errorMessage: String?
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentList = _conversationsFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == conversationId }
            if (index != -1) {
                val conv = currentList[index]
                val msgIndex = conv.messages.indexOfFirst { it.id == messageId }
                if (msgIndex != -1) {
                    val updatedMsgs = conv.messages.toMutableList()
                    val targetMsg = updatedMsgs[msgIndex]
                    updatedMsgs[msgIndex] = targetMsg.copy(
                        status = status,
                        errorMessage = errorMessage
                    )
                    currentList[index] = conv.copy(
                        updatedAtMillis = System.currentTimeMillis(),
                        messages = updatedMsgs
                    )
                    _conversationsFlow.value = currentList
                    persistPayloadLocked()
                }
            }
        }
    }

    override suspend fun deleteConversation(conversationId: String): Boolean = withContext(ioDispatcher) {
        mutex.withLock {
            val currentList = _conversationsFlow.value.toMutableList()
            val removed = currentList.removeAll { it.id == conversationId }
            if (removed) {
                _conversationsFlow.value = currentList
                persistPayloadLocked()
            }
            removed
        }
    }

    override suspend fun clearAllConversations() = withContext(ioDispatcher) {
        mutex.withLock {
            _conversationsFlow.value = emptyList()
            persistPayloadLocked()
        }
    }

    override suspend fun updateConsentPreferences(
        preferences: AssistantConsentPreferences
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            _consentPreferencesFlow.value = preferences
            persistPayloadLocked()
        }
    }

    override suspend fun updateCropProfile(
        profile: CropProfile
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            _cropProfileFlow.value = profile
            persistPayloadLocked()
        }
    }
}

