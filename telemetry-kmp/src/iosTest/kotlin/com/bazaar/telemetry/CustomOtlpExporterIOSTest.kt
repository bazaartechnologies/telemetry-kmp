package com.bazaar.telemetry

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class CustomOtlpExporterIOSTest {

    private lateinit var mockEngine: MockEngine
    private lateinit var otlpHttpClient: OtlpHttpClient
    private lateinit var exporter: CustomOtlpExporter
    private lateinit var testConfig: TelemetryConfig

    // Captured requests
    private val capturedRequests = mutableListOf<MockHttpRequest>()

    @BeforeTest
    fun setup() {
        capturedRequests.clear()
        mockEngine = MockEngine { request ->
            // Capture the request for assertions
            capturedRequests.add(request)

            // Respond with 200 OK for all OTLP requests in this basic test
            respond(
                content = "OK", // OTLP HTTP often returns JSON for errors, but simple OK for success
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        otlpHttpClient = OtlpHttpClient(engineFactory = mockEngine)
        testConfig = DefaultTelemetryConfig(
            serviceName = "TestServiceiOSCustom",
            endpoint = "http://fake-collector:4318" // Base endpoint
        )
        // For testing, use a very short export interval to speed things up if relying on periodic flush.
        // However, we will mostly trigger flushes manually or by batch size.
        // The CustomOtlpExporter's hardcoded interval and batch size will be used unless made configurable.
        exporter = CustomOtlpExporter(testConfig, Attributes.empty(), otlpHttpClient)
        // Exporter is not started by default here, tests can call start() or trigger flushes manually.
    }

    @Test
    fun testExporterSendsSpanData() = runBlocking {
        exporter.start() // Start the exporter for periodic flushes if needed, or rely on batch limits

        // Create a placeholder OtlpSpan (actual mapping depends on Wire)
        // In a real test, this would come from OtlpMappers.kt
        val testOtlpSpan: OtlpSpan = Any() // Placeholder for opentelemetry.proto.trace.v1.Span

        // Add enough spans to trigger a batch export (assuming batchSizeLimit = 100, or adjust)
        // For simplicity, let's assume one item is enough if we manually flush or batch size is 1.
        // To test batchSizeLimit, CustomOtlpExporter needs to be configurable or we send 100 items.
        // Let's assume the exporter's batchSizeLimit is high, and we'll test flushing behavior.

        // This test currently relies on the placeholder `requestBytes = ByteArray(0)` in CustomOtlpExporter
        // not being empty. If it is, the test will show no requests.
        // The user must implement the actual serialization for this test to be meaningful for content.
        println("Warning: This test's request validation is limited until actual OTLP serialization is implemented in CustomOtlpExporter.")

        exporter.addSpan(testOtlpSpan)
        // Manually trigger flush or wait for batch limit / interval
        // For now, let CustomOtlpExporter's internal logic (e.g. batch size = 1 for testing) handle it
        // Or, if we had a manual flush on the exporter: exporter.flushSpansOnly()

        // To ensure the async addSpan and subsequent flush (if triggered by size) completes:
        delay(500.milliseconds) // Give some time for async operations if any.
                                  // A more robust way would be to have flush methods return Job and join.

        // If relying on periodic flush, need to delay for more than exportInterval.
        // For this test, assume addSpan might trigger a flush if batch size is met.
        // The CustomOtlpExporter's current flush logic is async.

        // Check if a request was made
        // This part of the test will be more robust once actual serialization is in place.
        // If OtlpMappers and serialization are placeholders, no actual data bytes are sent.
        if (capturedRequests.isEmpty()) {
            // This might happen if batch conditions weren't met or serialization is placeholder
            println("No requests captured. This might be due to placeholder serialization in exporter or batching conditions.")
        }

        val traceRequest = capturedRequests.find { it.url.toString().endsWith("/v1/traces") }
        assertNotNull(traceRequest, "No trace request was made to /v1/traces")
        assertEquals(HttpMethod.Post, traceRequest.method)
        assertEquals("application/x-protobuf", traceRequest.headers[HttpHeaders.ContentType])
        // Cannot easily assert body content without parsing protobuf, but can check if it's non-empty
        // when actual serialization is implemented.
        // assertTrue(traceRequest.body.toByteArray().isNotEmpty(), "Request body should not be empty")

        exporter.shutdown()
    }

    @Test
    fun testExporterSendsLogData() = runBlocking {
        exporter.start()
        val testOtlpLogRecord: OtlpLogRecord = Any() // Placeholder

        println("Warning: This test's request validation is limited until actual OTLP serialization is implemented in CustomOtlpExporter.")
        exporter.addLogRecord(testOtlpLogRecord)
        delay(500.milliseconds)


        if (capturedRequests.isEmpty()) {
            println("No requests captured for logs. Placeholder serialization or batching?")
        }

        val logRequest = capturedRequests.find { it.url.toString().endsWith("/v1/logs") }
        assertNotNull(logRequest, "No log request was made to /v1/logs")
        assertEquals(HttpMethod.Post, logRequest.method)
        assertEquals("application/x-protobuf", logRequest.headers[HttpHeaders.ContentType])

        exporter.shutdown()
    }

    @Test
    fun testExporterSendsMetricData() = runBlocking {
        exporter.start()
        val testOtlpMetric: OtlpMetric = Any() // Placeholder

        println("Warning: This test's request validation is limited until actual OTLP serialization is implemented in CustomOtlpExporter.")
        exporter.addMetric(testOtlpMetric)
        delay(500.milliseconds)

        if (capturedRequests.isEmpty()) {
            println("No requests captured for metrics. Placeholder serialization or batching?")
        }

        val metricRequest = capturedRequests.find { it.url.toString().endsWith("/v1/metrics") }
        assertNotNull(metricRequest, "No metric request was made to /v1/metrics")
        assertEquals(HttpMethod.Post, metricRequest.method)
        assertEquals("application/x-protobuf", metricRequest.headers[HttpHeaders.ContentType])

        exporter.shutdown()
    }

    @Test
    fun testShutdownFlushesDataAndStopsExporter() = runBlocking {
        // Add some data
        exporter.addSpan(Any())
        exporter.addLogRecord(Any())
        exporter.addMetric(Any())

        val initialRequestCount = capturedRequests.size

        exporter.start() // Start it if not already (e.g. if tests run it selectively)
        delay(100.milliseconds) // brief delay to ensure start processing

        exporter.shutdown() // This should trigger a final flush

        // More requests should have been made due to shutdown flush
        // The exact number depends on whether items triggered individual flushes before shutdown
        // For this test, we expect at least some attempt to send the 3 items.
        assertTrue(capturedRequests.size > initialRequestCount || capturedRequests.size >= 3, "Shutdown should attempt to flush pending data. Requests: ${capturedRequests.size}")

        // Verify exporter is stopped (e.g., new data isn't sent)
        // This is harder to verify without inspecting internal state of exporter's jobs
        // or by trying to send more data and ensuring no new network calls.
        val requestsAfterShutdown = capturedRequests.size
        exporter.addSpan(Any()) // Try adding more data
        delay(100.milliseconds) // Wait a bit

        assertEquals(requestsAfterShutdown, capturedRequests.size, "No new requests should be made after shutdown.")
    }
}
