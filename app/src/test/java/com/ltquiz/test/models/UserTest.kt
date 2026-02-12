package com.ltquiz.test.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class UserTest {
    
    private val validDeviceInfo = DeviceInfo("Samsung A54", "User's Samsung A54")
    private val validUserId = "550e8400-e29b-41d4-a716-446655440000" // UUID format
    private val validDisplayName = "John's Phone"
    
    @Test
    fun `create user with valid data succeeds`() {
        // When
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        
        // Then
        assertEquals(validUserId, user.userId)
        assertEquals(validDisplayName, user.displayName)
        assertEquals(validDeviceInfo, user.deviceInfo)
    }
    
    @Test
    fun `create user trims display name`() {
        // When
        val user = User.create(validUserId, "  John's Phone  ", validDeviceInfo)
        
        // Then
        assertEquals("John's Phone", user.displayName)
    }
    
    @Test
    fun `create user with blank userId throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create("", validDisplayName, validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with whitespace-only userId throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create("   ", validDisplayName, validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with short userId throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create("short", validDisplayName, validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with blank display name throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create(validUserId, "", validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with whitespace-only display name throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create(validUserId, "   ", validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with too long display name throws exception`() {
        // Given
        val longName = "a".repeat(51) // Exceeds MAX_DISPLAY_NAME_LENGTH
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create(validUserId, longName, validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with maximum length display name succeeds`() {
        // Given
        val maxLengthName = "a".repeat(50) // Exactly MAX_DISPLAY_NAME_LENGTH
        
        // When
        val user = User.create(validUserId, maxLengthName, validDeviceInfo)
        
        // Then
        assertEquals(maxLengthName, user.displayName)
    }
    
    @Test
    fun `create user with invalid characters throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.create(validUserId, "John@#$%", validDeviceInfo)
        }
    }
    
    @Test
    fun `create user with valid special characters succeeds`() {
        // Given
        val nameWithValidChars = "John's Phone-2024_v1.0"
        
        // When
        val user = User.create(validUserId, nameWithValidChars, validDeviceInfo)
        
        // Then
        assertEquals(nameWithValidChars, user.displayName)
    }
    
    @Test
    fun `updateDisplayName with valid name succeeds`() {
        // Given
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        val newName = "Updated Name"
        
        // When
        val updatedUser = user.updateDisplayName(newName)
        
        // Then
        assertEquals(newName, updatedUser.displayName)
        assertEquals(validUserId, updatedUser.userId) // Other fields unchanged
        assertEquals(validDeviceInfo, updatedUser.deviceInfo)
    }
    
    @Test
    fun `updateDisplayName trims whitespace`() {
        // Given
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        
        // When
        val updatedUser = user.updateDisplayName("  Updated Name  ")
        
        // Then
        assertEquals("Updated Name", updatedUser.displayName)
    }
    
    @Test
    fun `updateDisplayName with invalid name throws exception`() {
        // Given
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            user.updateDisplayName("")
        }
    }
    
    @Test
    fun `isValid returns true for valid user`() {
        // Given
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        
        // When
        val result = user.isValid()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isValid returns false for user with invalid data`() {
        // Given - Create user with invalid data by bypassing validation
        val invalidUser = User(
            userId = "short", // Too short
            displayName = validDisplayName,
            deviceInfo = validDeviceInfo
        )
        
        // When
        val result = invalidUser.isValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `data class properties work correctly`() {
        // Given
        val user1 = User.create(validUserId, validDisplayName, validDeviceInfo)
        val user2 = User.create(validUserId, validDisplayName, validDeviceInfo)
        val user3 = User.create(validUserId, "Different Name", validDeviceInfo)
        
        // Then
        assertEquals(user1, user2) // Same data should be equal
        assertNotEquals(user1, user3) // Different data should not be equal
        assertEquals(user1.hashCode(), user2.hashCode()) // Same hash codes
        assertTrue(user1.toString().contains(validDisplayName)) // toString includes data
    }
    
    @Test
    fun `copy functionality works correctly`() {
        // Given
        val user = User.create(validUserId, validDisplayName, validDeviceInfo)
        val newDeviceInfo = DeviceInfo("iPhone 15", "User's iPhone 15")
        
        // When
        val copiedUser = user.copy(deviceInfo = newDeviceInfo)
        
        // Then
        assertEquals(validUserId, copiedUser.userId)
        assertEquals(validDisplayName, copiedUser.displayName)
        assertEquals(newDeviceInfo, copiedUser.deviceInfo)
    }
}