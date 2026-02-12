package com.ltquiz.test.managers

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.ltquiz.test.models.DeviceInfo
import com.ltquiz.test.models.User
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IdentityManager that manages device-based user identity
 * using SharedPreferences for local storage.
 */
@Singleton
class IdentityManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IdentityManager {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "xsend_meet_identity"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val DEFAULT_USER_PREFIX = "User's"
    }
    
    /**
     * Gets the current display name for the user.
     * If no custom name is set, returns the default generated name.
     */
    override fun getDisplayName(): String {
        return sharedPreferences.getString(KEY_DISPLAY_NAME, null) 
            ?: getDeviceInfo().defaultName
    }
    
    /**
     * Sets a new display name for the user and persists it locally.
     */
    override fun setDisplayName(name: String) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NAME, name.trim())
            .apply()
    }
    
    /**
     * Gets the unique user ID for this device.
     * Generates and persists a new UUID if one doesn't exist.
     */
    override fun getUserId(): String {
        val existingUserId = sharedPreferences.getString(KEY_USER_ID, null)
        if (existingUserId != null) {
            return existingUserId
        }
        
        // Generate new UUID and persist it
        val newUserId = UUID.randomUUID().toString()
        sharedPreferences.edit()
            .putString(KEY_USER_ID, newUserId)
            .apply()
        
        return newUserId
    }
    
    /**
     * Gets device information including model and generated default name.
     */
    override fun getDeviceInfo(): DeviceInfo {
        val deviceModel = Build.MODEL ?: "Unknown Device"
        val defaultName = generateDefaultName(deviceModel)
        
        return DeviceInfo(
            model = deviceModel,
            defaultName = defaultName
        )
    }
    
    /**
     * Creates a User instance with current identity information.
     * @return User instance with current userId, displayName, and deviceInfo
     */
    fun getCurrentUser(): User {
        return User.create(
            userId = getUserId(),
            displayName = getDisplayName(),
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Generates a default display name based on device model.
     * Format: "User's {DeviceModel}"
     */
    private fun generateDefaultName(deviceModel: String): String {
        return "$DEFAULT_USER_PREFIX $deviceModel"
    }
}