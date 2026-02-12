package com.ltquiz.test.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DeviceInfoTest {
    
    @Test
    fun `DeviceInfo creation with valid data succeeds`() {
        // Given
        val model = "Samsung A54"
        val defaultName = "User's Samsung A54"
        
        // When
        val deviceInfo = DeviceInfo(model, defaultName)
        
        // Then
        assertEquals(model, deviceInfo.model)
        assertEquals(defaultName, deviceInfo.defaultName)
    }
    
    @Test
    fun `DeviceInfo handles empty model`() {
        // Given
        val model = ""
        val defaultName = "User's Unknown Device"
        
        // When
        val deviceInfo = DeviceInfo(model, defaultName)
        
        // Then
        assertEquals(model, deviceInfo.model)
        assertEquals(defaultName, deviceInfo.defaultName)
    }
    
    @Test
    fun `DeviceInfo handles special characters in model`() {
        // Given
        val model = "iPhone 15 Pro Max (256GB)"
        val defaultName = "User's iPhone 15 Pro Max (256GB)"
        
        // When
        val deviceInfo = DeviceInfo(model, defaultName)
        
        // Then
        assertEquals(model, deviceInfo.model)
        assertEquals(defaultName, deviceInfo.defaultName)
    }
    
    @Test
    fun `DeviceInfo data class equality works correctly`() {
        // Given
        val deviceInfo1 = DeviceInfo("Pixel 7", "User's Pixel 7")
        val deviceInfo2 = DeviceInfo("Pixel 7", "User's Pixel 7")
        val deviceInfo3 = DeviceInfo("Pixel 8", "User's Pixel 8")
        
        // Then
        assertEquals(deviceInfo1, deviceInfo2) // Same data should be equal
        assertNotEquals(deviceInfo1, deviceInfo3) // Different data should not be equal
        assertEquals(deviceInfo1.hashCode(), deviceInfo2.hashCode()) // Same hash codes
    }
    
    @Test
    fun `DeviceInfo toString contains model and defaultName`() {
        // Given
        val model = "OnePlus 11"
        val defaultName = "User's OnePlus 11"
        val deviceInfo = DeviceInfo(model, defaultName)
        
        // When
        val toString = deviceInfo.toString()
        
        // Then
        assertTrue(toString.contains(model))
        assertTrue(toString.contains(defaultName))
    }
    
    @Test
    fun `DeviceInfo copy functionality works correctly`() {
        // Given
        val original = DeviceInfo("Original Model", "User's Original Model")
        
        // When
        val copied = original.copy(model = "New Model")
        
        // Then
        assertEquals("New Model", copied.model)
        assertEquals("User's Original Model", copied.defaultName) // Unchanged
        assertNotEquals(original, copied)
    }
    
    @Test
    fun `DeviceInfo handles long model names`() {
        // Given
        val longModel = "Samsung Galaxy S24 Ultra 5G Enterprise Edition (1TB Storage)"
        val defaultName = "User's $longModel"
        
        // When
        val deviceInfo = DeviceInfo(longModel, defaultName)
        
        // Then
        assertEquals(longModel, deviceInfo.model)
        assertEquals(defaultName, deviceInfo.defaultName)
    }
}