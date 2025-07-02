package com.bazaar.telemetry

import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

class TelemetryTest {
    @Test
    @Ignore("OpenTelemetry SDK cannot be initialized in JVM unit tests; run as instrumented test or mock")
    fun testInitDoesNotThrow() {
        // Should not throw
        Telemetry.init(DefaultTelemetryConfig("TestService", "http://localhost"))
    }

    @Test
    fun testLogDoesNotThrow() {
        // Should not throw
        Telemetry.log(LogLevel.INFO, "Test log message")
    }

    @Test
    fun testSpanExecutesBlock() {
        var called = false
        runBlocking {
            Telemetry.span("test-span") { span ->
                called = true
            }
        }
        assertTrue(called, "Span block should be called")
    }

    @Test
    @Ignore("CrashAndVitals not supported in JVM unit tests; run as instrumented test or mock")
    fun testCrashAndVitals() {
        val crashAndVitals = Telemetry.crashAndVitals
        crashAndVitals.recordVital("test_key", "test_value")
        val vitals = crashAndVitals.getVitals()
        assertTrue(vitals["test_key"] == "test_value", "Vitals should contain the test key")
        // Crash reporting should not throw
        crashAndVitals.recordCrash(Exception("Test crash"))
    }
}

class TelemetryApiTest {
    @Test
    fun testLogLevelEnum() {
        val levels = LogLevel.values().toSet()
        assertTrue(LogLevel.TRACE in levels)
        assertTrue(LogLevel.DEBUG in levels)
        assertTrue(LogLevel.INFO in levels)
        assertTrue(LogLevel.WARN in levels)
        assertTrue(LogLevel.ERROR in levels)
    }

    @Test
    fun testTelemetryApiSurface() {
        // These calls are compile-time only; actuals are platform-specific.
        // This test ensures the API is callable and signatures are correct.
        // No runtime assertion, just type checking.
        Telemetry.init(object : TelemetryConfig {
            override val serviceName = "test"
            override val endpoint = "http://localhost"
        })
        Telemetry.log(LogLevel.INFO, "Test message")
        Telemetry.log(LogLevel.ERROR, "Error message", Throwable("fail"))
        Telemetry.crashAndVitals.getVitals()
        Telemetry.counter("test_counter", 2)
        Telemetry.histogram("test_histogram", 1.23)
        Telemetry.gauge("test_gauge", 4.56)
    }
} 