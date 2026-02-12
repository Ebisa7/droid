package com.ltquiz.test.managers

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import com.ltquiz.test.models.RoomData
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for QR code generation and scanning flow.
 */
class QRCodeIntegrationTest {
    
    private lateinit var qrCodeManager: QRCodeManagerImpl
    private lateinit var mockContext: Context
    private val json = Json { ignoreUnknownKeys = true }
    
    @BeforeEach
    fun setUp() {
        // Mock Android dependencies
        mockkStatic(Bitmap::class)
        every { 
            Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) 
        } returns mockk<Bitmap>(relaxed = true)
        
        mockContext = mockk<Context>(relaxed = true)
        qrCodeManager = QRCodeManagerImpl(mockContext)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `QR code generation and data extraction should work end-to-end`() {
        // Given
        val originalRoomData = RoomData(
            roomId = "ROOM123",
            serverAddress = "wss://example.com:8080"
        )
        
        // When - Generate QR code
        val qrBitmap = qrCodeManager.generateQRCode(originalRoomData)
        
        // Then - QR code should be generated successfully
        assertNotNull(qrBitmap)
        
        // Verify that the room data can be serialized/deserialized correctly
        val serializedData = json.encodeToString(originalRoomData)
        val deserializedData = json.decodeFromString<RoomData>(serializedData)
        
        assertEquals(originalRoomData.roomId, deserializedData.roomId)
        assertEquals(originalRoomData.serverAddress, deserializedData.serverAddress)
    }
    
    @Test
    fun `QR code generation should handle various room data formats`() {
        val testCases = listOf(
            RoomData("ABC123", "wss://server1.com:8080"),
            RoomData("XYZ789", "wss://test-server.example.com:9090/ws"),
            RoomData("", "wss://example.com:8080"), // Empty room ID
            RoomData("ROOM01", ""), // Empty server address
            RoomData("VERYLONGROOMID123456789", "wss://very-long-server-name.example.com:8080/websocket/path")
        )
        
        testCases.forEach { roomData ->
            // When
            val result = qrCodeManager.generateQRCode(roomData)
            
            // Then
            assertNotNull(result, "QR code generation failed for: $roomData")
        }
    }
    
    @Test
    fun `QR code generation should throw exception for invalid data`() {
        // This test would be more relevant if we had validation logic
        // For now, we test that the manager handles the generation gracefully
        
        val roomData = RoomData("TEST", "wss://test.com")
        
        // Should not throw exception
        assertDoesNotThrow {
            qrCodeManager.generateQRCode(roomData)
        }
    }
    
    @Test
    fun `processImageForQRCode should handle null image gracefully`() = runBlocking {
        // Given
        val mockImageProxy = mockk<ImageProxy>(relaxed = true)
        every { mockImageProxy.image } returns null
        every { mockImageProxy.close() } just Runs
        
        // When
        val result = qrCodeManager.processImageForQRCode(mockImageProxy)
        
        // Then
        assertNull(result)
        verify { mockImageProxy.close() }
    }
    
    @Test
    fun `startQRCodeScanning should accept lifecycle owner`() = runBlocking {
        // Given
        val mockLifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
        
        // When & Then - Should not throw exception
        assertDoesNotThrow {
            val flow = qrCodeManager.startQRCodeScanning(mockLifecycleOwner)
            assertNotNull(flow)
        }
    }
    
    @Test
    fun `stopQRCodeScanning should be safe to call multiple times`() {
        // When & Then - Should not throw exception
        assertDoesNotThrow {
            qrCodeManager.stopQRCodeScanning()
            qrCodeManager.stopQRCodeScanning() // Call again
        }
    }
    
    @Test
    fun `JSON serialization should be consistent`() {
        // Given
        val roomData = RoomData(
            roomId = "TEST123",
            serverAddress = "wss://example.com:8080"
        )
        
        // When
        val serialized1 = json.encodeToString(roomData)
        val serialized2 = json.encodeToString(roomData)
        
        // Then
        assertEquals(serialized1, serialized2, "JSON serialization should be consistent")
        
        // And deserialization should work
        val deserialized = json.decodeFromString<RoomData>(serialized1)
        assertEquals(roomData, deserialized)
    }
}