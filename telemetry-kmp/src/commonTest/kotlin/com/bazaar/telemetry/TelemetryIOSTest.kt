package com.bazaar.telemetry

import com.bazaar.telemetry.Attributes
import com.bazaar.telemetry.DefaultTelemetryConfig
import com.bazaar.telemetry.LogLevel
import com.bazaar.telemetry.Telemetry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class TelemetryIOSTest {

    @Test
    fun testTelemetryIOSInitializationAndBasicUsage() {
        // This test will run on all platforms, including iOS.
        // It primarily serves as a smoke test for the iOS OTLP implementation.

        val config = DefaultTelemetryConfig(
            serviceName = "TestServiceiOS",
            endpoint = "http://localhost:4318", // Dummy endpoint, OTLP HTTP default. Exporter might try to connect.
            // Depending on exporter, might need /v1/logs, /v1/traces etc. appended.
            // For a smoke test, as long as it doesn't crash resolving/connecting, it's a basic check.
            deploymentEnvironment = "test",
            telemetrySchemaVersion = "1.0.0"
        )

        var initializationSuccessful = false
        try {
            Telemetry.init(config)
            initializationSuccessful = true
        } catch (e: Exception) {
            // Log or print exception if needed for debugging
            // e.printStackTrace()
            assertTrue(false, "Telemetry.init on iOS threw an exception: ${e.message}")
        }
        assertTrue(initializationSuccessful, "Telemetry.init on iOS failed to complete without an exception (see logs if it printed one).")


        try {
            runBlocking {
                Telemetry.span("testSpaniOS", Attributes.builder().put("test.attr", "value").build()) { span ->
                    span.setAttribute("inside.span", "true")
                    // Simulate some work
                }
            }

            Telemetry.log(
                LogLevel.INFO,
                "Test log message from iOS",
                attrs = Attributes.builder().put("log.attr", "logValue").build()
            )

            Telemetry.counter("testCounteriOS", 1, Attributes.builder().put("counter.attr", "cValue").build())
            Telemetry.histogram("testHistogramiOS", 12.3, Attributes.builder().put("hist.attr", "hValue").build())
            Telemetry.gauge("testGaugeiOS", 45.6, Attributes.builder().put("gauge.attr", "gValue").build())

            Telemetry.setCommonAttributes(Attributes.builder().put("common.attr", "commonValue").build())
            // Call a method again to see if common attributes are picked up (not verifiable here without backend/mock)
             Telemetry.log(LogLevel.DEBUG, "Test log with common attributes from iOS")


        } catch (e: Exception) {
            // Log or print exception
            // e.printStackTrace()
            assertTrue(false, "Telemetry usage on iOS (span, log, metrics) threw an exception: ${e.message}")
        } finally {
            try {
                Telemetry.shutdown()
            } catch (e: Exception) {
                // Log or print exception
                // e.printStackTrace()
                assertTrue(false, "Telemetry.shutdown on iOS threw an exception: ${e.message}")
            }
        }

        // If we reached here, basic operations did not crash.
        assertTrue(true, "Telemetry iOS basic usage test completed without throwing exceptions.")
    }
}
