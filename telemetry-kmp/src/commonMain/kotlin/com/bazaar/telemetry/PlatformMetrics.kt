package com.bazaar.telemetry

/**
 * Platform-specific metrics helpers.
 */
expect fun getBatteryLevel(): Int?
/** Returns the current network type as a string (e.g., "wifi", "cellular", etc.) */
expect fun getNetworkType(): String?
/** Returns the used storage space in bytes. */
expect fun getUsedStorageSpace(): Long?
/** Returns the device model name. */
expect fun getDeviceModel(): String?
/** Returns the screen density in DPI. */
expect fun getScreenDensity(): Int? 