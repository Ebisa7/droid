package com.ltquiz.test.managers

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.ltquiz.test.models.DeviceInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class IdentityManagerImplTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var identityManager: IdentityManagerImpl
    
    @BeforeEach
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences("xsend_meet_identity", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        
        identityManager = IdentityManagerImpl(context)
    }
    
    @Test
    fun `getDisplayName returns custom name when set`() {
        // Given
        val customName = "John's Phone"
        every { sharedPreferences.getString("display_name", null) } returns customName
        
        // When
        val result = identityManager.getDisplayName()
        
        // Then
        assertEquals(customName, result)
    }
    
    @Test
    fun `getDisplayName returns default name when no custom name set`() {
        // Given
        every { sharedPreferences.getString("display_name", null) } returns null
        mockkStatic(Build::class)
        every { Build.MODEL } returns "Samsung A54"
        
        // When
        val result = identityManager.getDisplayName()
        
        // Then
        assertEquals("User's Samsung A54", result)
    }
    
    @Test
    fun `setDisplayName stores trimmed name in preferences`() {
        // Given
        val nameSlot = slot<String>()
        every { editor.putString("display_name", capture(nameSlot)) } returns editor
        
        // When
        identityManager.setDisplayName("  John's Phone  ")
        
        // Then
        verify { editor.putString("display_name", "John's Phone") }
        verify { editor.apply() }
    }
    
    @Test
    fun `getUserId returns existing user ID when available`() {
        // Given
        val existingUserId = "existing-uuid-123"
        every { sharedPreferences.getString("user_id", null) } returns existingUserId
        
        // When
        val result = identityManager.getUserId()
        
        // Then
        assertEquals(existingUserId, result)
    }
    
    @Test
    fun `getUserId generates and stores new UUID when none exists`() {
        // Given
        every { sharedPreferences.getString("user_id", null) } returns null
        val userIdSlot = slot<String>()
        every { editor.putString("user_id", capture(userIdSlot)) } returns editor
        
        // When
        val result = identityManager.getUserId()
        
        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Verify it's a valid UUID format
        assertDoesNotThrow { UUID.fromString(result) }
        verify { editor.putString("user_id", result) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getUserId returns same UUID on subsequent calls`() {
        // Given
        every { sharedPreferences.getString("user_id", null) } returns null andThen "generated-uuid"
        every { editor.putString(any(), any()) } returns editor
        
        // When
        val firstCall = identityManager.getUserId()
        val secondCall = identityManager.getUserId()
        
        // Then
        assertEquals("generated-uuid", secondCall)
    }
    
    @Test
    fun `getDeviceInfo returns correct device information`() {
        // Given
        mockkStatic(Build::class)
        every { Build.MODEL } returns "Pixel 7"
        
        // When
        val result = identityManager.getDeviceInfo()
        
        // Then
        assertEquals(DeviceInfo("Pixel 7", "User's Pixel 7"), result)
    }
    
    @Test
    fun `getDeviceInfo handles null device model`() {
        // Given
        mockkStatic(Build::class)
        every { Build.MODEL } returns null
        
        // When
        val result = identityManager.getDeviceInfo()
        
        // Then
        assertEquals(DeviceInfo("Unknown Device", "User's Unknown Device"), result)
    }
    
    @Test
    fun `default name generation follows correct format`() {
        // Given
        mockkStatic(Build::class)
        every { Build.MODEL } returns "iPhone 15"
        
        // When
        val deviceInfo = identityManager.getDeviceInfo()
        
        // Then
        assertEquals("User's iPhone 15", deviceInfo.defaultName)
        assertTrue(deviceInfo.defaultName.startsWith("User's "))
    }
    
    @Test
    fun `getCurrentUser returns valid User instance`() {
        // Given
        val userId = "test-uuid-123456789"
        val displayName = "Test User"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { sharedPreferences.getString("display_name", null) } returns displayName
        mockkStatic(Build::class)
        every { Build.MODEL } returns "Test Device"
        
        // When
        val user = identityManager.getCurrentUser()
        
        // Then
        assertEquals(userId, user.userId)
        assertEquals(displayName, user.displayName)
        assertEquals("Test Device", user.deviceInfo.model)
        assertEquals("User's Test Device", user.deviceInfo.defaultName)
        assertTrue(user.isValid())
    }
    
    @Test
    fun `getCurrentUser with default display name`() {
        // Given
        val userId = "test-uuid-123456789"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { sharedPreferences.getString("display_name", null) } returns null // No custom name
        mockkStatic(Build::class)
        every { Build.MODEL } returns "Samsung Galaxy"
        
        // When
        val user = identityManager.getCurrentUser()
        
        // Then
        assertEquals(userId, user.userId)
        assertEquals("User's Samsung Galaxy", user.displayName) // Should use default name
        assertEquals("Samsung Galaxy", user.deviceInfo.model)
    }
}