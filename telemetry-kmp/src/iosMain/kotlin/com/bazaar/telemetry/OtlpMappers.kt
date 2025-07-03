package com.bazaar.telemetry

import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.resource.v1.Resource
import io.opentelemetry.proto.trace.v1.Span
import okio.ByteString
import io.opentelemetry.proto.logs.v1.LogRecord
import io.opentelemetry.proto.logs.v1.SeverityNumber
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.NumberDataPoint
import io.opentelemetry.proto.metrics.v1.Sum
import io.opentelemetry.proto.metrics.v1.Gauge
import io.opentelemetry.proto.metrics.v1.Histogram
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint
import io.opentelemetry.proto.metrics.v1.AggregationTemporality
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.trace.v1.ResourceSpans
import io.opentelemetry.proto.trace.v1.ScopeSpans
import io.opentelemetry.proto.logs.v1.ResourceLogs
import io.opentelemetry.proto.logs.v1.ScopeLogs
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import io.opentelemetry.proto.metrics.v1.ScopeMetrics
import io.opentelemetry.proto.common.v1.InstrumentationScope

// TODO: Import actual generated OTLP classes from Wire.
// These will likely be in packages like:
// import opentelemetry.proto.common.v1.*
// import opentelemetry.proto.resource.v1.*
// import opentelemetry.proto.trace.v1.*
// import opentelemetry.proto.logs.v1.*
// import opentelemetry.proto.metrics.v1.*
// For now, we'll use placeholder type names prefixed with "Otlp" (e.g., OtlpKeyValue, OtlpSpan).
// Replace these with actual generated types once Wire build is successful.

// Placeholder for actual generated types (these are NOT real types, just for structure)
// typealias OtlpAnyValue = Any
// typealias OtlpKeyValue = Any
// typealias OtlpResource = Any
// typealias OtlpSpan = Any
// typealias OtlpSpanKind = Int // Replace with opentelemetry.proto.trace.v1.Span.SpanKind (enum)
// typealias OtlpStatus = Any // Replace with opentelemetry.proto.trace.v1.Status
// typealias OtlpStatusCode = Int // Replace with opentelemetry.proto.trace.v1.Status.StatusCode (enum)
// typealias OtlpLogRecord = Any // Replace with opentelemetry.proto.logs.v1.LogRecord
// typealias OtlpSeverityNumber = Int // Replace with opentelemetry.proto.logs.v1.SeverityNumber (enum)
// typealias OtlpMetric = Any // Replace with opentelemetry.proto.metrics.v1.Metric
// typealias OtlpNumberDataPoint = Any // Replace with opentelemetry.proto.metrics.v1.NumberDataPoint
// typealias OtlpHistogramDataPoint = Any // Replace with opentelemetry.proto.metrics.v1.HistogramDataPoint
// typealias OtlpAggregationTemporality = Int // Replace with opentelemetry.proto.metrics.v1.AggregationTemporality (enum)

// --- Time Conversion ---

/**
 * Converts a Kotlin Instant to OTLP timestamp (nanoseconds since Unix epoch).
 */
fun Long.millisToNanos(): Long {
    // Placeholder: System.currentTimeMillis() * 1_000_000 for example
    // Proper conversion from our time source to nanos needed.
    // OTLP expects nanoseconds since UNIX epoch.
    // If our internal representation is milliseconds, multiply by 1,000,000.
    return this * 1_000_000L
}

// --- Attribute and Resource Mapping ---

/**
 * Converts the library's [Attributes] to a list of OTLP [KeyValue].
 */
fun Attributes.toOtlpKeyValues(): List<KeyValue> =
    this.toMap().map { (key, value) ->
        val anyValue = when (value) {
            is String -> AnyValue(string_value = value)
            is Long -> AnyValue(int_value = value)
            is Int -> AnyValue(int_value = value.toLong())
            is Double -> AnyValue(double_value = value)
            is Boolean -> AnyValue(bool_value = value)
            else -> AnyValue(string_value = value.toString())
        }
        KeyValue(key = key, value_ = anyValue)
    }

/**
 * Creates an OTLP [Resource] object from config and common attributes.
 */
fun createOtlpResource(config: TelemetryConfig, commonAttributes: Attributes): Resource {
    val resourceAttributes = mutableListOf<KeyValue>()
    // Add service.name (mandatory)
    resourceAttributes.add(
        KeyValue(
            key = "service.name",
            value_ = AnyValue(string_value = config.serviceName)
        )
    )
    // Add other common attributes
    resourceAttributes.addAll(commonAttributes.toOtlpKeyValues())
    return Resource(attributes = resourceAttributes)
}


// --- Trace Mapping ---

/**
 * Converts library span data to an OTLP [Span].
 * This is a simplified example. Actual span data includes name, timestamps, parent ID, status, kind, events, links.
 */
fun mapToOtlpSpan(
    traceId: String, // Hex string, must be 16 bytes (32 hex chars)
    spanId: String,  // Hex string, must be 8 bytes (16 hex chars)
    parentSpanId: String? = null, // Hex string or null
    name: String,
    startTimeMillis: Long,
    endTimeMillis: Long,
    attributes: Attributes,
    status: String, // Simplified: "OK", "ERROR"
    kind: Int // Should map to Span.SpanKind
): Span {
    val traceIdBytes = try { ByteString.decodeHex(traceId) } catch (_: Exception) { ByteString.EMPTY }
    val spanIdBytes = try { ByteString.decodeHex(spanId) } catch (_: Exception) { ByteString.EMPTY }
    val parentSpanIdBytes = parentSpanId?.let { try { ByteString.decodeHex(it) } catch (_: Exception) { ByteString.EMPTY } } ?: ByteString.EMPTY
    return Span(
        trace_id = traceIdBytes,
        span_id = spanIdBytes,
        parent_span_id = parentSpanIdBytes,
        name = name,
        kind = Span.SpanKind.fromValue(kind) ?: Span.SpanKind.SPAN_KIND_INTERNAL,
        start_time_unix_nano = startTimeMillis.millisToNanos(),
        end_time_unix_nano = endTimeMillis.millisToNanos(),
        attributes = attributes.toOtlpKeyValues(),
        // status, events, links, etc. can be added as needed
    )
}

// TODO: Implement mapToOtlpStatus, mapToOtlpSpanKind helper functions


// --- Log Record Mapping ---

/**
 * Converts library log data to an OTLP [OtlpLogRecord].
 */
fun mapToOtlpLogRecord(
    timestampMillis: Long,
    severity: LogLevel,
    message: String,
    attributes: Attributes,
    traceId: String? = null, // For correlating with spans
    spanId: String? = null   // For correlating with spans
): LogRecord {
    val traceIdBytes = traceId?.let { try { ByteString.decodeHex(it) } catch (_: Exception) { ByteString.EMPTY } } ?: ByteString.EMPTY
    val spanIdBytes = spanId?.let { try { ByteString.decodeHex(it) } catch (_: Exception) { ByteString.EMPTY } } ?: ByteString.EMPTY
    return LogRecord(
        time_unix_nano = timestampMillis.millisToNanos(),
        observed_time_unix_nano = System.currentTimeMillis().millisToNanos(),
        severity_number = when (severity) {
            LogLevel.DEBUG -> SeverityNumber.SEVERITY_NUMBER_DEBUG
            LogLevel.INFO -> SeverityNumber.SEVERITY_NUMBER_INFO
            LogLevel.WARN -> SeverityNumber.SEVERITY_NUMBER_WARN
            LogLevel.ERROR -> SeverityNumber.SEVERITY_NUMBER_ERROR
        },
        severity_text = severity.name,
        body = AnyValue(string_value = message),
        attributes = attributes.toOtlpKeyValues(),
        trace_id = traceIdBytes,
        span_id = spanIdBytes
    )
}

// TODO: Implement mapToOtlpSeverityNumber helper function


// --- Metric Mapping ---
// Metrics are more complex, involving Metric, Sum, Gauge, Histogram types and DataPoints.

/**
 * Converts a counter metric to an OTLP [OtlpMetric] (specifically a Sum).
 */
fun mapCounterToOtlpMetric(
    name: String,
    value: Long,
    timestampMillis: Long,
    attributes: Attributes,
    startTimeMillis: Long
): Metric {
    val dataPoint = NumberDataPoint(
        attributes = attributes.toOtlpKeyValues(),
        start_time_unix_nano = startTimeMillis.millisToNanos(),
        time_unix_nano = timestampMillis.millisToNanos(),
        as_int = value
    )
    val sum = Sum(
        data_points = listOf(dataPoint),
        aggregation_temporality = AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE,
        is_monotonic = true
    )
    return Metric(
        name = name,
        sum = sum
    )
}

/**
 * Converts a gauge metric to an OTLP [OtlpMetric].
 */
fun mapGaugeToOtlpMetric(
    name: String,
    value: Double,
    timestampMillis: Long,
    attributes: Attributes
): Metric {
    val dataPoint = NumberDataPoint(
        attributes = attributes.toOtlpKeyValues(),
        time_unix_nano = timestampMillis.millisToNanos(),
        as_double = value
    )
    val gauge = Gauge(
        data_points = listOf(dataPoint)
    )
    return Metric(
        name = name,
        gauge = gauge
    )
}

/**
 * Converts a histogram metric to an OTLP [OtlpMetric].
 * This is highly simplified. Real histograms have bucket counts and explicit bounds.
 */
fun mapHistogramToOtlpMetric(
    name: String,
    value: Double, // A single observation
    timestampMillis: Long,
    attributes: Attributes,
    startTimeMillis: Long
): Metric {
    val dataPoint = HistogramDataPoint(
        attributes = attributes.toOtlpKeyValues(),
        start_time_unix_nano = startTimeMillis.millisToNanos(),
        time_unix_nano = timestampMillis.millisToNanos(),
        count = 1,
        sum = value
        // For a real histogram, bucket_counts and explicit_bounds would be set
    )
    val histogram = Histogram(
        data_points = listOf(dataPoint),
        aggregation_temporality = AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE
    )
    return Metric(
        name = name,
        histogram = histogram
    )
}

// --- Request Wrappers ---
// OTLP data is sent in request messages like ExportTraceServiceRequest, etc.

fun createTraceExportRequest(spans: List<Span>, resource: Resource): ExportTraceServiceRequest {
    val scopeSpans = ScopeSpans(
        scope = InstrumentationScope(name = "com.bazaar.telemetry", version = "0.1.0"),
        spans = spans
    )
    val resourceSpans = ResourceSpans(
        resource = resource,
        scope_spans = listOf(scopeSpans)
    )
    return ExportTraceServiceRequest(resource_spans = listOf(resourceSpans))
}

fun createLogsExportRequest(logRecords: List<LogRecord>, resource: Resource): ExportLogsServiceRequest {
    val scopeLogs = ScopeLogs(
        scope = InstrumentationScope(name = "com.bazaar.telemetry", version = "0.1.0"),
        log_records = logRecords
    )
    val resourceLogs = ResourceLogs(
        resource = resource,
        scope_logs = listOf(scopeLogs)
    )
    return ExportLogsServiceRequest(resource_logs = listOf(resourceLogs))
}

fun createMetricsExportRequest(metrics: List<Metric>, resource: Resource): ExportMetricsServiceRequest {
    val scopeMetrics = ScopeMetrics(
        scope = InstrumentationScope(name = "com.bazaar.telemetry", version = "0.1.0"),
        metrics = metrics
    )
    val resourceMetrics = ResourceMetrics(
        resource = resource,
        scope_metrics = listOf(scopeMetrics)
    )
    return ExportMetricsServiceRequest(resource_metrics = listOf(resourceMetrics))
}

// Note: The actual generated Wire classes will have builders and specific field names.
// The ByteString type from okio (used by Wire) will be needed for IDs.
// Enums like SpanKind, StatusCode, SeverityNumber, AggregationTemporality will also be generated by Wire.
// This file is a detailed TO-DO list for the developer who will integrate the Wire-generated code.
// The developer will need to replace placeholder types and uncomment/implement the logic.
// Careful study of the OTLP specification and the generated Kotlin files from the .proto schemas is required.
// opentelemetry-proto version should align with the OTLP spec version being targeted.
// For example, see: https://github.com/open-telemetry/opentelemetry-proto/tree/main/opentelemetry/proto
