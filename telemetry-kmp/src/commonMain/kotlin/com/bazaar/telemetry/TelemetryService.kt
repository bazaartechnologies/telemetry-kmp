package com.bazaar.telemetry

/**
 * Common multiplatform Telemetry API for tracing, logging, and metrics.
 */
interface TelemetryService {
    /** Log levels for structured logging. */
    enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    /** Initialize telemetry with the given config. */
    fun init(config: TelemetryConfig)

    /** Set additional common attributes (e.g. user.id, session.id). */
    fun setCommonAttributes(attrs: Attributes)

    /** Shutdown and flush all telemetry providers. */
    fun shutdown()

    /** Log a message at the given level, optionally with attributes and a cause. */
    fun log(level: LogLevel, message: String, attrs: Attributes = Attributes.empty(), throwable: Throwable? = null)

    /** Increment the request count metric. */
    fun incRequestCount(amount: Long = 1, attrs: Attributes = Attributes.empty())

    /** Crash and vitals interface for platform-specific reporting. */
    val crashAndVitals: CrashAndVitals
}

/**
 * Exporter config abstractions for minimal user input.
 */
data class TelemetryExporterConfig(
    val endpoint: String,
    val headers: Map<String, String> = emptyMap(),
    val type: ExporterType = ExporterType.OTLP_GRPC
) {
    enum class ExporterType { OTLP_GRPC /*, OTLP_HTTP, JAEGER, ZIPKIN, etc. */ }
}

/**
 * Attributes for structured telemetry data.
 */
class Attributes private constructor(private val attributesMap: Map<String, Any>) {
    fun getString(key: String): String? = attributesMap[key] as? String
    fun getInt(key: String): Int? = attributesMap[key] as? Int
    fun getLong(key: String): Long? = attributesMap[key] as? Long
    fun getDouble(key: String): Double? = attributesMap[key] as? Double
    fun getBoolean(key: String): Boolean? = attributesMap[key] as? Boolean

    internal fun toMap(): Map<String, Any> = attributesMap

    class Builder {
        private val attributesMap = mutableMapOf<String, Any>()
        fun put(key: String, value: String): Builder { attributesMap[key] = value; return this }
        fun put(key: String, value: Int): Builder { attributesMap[key] = value; return this }
        fun put(key: String, value: Long): Builder { attributesMap[key] = value; return this }
        fun put(key: String, value: Double): Builder { attributesMap[key] = value; return this }
        fun put(key: String, value: Boolean): Builder { attributesMap[key] = value; return this }
        fun putAll(attributes: Attributes): Builder { attributesMap.putAll(attributes.attributesMap); return this }
        fun build(): Attributes = Attributes(attributesMap.toMap())
    }
    companion object {
        fun builder(): Builder = Builder()
        fun empty(): Attributes = Attributes(emptyMap())
    }
} 