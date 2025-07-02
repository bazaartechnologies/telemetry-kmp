package com.bazaar.telemetry

/**
 * Common interface for crash reporting and app vitals.
 */
interface CrashAndVitals {
    /** Record a crash/exception. */
    fun recordCrash(throwable: Throwable)
    /** Record a vital key-value pair. */
    fun recordVital(key: String, value: String)
    /** Get all vitals as a map. */
    fun getVitals(): Map<String, String>
}

/**
 * Platform-specific implementation provider for crash and vitals.
 */
expect object CrashAndVitalsProvider {
    val instance: CrashAndVitals
} 