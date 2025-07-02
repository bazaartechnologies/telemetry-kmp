package com.bazaar.telemetry

actual object CrashAndVitalsProvider {
    actual val instance: CrashAndVitals = IOSCrashAndVitals
}

object IOSCrashAndVitals : CrashAndVitals {
    private val vitals = mutableMapOf<String, String>()

    override fun recordCrash(throwable: Throwable) {
        // NSLog("Crash recorded: %@", throwable.message ?: "unknown")
        // Integrate with Sentry, Firebase Crashlytics, or similar if needed
    }

    override fun recordVital(key: String, value: String) {
        vitals[key] = value
    }

    override fun getVitals(): Map<String, String> {
        // Return only the vitals map to avoid native API calls in tests
        return vitals.toMap()
    }
} 