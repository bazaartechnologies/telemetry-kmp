package com.bazaar.telemetry

import android.os.Build
import android.util.Log

actual object CrashAndVitalsProvider {
    actual val instance: CrashAndVitals = AndroidCrashAndVitals
}

object AndroidCrashAndVitals : CrashAndVitals {
    private val vitals = mutableMapOf<String, String>()

    override fun recordCrash(throwable: Throwable) {
        Log.e("CrashAndVitals", "Crash recorded", throwable)
        // Here you could integrate with Firebase Crashlytics or similar
    }

    override fun recordVital(key: String, value: String) {
        vitals[key] = value
    }

    override fun getVitals(): Map<String, String> {
        // Example: add some system vitals
        vitals["device_model"] = Build.MODEL ?: "unknown"
        vitals["android_version"] = Build.VERSION.RELEASE ?: "unknown"
        return vitals.toMap()
    }
} 