package com.bazaar.telemetry

/**
 * Represents a tracing span for distributed tracing.
 *
 * Use [setAttribute] to add metadata, [recordException] to log errors, and [close] to finish the span.
 */
interface Span : AutoCloseable {
    /**
     * Set a key-value attribute on the span.
     */
    fun setAttribute(key: String, value: String)
    /**
     * Record an exception in the span.
     */
    fun recordException(t: Throwable)
    /**
     * End the span.
     */
    override fun close()
}
