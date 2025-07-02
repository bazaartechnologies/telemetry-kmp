package com.bazaar.telemetry

import com.bazaar.telemetry.Telemetry.init
import com.bazaar.telemetry.Telemetry.log
import com.bazaar.telemetry.Telemetry.span

/**
 * Telemetry API for tracing, logging, and metrics in a multiplatform context.
 *
 * Use [init] to configure, [log] for structured logs, and [span] for tracing.
 *
 * This API is designed for extension and testability.
 */
expect object Telemetry {
    /**
     * Initialize telemetry with the given config.
     */
    fun init(config: TelemetryConfig)

    /**
     * Run a block within a named span for tracing.
     * @param name The span name.
     * @param attrs Attributes to attach to the span.
     * @param block The code to run within the span.
     */
    suspend fun <T> span(name: String, attrs: Attributes = Attributes.empty(), block: suspend (Span) -> T): T

    /**
     * Log a message at the given level, optionally with a cause and attributes.
     */
    fun log(level: LogLevel, message: String, cause: Throwable? = null, attrs: Attributes = Attributes.empty())

    /**
     * Record a counter metric (e.g., increment a named counter by a value).
     */
    fun counter(name: String, value: Long = 1, attrs: Attributes = Attributes.empty())

    /**
     * Record a histogram metric (e.g., observe a value for a named histogram).
     */
    fun histogram(name: String, value: Double, attrs: Attributes = Attributes.empty())

    /**
     * Record a gauge metric (e.g., set a value for a named gauge).
     */
    fun gauge(name: String, value: Double, attrs: Attributes = Attributes.empty())

    /**
     * Set additional common attributes (e.g. user.id, session.id) to be attached to all telemetry data.
     */
    fun setCommonAttributes(attrs: Attributes)

    /**
     * Shutdown and flush all telemetry providers.
     */
    fun shutdown()

    /**
     * Crash and vitals interface for platform-specific reporting.
     */
    val crashAndVitals: CrashAndVitals
}
