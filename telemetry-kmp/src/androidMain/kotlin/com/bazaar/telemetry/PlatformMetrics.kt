package com.bazaar.telemetry

import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.content.Context
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.DisplayMetrics
import android.content.res.Resources

// import com.bazaar.telemetry.TelemetryManager

// Helper to get application context (replace with your actual singleton if needed)
private fun getAppContext(): Context? = try {
    val clazz = Class.forName("android.app.ActivityThread")
    val method = clazz.getMethod("currentApplication")
    method.invoke(null) as? Context
} catch (e: Exception) { null }

actual fun getBatteryLevel(): Int? {
    val context = getAppContext() ?: return null
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

actual fun getNetworkType(): String? {
    val context = getAppContext() ?: return null
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
    val network = cm.activeNetwork ?: return null
    val capabilities = cm.getNetworkCapabilities(network) ?: return null
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
        else -> "other"
    }
}

actual fun getUsedStorageSpace(): Long? {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        (totalBlocks - availableBlocks) * blockSize
    } catch (e: Exception) {
        null
    }
}

actual fun getDeviceModel(): String? = Build.MODEL

actual fun getScreenDensity(): Int? {
    // Try to get from app context, fallback to system resources
    val context = getAppContext()
    val metrics: DisplayMetrics = context?.resources?.displayMetrics ?: Resources.getSystem().displayMetrics
    return metrics.densityDpi
} 