package com.bazaar.telemetry

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest

// Assuming OtlpMappers.kt contains the necessary mapping functions and typealiases
// for Wire-generated OTLP objects (OtlpSpan, OtlpLogRecord, OtlpMetric, OtlpResource)
// and request creators (createTraceExportRequest, etc.)

class CustomOtlpExporter(
    private val config: TelemetryConfig,
    initialCommonAttributes: Attributes,
    // Allow injecting OtlpHttpClient for testing, default to a new instance
    private val otlpHttpClient: OtlpHttpClient = OtlpHttpClient()
) {
    private val exporterScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()

    @Volatile
    private var isShutdown = false

    // --- Configuration for Batching and Retries ---
    // These could also be passed in or part of TelemetryConfig if more flexibility is needed
    private val batchSizeLimit = 100 // Max items per batch
    private val exportInterval: Duration = 5.seconds
    private val maxRetries = 3
    private val initialRetryDelay: Duration = 1.seconds

    // --- Buffers for Telemetry Data ---
    private val spanBuffer = mutableListOf<OtlpSpan>()
    private val logRecordBuffer = mutableListOf<OtlpLogRecord>()
    private val metricBuffer = mutableListOf<OtlpMetric>()

    // --- OTLP Resource ---
    private val otlpResource: OtlpResource by lazy {
        // This still uses the config and initialCommonAttributes passed to CustomOtlpExporter
        createOtlpResource(config, initialCommonAttributes)
    }

    private var periodicExportJob: Job? = null

    fun start() {
        if (isShutdown) {
            println("Exporter is shutdown, cannot start.")
            return
        }
        periodicExportJob = exporterScope.launch {
            while (isActive && !isShutdown) {
                delay(exportInterval)
                flushAllBuffers(triggeredByInterval = true)
            }
        }
        println("CustomOtlpExporter started. Export interval: $exportInterval, Batch size: $batchSizeLimit")
    }

    suspend fun addSpan(span: OtlpSpan) {
        if (isShutdown) return
        val needsFlush = mutex.withLock {
            spanBuffer.add(span)
            spanBuffer.size >= batchSizeLimit
        }
        if (needsFlush) {
            exporterScope.launch { flushSpansOnly() }
        }
    }

    suspend fun addLogRecord(logRecord: OtlpLogRecord) {
        if (isShutdown) return
        val needsFlush = mutex.withLock {
            logRecordBuffer.add(logRecord)
            logRecordBuffer.size >= batchSizeLimit
        }
        if (needsFlush) {
            exporterScope.launch { flushLogRecordsOnly() }
        }
    }

    suspend fun addMetric(metric: OtlpMetric) {
        if (isShutdown) return
        val needsFlush = mutex.withLock {
            metricBuffer.add(metric)
            metricBuffer.size >= batchSizeLimit
        }
        if (needsFlush) {
            exporterScope.launch { flushMetricsOnly() }
        }
    }

    private suspend fun flushAllBuffers(triggeredByInterval: Boolean = false) {
        if (isShutdown && !triggeredByInterval) { // Allow final flush on shutdown
             println("Exporter is shutdown. Skipping flush.")
             return
        }
        println("Flushing all telemetry buffers...")
        flushSpansOnly()
        flushLogRecordsOnly()
        flushMetricsOnly()
    }

    private suspend fun flushSpansOnly() {
        val spansToExport = mutex.withLock {
            if (spanBuffer.isEmpty()) return@withLock null
            val list = ArrayList(spanBuffer)
            spanBuffer.clear()
            list
        } ?: return

        println("Attempting to export [1m${spansToExport.size}[0m spans.")
        val traceRequest: ExportTraceServiceRequest = createTraceExportRequest(spansToExport, otlpResource)
        val requestBytes = traceRequest.encodeByteString().toByteArray()
        if (requestBytes.isEmpty() && spansToExport.isNotEmpty()) {
            println("WARN: Span export serialization produced empty bytes.")
            return
        } else if (requestBytes.isEmpty() && spansToExport.isEmpty()) {
            return
        }
        otlpHttpClient.sendWithRetries("${config.endpoint}/v1/traces", requestBytes, "Traces", maxRetries, initialRetryDelay)
    }

    private suspend fun flushLogRecordsOnly() {
        val logsToExport = mutex.withLock {
            if (logRecordBuffer.isEmpty()) return@withLock null
            val list = ArrayList(logRecordBuffer)
            logRecordBuffer.clear()
            list
        } ?: return

        println("Attempting to export [1m${logsToExport.size}[0m log records.")
        val logsRequest: ExportLogsServiceRequest = createLogsExportRequest(logsToExport, otlpResource)
        val requestBytes = logsRequest.encodeByteString().toByteArray()
        if (requestBytes.isEmpty() && logsToExport.isNotEmpty()) {
            println("WARN: Log export serialization produced empty bytes.")
            return
        } else if (requestBytes.isEmpty() && logsToExport.isEmpty()) {
            return
        }
        otlpHttpClient.sendWithRetries("${config.endpoint}/v1/logs", requestBytes, "Logs", maxRetries, initialRetryDelay)
    }

    private suspend fun flushMetricsOnly() {
        val metricsToExport = mutex.withLock {
            if (metricBuffer.isEmpty()) return@withLock null
            val list = ArrayList(metricBuffer)
            metricBuffer.clear()
            list
        } ?: return

        println("Attempting to export [1m${metricsToExport.size}[0m metrics.")
        val metricsRequest: ExportMetricsServiceRequest = createMetricsExportRequest(metricsToExport, otlpResource)
        val requestBytes = metricsRequest.encodeByteString().toByteArray()
        if (requestBytes.isEmpty() && metricsToExport.isNotEmpty()) {
            println("WARN: Metric export serialization produced empty bytes.")
            return
        } else if (requestBytes.isEmpty() && metricsToExport.isEmpty()) {
            return
        }
        otlpHttpClient.sendWithRetries("${config.endpoint}/v1/metrics", requestBytes, "Metrics", maxRetries, initialRetryDelay)
    }

    // sendWithRetries is now part of OtlpHttpClient class.

    suspend fun shutdown() {
        if (isShutdown) return
        isShutdown = true
        println("CustomOtlpExporter shutting down...")
        periodicExportJob?.cancelAndJoin() // Stop periodic exports and wait

        // Perform a final flush of all buffers
        flushAllBuffers(triggeredByInterval = false) // Pass false or a specific flag for shutdown flush

        otlpHttpClient.shutdown() // Shutdown the Ktor client instance
        exporterScope.cancel() // Cancel any other coroutines in the scope
        println("CustomOtlpExporter shutdown complete.")
    }
}

// Double.pow extension was moved or is accessible via OtlpHttpClient's context if needed by it.
// No longer needed here directly.
