package com.bazaar.telemetry

import platform.UIKit.UIDevice

actual fun getBatteryLevel(): Int? {
    val device = UIDevice.currentDevice
    // device.isBatteryMonitoringEnabled = true // Disabled for compatibility
    val level = device.batteryLevel
    return if (level >= 0) (level * 100).toInt() else null
}

actual fun getNetworkType(): String? {
    // iOS: Would require Reachability or NWPathMonitor; stub for now
    return "unknown"
}

actual fun getUsedStorageSpace(): Long? {
    // iOS: Would require FileManager/NSFileSystemAttributes; stub for now
    return null
}

actual fun getDeviceModel(): String? = UIDevice.currentDevice.model

actual fun getScreenDensity(): Int? {
    // iOS: Would require UIScreen.mainScreen.scale * 160; stub for now
    return null
} 