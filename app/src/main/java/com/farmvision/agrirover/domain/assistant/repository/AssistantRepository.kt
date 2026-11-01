package com.farmvision.agrirover.domain.assistant.repository

import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing conversations, privacy consent, and crop profiles.
 */
interface AssistantRepository {
    fun getConversationsFlow(): Flow<List<AssistantConversation>>
    fun getConversationFlow(conversationId: String): Flow<AssistantConversation?>
    fun getConsentPreferencesFlow(): Flow<AssistantConsentPreferences>
    fun getCropProfileFlow(): Flow<CropProfile>

    suspend fun getConversations(): List<AssistantConversation>
    suspend fun getConversation(conversationId: String): AssistantConversation?
    suspend fun createConversation(title: String): AssistantConversation
    suspend fun addMessage(conversationId: String, message: AssistantMessage): AssistantConversation
    suspend fun updateMessageStatus(conversationId: String, messageId: String, status: MessageStatus, errorMessage: String? = null)
    suspend fun deleteConversation(conversationId: String): Boolean
    suspend fun clearAllConversations()
    suspend fun updateConsentPreferences(preferences: AssistantConsentPreferences)
    suspend fun updateCropProfile(profile: CropProfile)
}

