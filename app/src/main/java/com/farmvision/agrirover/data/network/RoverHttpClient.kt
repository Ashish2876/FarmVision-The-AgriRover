package com.farmvision.agrirover.data.network

import com.farmvision.agrirover.domain.protocol.ProtocolConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Encapsulates an HTTP response from a rover microcontroller endpoint.
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String?,
    val latencyMs: Long,
    val headers: Map<String, List<String>> = emptyMap()
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Exception representing network communication failures with rover microcontrollers.
 */
/**
 * Exception representing network communication failures with rover microcontrollers.
 */
class RoverNetworkException(
    message: String,
    cause: Throwable? = null,
    val isTimeout: Boolean = false,
    val isUnreachable: Boolean = false
) : Exception(message, cause)

/**
 * Exception representing authentication failures or missing credentials.
 */
class RoverAuthenticationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Abstraction for dispatching HTTP requests to rover microcontrollers.
 */
interface RoverHttpClient {
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 3000L
    ): HttpResponse

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 3000L
    ): HttpResponse
}

/**
 * Standard implementation of [RoverHttpClient] using [HttpURLConnection] on [Dispatchers.IO].
 * Supports per-request [android.net.Network] socket binding when [networkProvider] is supplied.
 * Zero external dependencies; fully compliant with Android cleartext domain restrictions.
 */
class StandardRoverHttpClient(
    private val networkProvider: (() -> android.net.Network?)? = null
) : RoverHttpClient {

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(
            method = "GET",
            urlString = url,
            body = null,
            headers = headers,
            timeoutMs = timeoutMs
        )
    }

    override suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(
            method = "POST",
            urlString = url,
            body = body,
            headers = headers,
            timeoutMs = timeoutMs
        )
    }

    private fun executeRequest(
        method: String,
        urlString: String,
        body: String?,
        headers: Map<String, String>,
        timeoutMs: Long
    ): HttpResponse {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            val boundNetwork = networkProvider?.invoke()
            connection = (if (boundNetwork != null) {
                boundNetwork.openConnection(url) as HttpURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }).apply {
                requestMethod = method
                connectTimeout = timeoutMs.toInt().coerceAtLeast(100)
                readTimeout = timeoutMs.toInt().coerceAtLeast(100)
                useCaches = false
                instanceFollowRedirects = false

                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }

                // Add custom headers (e.g. bearer token)
                headers.forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
            }

            // Write payload if present
            if (body != null) {
                connection.outputStream.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                        writer.write(body)
                        writer.flush()
                    }
                }
            }

            val statusCode = connection.responseCode
            val latencyMs = System.currentTimeMillis() - startTime

            val stream = if (statusCode in 200..399) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = stream?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }

            return HttpResponse(
                statusCode = statusCode,
                body = responseBody,
                latencyMs = latencyMs,
                headers = connection.headerFields ?: emptyMap()
            )
        } catch (e: SocketTimeoutException) {
            val latency = System.currentTimeMillis() - startTime
            throw RoverNetworkException(
                message = "Request to ${redactUrl(urlString)} timed out after ${latency}ms (limit: ${timeoutMs}ms)",
                cause = e,
                isTimeout = true
            )
        } catch (e: ConnectException) {
            throw RoverNetworkException(
                message = "Endpoint unreachable at ${redactUrl(urlString)} (${e.message ?: "Connection refused"})",
                cause = e,
                isUnreachable = true
            )
        } catch (e: UnknownHostException) {
            throw RoverNetworkException(
                message = "Unknown host in ${redactUrl(urlString)}",
                cause = e,
                isUnreachable = true
            )
        } catch (e: Exception) {
            throw RoverNetworkException(
                message = "Network error connecting to ${redactUrl(urlString)}: ${e.message}",
                cause = e
            )
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        /**
         * Redacts bearer token credentials from URLs or log messages.
         */
        fun redactUrl(url: String): String {
            return url.replace(Regex("""token=[^&]+"""), "token=REDACTED")
        }

        /**
         * Redacts a secret token for display in error messages or logs.
         */
        fun redactToken(token: String?): String {
            if (token.isNullOrBlank()) return "<none>"
            return if (token.length <= 4) "****" else "${token.take(2)}****${token.takeLast(2)}"
        }
    }
}
