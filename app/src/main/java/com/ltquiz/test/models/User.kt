package com.ltquiz.test.models

/**
 * Represents a user in the XSEND Meet application.
 * Contains user identity information and validation logic.
 */
data class User(
    val userId: String,
    val displayName: String,
    val deviceInfo: DeviceInfo
) {
    
    companion object {
        const val MIN_DISPLAY_NAME_LENGTH = 1
        const val MAX_DISPLAY_NAME_LENGTH = 50
        
        /**
         * Creates a User with validation.
         * @throws IllegalArgumentException if validation fails
         */
        fun create(userId: String, displayName: String, deviceInfo: DeviceInfo): User {
            validateUserId(userId)
            validateDisplayName(displayName)
            
            return User(
                userId = userId,
                displayName = displayName.trim(),
                deviceInfo = deviceInfo
            )
        }
        
        /**
         * Validates that the user ID is not empty and is a valid format.
         */
        private fun validateUserId(userId: String) {
            require(userId.isNotBlank()) { "User ID cannot be blank" }
            require(userId.length >= 10) { "User ID must be at least 10 characters long" }
        }
        
        /**
         * Validates that the display name meets requirements.
         */
        private fun validateDisplayName(displayName: String) {
            val trimmedName = displayName.trim()
            require(trimmedName.isNotBlank()) { "Display name cannot be blank" }
            require(trimmedName.length >= MIN_DISPLAY_NAME_LENGTH) { 
                "Display name must be at least $MIN_DISPLAY_NAME_LENGTH character long" 
            }
            require(trimmedName.length <= MAX_DISPLAY_NAME_LENGTH) { 
                "Display name cannot exceed $MAX_DISPLAY_NAME_LENGTH characters" 
            }
            require(!containsInvalidCharacters(trimmedName)) { 
                "Display name contains invalid characters" 
            }
        }
        
        /**
         * Checks if the display name contains invalid characters.
         * Allows letters, numbers, spaces, apostrophes, and basic punctuation.
         */
        private fun containsInvalidCharacters(displayName: String): Boolean {
            val validPattern = Regex("^[a-zA-Z0-9\\s'.,\\-_]+$")
            return !validPattern.matches(displayName)
        }
    }
    
    /**
     * Updates the display name with validation.
     * @return new User instance with updated display name
     * @throws IllegalArgumentException if validation fails
     */
    fun updateDisplayName(newDisplayName: String): User {
        validateDisplayName(newDisplayName)
        return copy(displayName = newDisplayName.trim())
    }
    
    /**
     * Checks if this user is valid according to current validation rules.
     */
    fun isValid(): Boolean {
        return try {
            validateUserId(userId)
            validateDisplayName(displayName)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}