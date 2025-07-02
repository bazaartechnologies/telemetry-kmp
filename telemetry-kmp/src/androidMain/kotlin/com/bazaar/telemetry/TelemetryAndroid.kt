package com.bazaar.telemetry

import android.app.Application
import android.util.Log
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler

private var otel: OpenTelemetry? = null
private var tracer: Tracer? = null
private var commonAttributes: Attributes = Attributes.empty()

actual object Telemetry {
    actual fun init(config: TelemetryConfig) {
        val resource = Resource.getDefault().toBuilder()
            .put("service.name", config.serviceName)
            .build()
        val spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(config.endpoint)
            .build()
        val tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setSampler(Sampler.alwaysOn())
            .build()
        val sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal()
        otel = sdk
        tracer = sdk.getTracer(config.serviceName)
    }

    actual suspend fun <T> span(name: String, attrs: Attributes, block: suspend (Span) -> T): T {
        val span = tracer?.spanBuilder(name)?.setSpanKind(SpanKind.INTERNAL)
        attrs.toMap().forEach { (k, v) -> span?.setAttribute(k, v.toString()) }
        commonAttributes.toMap().forEach { (k, v) -> span?.setAttribute(k, v.toString()) }
        val started = span?.startSpan()
        val spanImpl = object : com.bazaar.telemetry.Span {
            override fun setAttribute(key: String, value: String) {
                started?.setAttribute(key, value)
            }
            override fun recordException(t: Throwable) {
                started?.recordException(t)
            }
            override fun close() {
                started?.end()
            }
        }
        try {
            return block(spanImpl)
        } finally {
            started?.end()
        }
    }

    actual fun log(level: LogLevel, message: String, cause: Throwable?, attrs: Attributes) {
        // Fallback to Android Logcat for now
        val tag = "Telemetry"
        val msg = if (cause != null) "$message\n${cause.stackTraceToString()}" else message
        when (level) {
            LogLevel.TRACE, LogLevel.DEBUG -> Log.d(tag, msg)
            LogLevel.INFO -> Log.i(tag, msg)
            LogLevel.WARN -> Log.w(tag, msg)
            LogLevel.ERROR -> Log.e(tag, msg)
        }
        // TODO: Implement OpenTelemetry logging when SdkLoggerProvider is available
    }

    actual fun counter(name: String, value: Long, attrs: Attributes) {
        // TODO: Implement OpenTelemetry metrics
    }

    actual fun histogram(name: String, value: Double, attrs: Attributes) {
        // TODO: Implement OpenTelemetry metrics
    }

    actual fun gauge(name: String, value: Double, attrs: Attributes) {
        // TODO: Implement OpenTelemetry metrics
    }

    actual fun setCommonAttributes(attrs: Attributes) {
        commonAttributes = attrs
    }

    actual fun shutdown() {
        // TODO: Implement graceful shutdown of OpenTelemetry providers
    }

    actual val crashAndVitals: CrashAndVitals = CrashAndVitalsProvider.instance
}
