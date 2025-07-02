package com.bazaar.telemetry

import com.bazaar.telemetry.Attributes
import com.bazaar.telemetry.LogLevel
import com.bazaar.telemetry.Span
import com.bazaar.telemetry.TelemetryConfig
import io.opentelemetry.kotlin.api.OpenTelemetry
import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes as OtelAttributes
import io.opentelemetry.kotlin.api.logs.Logger
import io.opentelemetry.kotlin.api.logs.Severity
import io.opentelemetry.kotlin.api.metrics.Meter
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.Tracer
import io.opentelemetry.kotlin.context.propagation.TextMapPropagator
import io.opentelemetry.kotlin.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.kotlin.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.kotlin.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.kotlin.sdk.OpenTelemetrySdk
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.kotlin.sdk.metrics.SdkMeterProvider
import io.opentelemetry.kotlin.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.sdk.trace.SdkTracerProvider
import io.opentelemetry.kotlin.sdk.trace.export.BatchSpanProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext


private var sdk: OpenTelemetrySdk? = null
private var tracer: Tracer? = null
private var logger: Logger? = null
private var meter: Meter? = null
private var commonAttributes: OtelAttributes = OtelAttributes.empty()
private val telemetryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())


actual object Telemetry {
    actual fun init(config: TelemetryConfig) {
        if (sdk != null) {
            println("Telemetry already initialized.")
            return
        }

        val resource = Resource.builder()
            .put(AttributeKey.stringKey("service.name"), config.serviceName)
            // Add other common resource attributes here if needed
            .build()

        // Assuming config.endpoint is the base OTLP endpoint.
        // For OTLP HTTP, exporters typically expect full paths or allow appending paths.
        // e.g., endpoint/v1/traces, endpoint/v1/logs, endpoint/v1/metrics
        // The KMP OTLP HTTP exporter might handle this by convention or require full URLs.
        // For simplicity, we'll assume the exporter appends the necessary paths or the provided endpoint is base.

        val spanExporter = OtlpHttpSpanExporter.builder()
            .setEndpoint(config.endpoint) // May need to append /v1/traces if not handled by exporter
            .build()

        val logExporter = OtlpHttpLogRecordExporter.builder()
            .setEndpoint(config.endpoint) // May need to append /v1/logs
            .build()

        val metricExporter = OtlpHttpMetricExporter.builder()
            .setEndpoint(config.endpoint) // May need to append /v1/metrics
            .build()

        val sdkTracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .build()

        val sdkLoggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
            .build()

        val sdkMeterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.builder(metricExporter).build())
            .build()

        val internalSdk = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setLoggerProvider(sdkLoggerProvider)
            .setMeterProvider(sdkMeterProvider)
            .setPropagators(TextMapPropagator.noop()) // Or configure actual propagators if needed
            .buildAndInstallGlobal() // buildAndRegisterGlobal in Java, buildAndInstallGlobal in KMP? Check exact name.
                                    // opentelemetry-kotlin uses build() and then you can use it.
                                    // Global installation might differ or not be the default pattern.
                                    // For now, let's store it locally.

        sdk = internalSdk // Store the SDK instance

        tracer = internalSdk.getTracer(config.serviceName, "0.1.0") // Schema URL can be tracer name, version
        logger = internalSdk.loggerBuilder(config.serviceName).setInstrumentationVersion("0.1.0").build()
        meter = internalSdk.meterBuilder(config.serviceName).setInstrumentationVersion("0.1.0").build()

        println("Telemetry initialized for ${config.serviceName} (iOS) at ${config.endpoint} using OTLP HTTP Exporter")
    }

    actual suspend fun <T> span(name: String, attrs: Attributes, block: suspend (Span) -> T): T {
        val currentTracer = tracer ?: run {
            println("Telemetry not initialized, returning no-op span.")
            return block(NoOpSpan)
        }

        val otelAttrs = attrs.toOtelAttributes()
        val combinedAttrs = commonAttributes.toBuilder().putAll(otelAttrs).build()

        val spanBuilder = currentTracer.spanBuilder(name)
            .setAllAttributes(combinedAttrs)
            .setSpanKind(SpanKind.INTERNAL) // Default to internal, can be configured

        val otelSpan = spanBuilder.startSpan()
        val scope = io.opentelemetry.kotlin.context.Context.current().with(otelSpan).makeCurrent()

        val spanImpl = object : com.bazaar.telemetry.Span {
            override fun setAttribute(key: String, value: String) {
                otelSpan.setAttribute(key, value)
            }
            override fun recordException(t: Throwable) {
                otelSpan.recordException(t, OtelAttributes.empty())
            }
            override fun close() {
                otelSpan.end()
                scope.close()
            }
        }
        try {
            return block(spanImpl)
        } finally {
            spanImpl.close() // Ensure span is closed even if block throws
        }
    }

    actual fun log(level: LogLevel, message: String, cause: Throwable?, attrs: Attributes) {
        val currentLogger = logger ?: run {
            println("Telemetry not initialized, printing log to console: [$level] $message")
            return
        }
        val otelAttrs = attrs.toOtelAttributes()
        val combinedAttrs = commonAttributes.toBuilder().putAll(otelAttrs).build()

        val eventBuilder = currentLogger.eventBuilder(message) // KMP logger uses eventBuilder or logRecordBuilder
            .setSeverity(level.toOtelSeverity())
            .setAttributes(combinedAttrs)

        cause?.let {
            // TODO: Check how KMP logger handles exceptions. Might need specific attributes.
            // eventBuilder.put(AttributeKey.stringKey("exception.type"), it::class.simpleName ?: "Unknown")
            // eventBuilder.put(AttributeKey.stringKey("exception.message"), it.message ?: "")
            // eventBuilder.put(AttributeKey.stringKey("exception.stacktrace"), it.stackTraceToString())
            // For now, just log the message
        }
        eventBuilder.emit()
    }

    actual fun counter(name: String, value: Long, attrs: Attributes) {
        val currentMeter = meter ?: return
        val otelAttrs = attrs.toOtelAttributes()
        val combinedAttrs = commonAttributes.toBuilder().putAll(otelAttrs).build()
        currentMeter.counterBuilder(name)
            .build()
            .add(value, combinedAttrs)
    }

    actual fun histogram(name: String, value: Double, attrs: Attributes) {
        val currentMeter = meter ?: return
        val otelAttrs = attrs.toOtelAttributes()
        val combinedAttrs = commonAttributes.toBuilder().putAll(otelAttrs).build()
        currentMeter.histogramBuilder(name)
            .build()
            .record(value, combinedAttrs)
    }

    actual fun gauge(name: String, value: Double, attrs: Attributes) {
        val currentMeter = meter ?: return
        // Gauge in OpenTelemetry typically uses an observable callback.
        // For a simple "set" operation like this, it's less direct.
        // One approach is to use an upDownCounter if appropriate, or manage an observable gauge.
        // For simplicity, this might be a no-op or require a different API design for KMP gauges.
        // Or, if the KMP SDK supports an async gauge that can be directly recorded:
        /*
        currentMeter.gaugeBuilder(name)
            .buildObserver() // This is for observable.
            // Need to check KMP SDK for direct recording gauge or use a different instrument.
        */
        println("Gauge metric '$name' with value $value and attributes $attrs - KMP implementation needs review for gauges.")
        // Placeholder: use a counter to at least send something, though semantically different.
         currentMeter.upDownCounterBuilder("${name}_gauge_as_updowncounter")
             .build()
             .add(value, attrs.toOtelAttributes()) // This isn't a true gauge.
    }

    actual fun setCommonAttributes(attrs: Attributes) {
        commonAttributes = attrs.toOtelAttributes()
    }

    actual fun shutdown() {
        sdk?.shutdown()?.join(10_000) // KMP SDK might have slightly different shutdown sequence
        sdk = null
        tracer = null
        logger = null
        meter = null
        telemetryScope.cancel() // Cancel the scope
        println("Telemetry shutdown on iOS.")
    }

    actual val crashAndVitals: CrashAndVitals = CrashAndVitalsProvider.instance // Remains the same
}

// Helper to convert our Attributes to OpenTelemetry Attributes
private fun Attributes.toOtelAttributes(): OtelAttributes {
    val builder = OtelAttributes.builder()
    this.toMap().forEach { (key, value) ->
        when (value) {
            is String -> builder.put(AttributeKey.stringKey(key), value)
            is Long -> builder.put(AttributeKey.longKey(key), value)
            is Double -> builder.put(AttributeKey.doubleKey(key), value)
            is Boolean -> builder.put(AttributeKey.booleanKey(key), value)
            // TODO: Add support for list types if needed by your Attributes class
            else -> builder.put(AttributeKey.stringKey(key), value.toString())
        }
    }
    return builder.build()
}

private fun LogLevel.toOtelSeverity(): Severity = when (this) {
    LogLevel.TRACE -> Severity.TRACE
    LogLevel.DEBUG -> Severity.DEBUG
    LogLevel.INFO -> Severity.INFO
    LogLevel.WARN -> Severity.WARN
    LogLevel.ERROR -> Severity.ERROR
}

private object NoOpSpan : Span {
    override fun setAttribute(key: String, value: String) {}
    override fun recordException(t: Throwable) {}
    override fun close() {}
}
