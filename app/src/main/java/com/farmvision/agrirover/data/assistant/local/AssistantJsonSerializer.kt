package com.farmvision.agrirover.data.assistant.local

import com.farmvision.agrirover.domain.assistant.model.AssistantConsentPreferences
import com.farmvision.agrirover.domain.assistant.model.AssistantContext
import com.farmvision.agrirover.domain.assistant.model.AssistantConversation
import com.farmvision.agrirover.domain.assistant.model.AssistantMessage
import com.farmvision.agrirover.domain.assistant.model.CropProfile
import com.farmvision.agrirover.domain.assistant.model.InformationClass
import com.farmvision.agrirover.domain.assistant.model.InformationProvenance
import com.farmvision.agrirover.domain.assistant.model.MessageSender
import com.farmvision.agrirover.domain.assistant.model.MessageStatus

/**
 * Persistence envelope for private internal file storage.
 */
data class AssistantStorePayload(
    val schemaVersion: Int = 1,
    val lastSavedEpochMillis: Long = System.currentTimeMillis(),
    val consentPreferences: AssistantConsentPreferences = AssistantConsentPreferences(),
    val cropProfile: CropProfile = CropProfile(),
    val conversations: List<AssistantConversation> = emptyList()
)

/**
 * Framework-independent JSON serializer and deserializer for Assistant models.
 * Completely immune to Android framework unmocked exceptions on JVM unit test runners.
 */
object AssistantJsonSerializer {

    const val CURRENT_SCHEMA_VERSION = 1

    fun escape(string: String?): String {
        if (string == null) return "null"
        val sb = StringBuilder("\"")
        for (c in string) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0..0x1f) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun unescape(raw: String): String {
        return raw.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    // =========================================================================
    // STORE PAYLOAD SERIALIZATION & PARSING
    // =========================================================================

    fun serializeStorePayload(payload: AssistantStorePayload): String {
        val sb = StringBuilder("{\n")
        sb.append("  \"schemaVersion\": ").append(payload.schemaVersion).append(",\n")
        sb.append("  \"lastSavedEpochMillis\": ").append(payload.lastSavedEpochMillis).append(",\n")

        // Consent
        val cp = payload.consentPreferences
        sb.append("  \"consentPreferences\": {")
        sb.append("\"consentGranted\": ").append(cp.consentGranted).append(", ")
        sb.append("\"shareLiveTelemetry\": ").append(cp.shareLiveTelemetry).append(", ")
        sb.append("\"shareAlerts\": ").append(cp.shareAlerts).append(", ")
        sb.append("\"shareHistoricalAnalytics\": ").append(cp.shareHistoricalAnalytics).append(", ")
        sb.append("\"shareCropProfile\": ").append(cp.shareCropProfile).append("},\n")

        // Crop profile
        val cr = payload.cropProfile
        sb.append("  \"cropProfile\": {")
        sb.append("\"cropType\": ").append(escape(cr.cropType)).append(", ")
        sb.append("\"growthStage\": ").append(escape(cr.growthStage)).append(", ")
        sb.append("\"soilType\": ").append(escape(cr.soilType)).append(", ")
        sb.append("\"plantingDate\": ").append(escape(cr.plantingDate)).append("},\n")

        // Conversations array
        sb.append("  \"conversations\": [\n")
        val convs = payload.conversations
        for (i in convs.indices) {
            sb.append("    ").append(serializeConversation(convs[i]))
            if (i < convs.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    private fun serializeConversation(conv: AssistantConversation): String {
        val sb = StringBuilder("{")
        sb.append("\"id\": ").append(escape(conv.id)).append(", ")
        sb.append("\"title\": ").append(escape(conv.title)).append(", ")
        sb.append("\"createdAtMillis\": ").append(conv.createdAtMillis).append(", ")
        sb.append("\"updatedAtMillis\": ").append(conv.updatedAtMillis).append(", ")
        sb.append("\"messages\": [")
        for (j in conv.messages.indices) {
            sb.append(serializeMessage(conv.messages[j]))
            if (j < conv.messages.size - 1) sb.append(", ")
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun serializeMessage(msg: AssistantMessage): String {
        val sb = StringBuilder("{")
        sb.append("\"id\": ").append(escape(msg.id)).append(", ")
        sb.append("\"conversationId\": ").append(escape(msg.conversationId)).append(", ")
        sb.append("\"sender\": ").append(escape(msg.sender.name)).append(", ")
        sb.append("\"text\": ").append(escape(msg.text)).append(", ")
        sb.append("\"timestampMillis\": ").append(msg.timestampMillis).append(", ")
        sb.append("\"status\": ").append(escape(msg.status.name)).append(", ")
        sb.append("\"errorMessage\": ").append(escape(msg.errorMessage)).append(", ")
        sb.append("\"clientMessageId\": ").append(escape(msg.clientMessageId)).append(", ")
        sb.append("\"provenance\": [")
        for (k in msg.provenance.indices) {
            val p = msg.provenance[k]
            sb.append("{")
            sb.append("\"source\": ").append(escape(p.source)).append(", ")
            sb.append("\"infoClass\": ").append(escape(p.infoClass.name)).append(", ")
            sb.append("\"description\": ").append(escape(p.description))
            sb.append("}")
            if (k < msg.provenance.size - 1) sb.append(", ")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun parseStorePayload(json: String): AssistantStorePayload {
        if (json.isBlank()) return AssistantStorePayload()

        fun extractString(key: String, source: String): String? {
            val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(source)
            return match?.groupValues?.get(1)?.let { unescape(it) }
        }

        fun extractLong(key: String, source: String): Long? {
            val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(source)
            return match?.groupValues?.get(1)?.toLongOrNull()
        }

        fun extractBoolean(key: String, source: String): Boolean {
            val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(source)
            return match?.groupValues?.get(1)?.toBoolean() ?: false
        }

        val version = extractLong("schemaVersion", json)?.toInt() ?: CURRENT_SCHEMA_VERSION
        val lastSaved = extractLong("lastSavedEpochMillis", json) ?: System.currentTimeMillis()

        // Extract consent section
        val consentMatch = Regex("\"consentPreferences\"\\s*:\\s*\\{([^}]+)\\}").find(json)
        val consentPreferences = if (consentMatch != null) {
            val cBlock = consentMatch.groupValues[1]
            AssistantConsentPreferences(
                consentGranted = extractBoolean("consentGranted", cBlock),
                shareLiveTelemetry = extractBoolean("shareLiveTelemetry", cBlock),
                shareAlerts = extractBoolean("shareAlerts", cBlock),
                shareHistoricalAnalytics = extractBoolean("shareHistoricalAnalytics", cBlock),
                shareCropProfile = extractBoolean("shareCropProfile", cBlock)
            )
        } else {
            AssistantConsentPreferences()
        }

        // Extract crop profile section
        val cropMatch = Regex("\"cropProfile\"\\s*:\\s*\\{([^}]+)\\}").find(json)
        val cropProfile = if (cropMatch != null) {
            val crBlock = cropMatch.groupValues[1]
            CropProfile(
                cropType = extractString("cropType", crBlock) ?: "",
                growthStage = extractString("growthStage", crBlock) ?: "",
                soilType = extractString("soilType", crBlock) ?: "",
                plantingDate = extractString("plantingDate", crBlock) ?: ""
            )
        } else {
            CropProfile()
        }

        // Extract conversations
        val conversations = mutableListOf<AssistantConversation>()
        val convArrayStart = json.indexOf("\"conversations\"")
        if (convArrayStart != -1) {
            val bracketStart = json.indexOf('[', convArrayStart)
            if (bracketStart != -1) {
                var depth = 0
                var bracketEnd = -1
                for (i in bracketStart until json.length) {
                    if (json[i] == '[') depth++
                    else if (json[i] == ']') {
                        depth--
                        if (depth == 0) {
                            bracketEnd = i
                            break
                        }
                    }
                }
                if (bracketEnd > bracketStart) {
                    val arrayContent = json.substring(bracketStart + 1, bracketEnd)
                    var objDepth = 0
                    var objStart = -1
                    for (i in arrayContent.indices) {
                        val c = arrayContent[i]
                        if (c == '{') {
                            if (objDepth == 0) objStart = i
                            objDepth++
                        } else if (c == '}') {
                            objDepth--
                            if (objDepth == 0 && objStart != -1) {
                                val objStr = arrayContent.substring(objStart, i + 1)
                                val parsedConv = parseSingleConversation(objStr)
                                if (parsedConv != null) {
                                    conversations.add(parsedConv)
                                }
                                objStart = -1
                            }
                        }
                    }
                }
            }
        }

        return AssistantStorePayload(
            schemaVersion = version,
            lastSavedEpochMillis = lastSaved,
            consentPreferences = consentPreferences,
            cropProfile = cropProfile,
            conversations = conversations
        )
    }

    private fun parseSingleConversation(convJson: String): AssistantConversation? {
        fun extractString(key: String): String? {
            val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(convJson)
            return match?.groupValues?.get(1)?.let { unescape(it) }
        }

        fun extractLong(key: String): Long? {
            val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(convJson)
            return match?.groupValues?.get(1)?.toLongOrNull()
        }

        val id = extractString("id") ?: return null
        val title = extractString("title") ?: "Conversation"
        val createdAt = extractLong("createdAtMillis") ?: System.currentTimeMillis()
        val updatedAt = extractLong("updatedAtMillis") ?: createdAt

        // Parse messages
        val messages = mutableListOf<AssistantMessage>()
        val msgArrayStart = convJson.indexOf("\"messages\"")
        if (msgArrayStart != -1) {
            val bracketStart = convJson.indexOf('[', msgArrayStart)
            if (bracketStart != -1) {
                var depth = 0
                var bracketEnd = -1
                for (i in bracketStart until convJson.length) {
                    if (convJson[i] == '[') depth++
                    else if (convJson[i] == ']') {
                        depth--
                        if (depth == 0) {
                            bracketEnd = i
                            break
                        }
                    }
                }
                if (bracketEnd > bracketStart) {
                    val arrayContent = convJson.substring(bracketStart + 1, bracketEnd)
                    var objDepth = 0
                    var objStart = -1
                    for (i in arrayContent.indices) {
                        val c = arrayContent[i]
                        if (c == '{') {
                            if (objDepth == 0) objStart = i
                            objDepth++
                        } else if (c == '}') {
                            objDepth--
                            if (objDepth == 0 && objStart != -1) {
                                val objStr = arrayContent.substring(objStart, i + 1)
                                val parsedMsg = parseSingleMessage(objStr)
                                if (parsedMsg != null) {
                                    messages.add(parsedMsg)
                                }
                                objStart = -1
                            }
                        }
                    }
                }
            }
        }

        return AssistantConversation(
            id = id,
            title = title,
            createdAtMillis = createdAt,
            updatedAtMillis = updatedAt,
            messages = messages
        )
    }

    private fun parseSingleMessage(msgJson: String): AssistantMessage? {
        fun extractString(key: String): String? {
            val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(msgJson)
            return match?.groupValues?.get(1)?.let { unescape(it) }
        }

        fun extractLong(key: String): Long? {
            val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(msgJson)
            return match?.groupValues?.get(1)?.toLongOrNull()
        }

        val id = extractString("id") ?: return null
        val convId = extractString("conversationId") ?: ""
        val senderStr = extractString("sender") ?: MessageSender.USER.name
        val sender = runCatching { MessageSender.valueOf(senderStr) }.getOrDefault(MessageSender.USER)
        val text = extractString("text") ?: ""
        val timestamp = extractLong("timestampMillis") ?: System.currentTimeMillis()
        val statusStr = extractString("status") ?: MessageStatus.SENT.name
        val status = runCatching { MessageStatus.valueOf(statusStr) }.getOrDefault(MessageStatus.SENT)
        val errorMsg = extractString("errorMessage")
        val clientMsgId = extractString("clientMessageId") ?: id

        // Provenance parsing
        val provenanceList = mutableListOf<InformationProvenance>()
        val provStart = msgJson.indexOf("\"provenance\"")
        if (provStart != -1) {
            val bracketStart = msgJson.indexOf('[', provStart)
            if (bracketStart != -1) {
                var depth = 0
                var bracketEnd = -1
                for (i in bracketStart until msgJson.length) {
                    if (msgJson[i] == '[') depth++
                    else if (msgJson[i] == ']') {
                        depth--
                        if (depth == 0) {
                            bracketEnd = i
                            break
                        }
                    }
                }
                if (bracketEnd > bracketStart) {
                    val arrayContent = msgJson.substring(bracketStart + 1, bracketEnd)
                    var objDepth = 0
                    var objStart = -1
                    for (i in arrayContent.indices) {
                        val c = arrayContent[i]
                        if (c == '{') {
                            if (objDepth == 0) objStart = i
                            objDepth++
                        } else if (c == '}') {
                            objDepth--
                            if (objDepth == 0 && objStart != -1) {
                                val pObj = arrayContent.substring(objStart, i + 1)
                                val src = Regex("\"source\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1)?.let { unescape(it) } ?: "Unknown"
                                val icStr = Regex("\"infoClass\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1) ?: InformationClass.AI_SUGGESTION.name
                                val ic = runCatching { InformationClass.valueOf(icStr) }.getOrDefault(InformationClass.AI_SUGGESTION)
                                val desc = Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1)?.let { unescape(it) } ?: ""
                                provenanceList.add(InformationProvenance(source = src, infoClass = ic, description = desc))
                                objStart = -1
                            }
                        }
                    }
                }
            }
        }

        return AssistantMessage(
            id = id,
            conversationId = convId,
            sender = sender,
            text = text,
            timestampMillis = timestamp,
            provenance = provenanceList,
            status = status,
            errorMessage = errorMsg,
            clientMessageId = clientMsgId
        )
    }

    // =========================================================================
    // FASTAPI HTTP REQUEST / RESPONSE SERIALIZATION
    // =========================================================================

    fun serializeChatRequest(
        clientMessageId: String,
        conversationId: String,
        userMessage: String,
        context: AssistantContext,
        forceMock: Boolean = false
    ): String {
        val sb = StringBuilder("{")
        sb.append("\"client_message_id\": ").append(escape(clientMessageId)).append(", ")
        sb.append("\"conversation_id\": ").append(escape(conversationId)).append(", ")
        sb.append("\"user_message\": ").append(escape(userMessage)).append(", ")
        sb.append("\"force_mock\": ").append(forceMock).append(", ")

        // Context object
        sb.append("\"context\": {")
        val cp = context.consent
        sb.append("\"consent_granted\": ").append(cp.consentGranted).append(", ")
        sb.append("\"share_live_telemetry\": ").append(cp.shareLiveTelemetry).append(", ")
        sb.append("\"share_alerts\": ").append(cp.shareAlerts).append(", ")
        sb.append("\"share_historical\": ").append(cp.shareHistoricalAnalytics).append(", ")
        sb.append("\"share_crop_profile\": ").append(cp.shareCropProfile).append(", ")

        // Sensor context
        val sc = context.sensorContext
        if (sc != null) {
            sb.append("\"sensor_context\": {")
            sb.append("\"battery_percent\": ").append(sc.batteryPercent ?: "null").append(", ")
            sb.append("\"solar_volts\": ").append(sc.solarVolts ?: "null").append(", ")
            sb.append("\"soil_moisture_raw\": ").append(sc.soilMoistureRaw ?: "null").append(", ")
            sb.append("\"soil_moisture_uncalibrated\": ").append(sc.soilMoistureUncalibrated).append(", ")
            sb.append("\"soil_moisture_label\": ").append(escape(sc.soilMoistureLabel)).append(", ")
            sb.append("\"soil_temperature\": ").append(sc.soilTemperature ?: "null").append(", ")
            sb.append("\"soil_ph_raw\": ").append(sc.soilPhRaw ?: "null").append(", ")
            sb.append("\"soil_ph_label\": ").append(escape(sc.soilPhLabel)).append(", ")
            sb.append("\"air_temp\": ").append(sc.airTemp ?: "null").append(", ")
            sb.append("\"air_humidity\": ").append(sc.airHumidity ?: "null").append(", ")
            sb.append("\"data_origin\": ").append(escape(sc.dataOrigin)).append(", ")
            sb.append("\"is_stale\": ").append(sc.isStale).append(", ")
            sb.append("\"reading_age_seconds\": ").append(sc.readingAgeSeconds ?: "null")
            sb.append("}, ")
        } else {
            sb.append("\"sensor_context\": null, ")
        }

        // Active Alerts array
        sb.append("\"alert_context\": [")
        for (i in context.alertContext.indices) {
            val a = context.alertContext[i]
            sb.append("{")
            sb.append("\"alert_id\": ").append(escape(a.id)).append(", ")
            sb.append("\"title\": ").append(escape(a.title)).append(", ")
            sb.append("\"severity\": ").append(escape(a.severity.name)).append(", ")
            sb.append("\"metric\": ").append(escape(a.sourceSensor)).append(", ")
            sb.append("\"current_value\": ").append(escape(a.triggerValue?.toString() ?: "N/A")).append(", ")
            sb.append("\"threshold_value\": ").append(escape(a.configuredThreshold?.toString() ?: "N/A")).append(", ")
            sb.append("\"timestamp_epoch_ms\": ").append(a.createdTimestampEpochMillis)
            sb.append("}")
            if (i < context.alertContext.size - 1) sb.append(", ")
        }
        sb.append("], ")

        // Historical Context array
        sb.append("\"historical_context\": [")
        for (i in context.historicalContext.indices) {
            val h = context.historicalContext[i]
            sb.append("{")
            sb.append("\"metric_name\": ").append(escape(h.metricName)).append(", ")
            sb.append("\"range_label\": ").append(escape(h.rangeLabel)).append(", ")
            sb.append("\"min_value\": ").append(h.minValue ?: "null").append(", ")
            sb.append("\"max_value\": ").append(h.maxValue ?: "null").append(", ")
            sb.append("\"avg_value\": ").append(h.avgValue ?: "null").append(", ")
            sb.append("\"data_points_count\": ").append(h.dataPointsCount).append(", ")
            sb.append("\"insufficient_data\": ").append(h.insufficientData)
            sb.append("}")
            if (i < context.historicalContext.size - 1) sb.append(", ")
        }
        sb.append("], ")

        // Crop profile
        val prof = context.cropProfile
        if (prof != null) {
            sb.append("\"crop_profile\": {")
            sb.append("\"crop_type\": ").append(escape(prof.cropType)).append(", ")
            sb.append("\"growth_stage\": ").append(escape(prof.growthStage)).append(", ")
            sb.append("\"soil_type\": ").append(escape(prof.soilType)).append(", ")
            sb.append("\"planting_date\": ").append(escape(prof.plantingDate))
            sb.append("}")
        } else {
            sb.append("\"crop_profile\": null")
        }

        sb.append("}}")
        return sb.toString()
    }

    fun parseChatResponse(json: String): AssistantMessage {
        fun extractString(key: String): String? {
            // Match escaped strings or JSON string content
            val match = Regex("\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"").find(json)
            return match?.groupValues?.get(1)?.let { unescape(it) }
        }

        val convId = extractString("conversation_id") ?: ""
        val clientMsgId = extractString("client_message_id") ?: ""
        val replyText = extractString("reply_text") ?: "No response from AI assistant."
        val modelUsed = extractString("model_used") ?: "fastapi-gemini"

        // Parse provenance
        val provenance = mutableListOf<InformationProvenance>()
        val provStart = json.indexOf("\"provenance\"")
        if (provStart != -1) {
            val bracketStart = json.indexOf('[', provStart)
            if (bracketStart != -1) {
                var depth = 0
                var bracketEnd = -1
                for (i in bracketStart until json.length) {
                    if (json[i] == '[') depth++
                    else if (json[i] == ']') {
                        depth--
                        if (depth == 0) {
                            bracketEnd = i
                            break
                        }
                    }
                }
                if (bracketEnd > bracketStart) {
                    val arrayContent = json.substring(bracketStart + 1, bracketEnd)
                    var objDepth = 0
                    var objStart = -1
                    for (i in arrayContent.indices) {
                        val c = arrayContent[i]
                        if (c == '{') {
                            if (objDepth == 0) objStart = i
                            objDepth++
                        } else if (c == '}') {
                            objDepth--
                            if (objDepth == 0 && objStart != -1) {
                                val pObj = arrayContent.substring(objStart, i + 1)
                                val src = Regex("\"source\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1)?.let { unescape(it) } ?: modelUsed
                                val icStr = Regex("\"information_class\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1) ?: InformationClass.AI_SUGGESTION.name
                                val ic = runCatching { InformationClass.valueOf(icStr) }.getOrDefault(InformationClass.AI_SUGGESTION)
                                val desc = Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").find(pObj)?.groupValues?.get(1)?.let { unescape(it) } ?: ""
                                provenance.add(InformationProvenance(source = src, infoClass = ic, description = desc))
                                objStart = -1
                            }
                        }
                    }
                }
            }
        }

        return AssistantMessage(
            conversationId = convId,
            sender = MessageSender.ASSISTANT,
            text = replyText,
            timestampMillis = System.currentTimeMillis(),
            provenance = provenance,
            status = MessageStatus.SENT,
            clientMessageId = clientMsgId
        )
    }

    fun parseErrorResponse(json: String, fallbackStatus: Int): String {
        val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(json)
        return msgMatch?.groupValues?.get(1)?.let { unescape(it) }
            ?: "Server error ($fallbackStatus)"
    }
}

