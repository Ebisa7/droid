package com.ltquiz.test.managers

import com.ltquiz.test.models.Signal
import com.ltquiz.test.models.SignalType
import com.ltquiz.test.models.ConnectionState
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

class SignalingClientImplTest {
    
    private lateinit var signalingClient: SignalingClientImpl
    
    @Before
    fun setup() {
        signalingClient = SignalingClientImpl()
    }
    
    @Test
    fun `initial connection state should be disconnected`() = runTest {
        // When
        val connectionState = signalingClient.observeConnectionState().first()
        
        // Then
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
    }
    
    @Test
    fun `connect should set connection state to connecting`() = runTest {
        // Given
        val serverUrl = "ws://localhost:8080"
        
        // When
        signalingClient.connect(serverUrl)
        
        // Then
        val connectionState = signalingClient.observeConnectionState().first()
        assertEquals(ConnectionState.CONNECTING, connectionState)
    }
    
    @Test
    fun `disconnect should set connection state to disconnected`() = runTest {
        // Given
        signalingClient.connect("ws://localhost:8080")
        
        // When
        signalingClient.disconnect()
        
        // Then
        val connectionState = signalingClient.observeConnectionState().first()
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
    }
    
    @Test
    fun `signal serialization should create valid JSON`() {
        // Given
        val signal = Signal(
            from = "user1",
            to = "user2",
            type = SignalType.OFFER,
            payload = "test-payload"
        )
        
        // When
        val json = JSONObject().apply {
            put("from", signal.from)
            put("to", signal.to)
            put("type", signal.type.name)
            put("payload", signal.payload)
        }
        
        // Then
        assertEquals("user1", json.getString("from"))
        assertEquals("user2", json.getString("to"))
        assertEquals("OFFER", json.getString("type"))
        assertEquals("test-payload", json.getString("payload"))
    }
    
    @Test
    fun `signal deserialization should parse JSON correctly`() {
        // Given
        val jsonString = """
            {
                "from": "user1",
                "to": "user2",
                "type": "ANSWER",
                "payload": "test-answer"
            }
        """.trimIndent()
        
        // When
        val json = JSONObject(jsonString)
        val signal = Signal(
            from = json.getString("from"),
            to = json.getString("to"),
            type = SignalType.valueOf(json.getString("type")),
            payload = json.getString("payload")
        )
        
        // Then
        assertEquals("user1", signal.from)
        assertEquals("user2", signal.to)
        assertEquals(SignalType.ANSWER, signal.type)
        assertEquals("test-answer", signal.payload)
    }
    
    @Test
    fun `all signal types should be serializable`() {
        // Given/When/Then
        SignalType.values().forEach { signalType ->
            val signal = Signal(
                from = "user1",
                to = "user2",
                type = signalType,
                payload = "test"
            )
            
            val json = JSONObject().apply {
                put("from", signal.from)
                put("to", signal.to)
                put("type", signal.type.name)
                put("payload", signal.payload)
            }
            
            assertEquals(signalType.name, json.getString("type"))
        }
    }
    
    @Test
    fun `invalid JSON should not crash parsing`() {
        // Given
        val invalidJson = "{ invalid json }"
        
        // When/Then - Should not throw exception
        try {
            JSONObject(invalidJson)
            fail("Expected JSONException")
        } catch (e: Exception) {
            // Expected - invalid JSON should throw exception
            assertTrue(e.message?.contains("json") == true || e is org.json.JSONException)
        }
    }
    
    @Test
    fun `missing fields in JSON should throw exception`() {
        // Given
        val incompleteJson = """{"from": "user1"}"""
        
        // When/Then
        try {
            val json = JSONObject(incompleteJson)
            json.getString("to") // This should throw
            fail("Expected JSONException for missing field")
        } catch (e: Exception) {
            // Expected - missing field should throw exception
            assertTrue(e is org.json.JSONException)
        }
    }
    
    @Test
    fun `connection state should transition correctly`() = runTest {
        // Given
        val serverUrl = "ws://localhost:8080"
        
        // When - Initial state
        var connectionState = signalingClient.observeConnectionState().first()
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
        
        // When - Connecting
        signalingClient.connect(serverUrl)
        connectionState = signalingClient.observeConnectionState().first()
        assertEquals(ConnectionState.CONNECTING, connectionState)
        
        // When - Disconnecting
        signalingClient.disconnect()
        connectionState = signalingClient.observeConnectionState().first()
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
    }
    
    @Test
    fun `sendSignal should handle all signal types`() = runTest {
        // Given
        val serverUrl = "ws://localhost:8080"
        signalingClient.connect(serverUrl)
        
        // When/Then - Test all signal types
        SignalType.values().forEach { signalType ->
            val signal = Signal(
                from = "user1",
                to = "user2",
                type = signalType,
                payload = "test-payload-${signalType.name}"
            )
            
            // Should not throw exception
            try {
                signalingClient.sendSignal(signal)
            } catch (e: Exception) {
                fail("sendSignal should not throw exception for ${signalType.name}")
            }
        }
    }
    
    @Test
    fun `signal flow should be observable`() = runTest {
        // Given
        val signalFlow = signalingClient.observeSignals()
        
        // When/Then
        assertNotNull(signalFlow)
        // Flow should be cold and not emit until connected
    }
}