package com.bazaar.telemetry

actual object Telemetry {
    actual fun init(config: TelemetryConfig) {
        // TODO: Configure OTLP exporter via Ktor gRPC or HTTP here.
        println("Telemetry initialized for ${config.serviceName} (iOS) at ${config.endpoint}")
    }

    actual suspend fun <T> span(name: String, block: suspend (Span) -> T): T = block(object : Span {
        override fun setAttribute(key: String, value: String) {}
        override fun recordException(t: Throwable) {}
        override fun close() {}
    })

    actual fun log(level: LogLevel, message: String, cause: Throwable?) {
        println("[${level.name}] $message")
        cause?.let { println("Cause: $it") }
    }

    actual val crashAndVitals = CrashAndVitalsProvider.instance

    actual fun counter(name: String, value: Long, attributes: Map<String, Any>) {
        // No-op or print for now
    }

    actual fun histogram(name: String, value: Double, attributes: Map<String, Any>) {
        // No-op or print for now
    }

    actual fun gauge(name: String, value: Double, attributes: Map<String, Any>) {
        // No-op or print for now
    }
}
