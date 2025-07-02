package com.bazaar.telemetry

/**
 * Configuration for the Telemetry system.
 */
interface TelemetryConfig {
    val serviceName: String
    val endpoint: String
}

/**
 * Default implementation of [TelemetryConfig].
 */
data class DefaultTelemetryConfig(
    override val serviceName: String,
    override val endpoint: String
) : TelemetryConfig
