package com.bazaar.telemetry

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.math.pow

/**
 * A Ktor HTTP client specifically for sending OTLP protobuf data.
 * Can be initialized with a specific engine for testing.
 */
class OtlpHttpClient(engineFactory: HttpClientEngineFactory<*> = Darwin) {

    private val ktorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val httpClient: HttpClient = HttpClient(engineFactory) {
        // engine specific configuration if any for Darwin, CIO, etc.
        // For Darwin, it might be within engineFactory block if needed:
        // if (engineFactory == Darwin) {
        //     engine {
        //         configureRequest {
        //             // setTimeoutIntervalForRequest(60.0)
        //         }
        //     }
        // }
        // No specific engine config here, assuming defaults are fine or set by test engine.

        // Default request configuration (can be overridden per request)
        // expectSuccess = true // Ktor will throw exceptions for non-2xx responses if true

        // Logging can be added here if desired
        // install(Logging) {
        //     logger = Logger.DEFAULT
        //     level = LogLevel.INFO
        // }
    }

    /**
     * Posts serialized OTLP data to the specified OTLP endpoint path.
     *
     * @param fullEndpointUrl The full URL for the OTLP signal (e.g., "http://localhost:4318/v1/traces").
     * @param data The serialized protobuf data as a ByteArray.
     * @return True if the request was successful (e.g., 2xx response), false otherwise.
     */
    suspend fun postOtlpData(fullEndpointUrl: String, data: ByteArray): Boolean {
        return try {
            val response: HttpResponse = httpClient.post(fullEndpointUrl) {
                contentType(ContentType.Application.XProtobuf)
                setBody(data) // Ktor will handle ByteArray appropriately
            }

            // OTLP/HTTP spec typically expects a 200 OK response for success.
            // Other 2xx codes might also be acceptable depending on the collector.
            if (response.status.isSuccess()) {
                println("OTLP data successfully sent to $fullEndpointUrl. Status: ${response.status}")
                true
            } else {
                // OTLP spec defines how partial success or failures should be reported in the response body (JSON).
                // For simplicity, we're just checking status code here.
                // A more robust implementation would parse the response body for error details.
                val errorBody = response.bodyAsText()
                println("Failed to send OTLP data to $fullEndpointUrl. Status: ${response.status}. Body: $errorBody")
                false
            }
        } catch (e: Exception) {
            // This includes network errors, serialization issues (though data is pre-serialized), etc.
            println("Exception while sending OTLP data to $fullEndpointUrl: ${e.message}")
            // e.printStackTrace() // For debugging
            false
        }
    }

    /**
     * Cleans up the HttpClient. Call during Telemetry shutdown.
     */
    fun shutdown() {
        httpClient.close()
        ktorScope.cancel() // Cancel any coroutines launched by this object
        println("OtlpHttpClient shutdown.")
    }

    suspend fun sendWithRetries(
        url: String,
        data: ByteArray,
        type: String,
        maxRetries: Int,
        initialRetryDelay: Duration
    ): Boolean {
        var currentRetry = 0
        var success = false
        while (currentRetry <= maxRetries && !success) {
            if (currentRetry > 0) {
                val delayMillis = (initialRetryDelay * (2.0.pow(currentRetry - 1))).inWholeMilliseconds
                delay(delayMillis) // Ensure delay is available in this scope (import kotlinx.coroutines.delay)
                println("Retrying $type export to $url (attempt ${currentRetry + 1})")
            }
            success = postOtlpData(url, data) // Call the class's own postOtlpData method
            if (success) {
                println("$type data successfully exported to $url.")
                break
            }
            currentRetry++
        }
        if (!success) {
            println("Failed to export $type data to $url after $maxRetries retries.")
        }
        return success
    }
}

// Helper for exponential backoff, not available in kotlin.math directly for Duration
// This should be top-level or in a common utility if used elsewhere.
// For now, keeping it here for context if needed by sendWithRetries.
// However, CustomOtlpExporter already has one, so this might be duplicate.
// private fun Double.pow(i: Int): Double = kotlin.math.pow(this, i.toDouble())


// Example usage (conceptual, will be used by CustomOtlpExporter):
// In CustomOtlpExporter:
// val success = otlpHttpClient.sendWithRetries(
//    "${config.endpoint}/v1/traces",
//    requestBytes,
//    "Traces",
//    maxRetries,
//    initialRetryDelay
// )
