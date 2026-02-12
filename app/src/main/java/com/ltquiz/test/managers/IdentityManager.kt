package com.ltquiz.test.managers

import com.ltquiz.test.models.DeviceInfo

/**
 * Manages device-based user identity without requiring accounts.
 * Handles device name, user ID generation, and device information.
 */
interface IdentityManager {
    /**
     * Gets the current display name for the user.
     */
    fun getDisplayName(): String
    
    /**
     * Sets a new display name for the user.
     */
    fun setDisplayName(name: String)
    
    /**
     * Gets the unique user ID for this device.
     */
    fun getUserId(): String
    
    /**
     * Gets device information including model and default name.
     */
    fun getDeviceInfo(): DeviceInfo
}