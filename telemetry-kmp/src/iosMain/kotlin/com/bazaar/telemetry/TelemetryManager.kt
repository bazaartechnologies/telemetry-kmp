package com.bazaar.telemetry

actual object TelemetryManager : TelemetryService {
    override fun init(config: TelemetryConfig) {
        println("[iOS] TelemetryManager.init called with config: $config")
    }
    override fun setCommonAttributes(attrs: Attributes) {
        println("[iOS] setCommonAttributes: $attrs")
    }
    override fun shutdown() {
        println("[iOS] shutdown called")
    }
    override fun log(level: TelemetryService.LogLevel, message: String, attrs: Attributes, throwable: Throwable?) {
        println("[iOS] log: $level, $message, $attrs, $throwable")
    }
    override fun incRequestCount(amount: Long, attrs: Attributes) {
        println("[iOS] incRequestCount: $amount, $attrs")
    }
    override val crashAndVitals: CrashAndVitals
        get() = CrashAndVitalsProvider.instance
} 