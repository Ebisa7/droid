package com.ltquiz.test.managers

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCManagerImpl @Inject constructor(
    private val context: Context
) : WebRTCManager {

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        initializePeerConnectionFactory()
    }

    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val _connectionStates = MutableStateFlow<Map<String, PeerConnection.PeerConnectionState>>(emptyMap())
    override val connectionStates: StateFlow<Map<String, PeerConnection.PeerConnectionState>> = _connectionStates

    private val rtcConfig = PeerConnection.RTCConfiguration(
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    ).apply {
        bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
    }

    override fun createPeerConnection(
        participantId: String,
        onIceCandidate: (IceCandidate) -> Unit,
        onRemoteStream: (MediaStream) -> Unit
    ): Boolean {
        if (peerConnections.containsKey(participantId)) {
            return false
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { onIceCandidate(it) }
            }

            override fun onAddStream(stream: MediaStream?) {
                stream?.let { onRemoteStream(it) }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let { state ->
                    val currentStates = _connectionStates.value.toMutableMap()
                    currentStates[participantId] = state
                    _connectionStates.value = currentStates
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        }

        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        return if (peerConnection != null) {
            peerConnections[participantId] = peerConnection
            true
        } else {
            false
        }
    }

    override fun addIceCandidate(participantId: String, candidate: IceCandidate): Boolean {
        return peerConnections[participantId]?.addIceCandidate(candidate) ?: false
    }

    override suspend fun createOffer(participantId: String): SessionDescription? {
        val peerConnection = peerConnections[participantId] ?: return null
        
        return try {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
            
            val sdp = peerConnection.createOffer(constraints)
            peerConnection.setLocalDescription(sdp)
            sdp
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createAnswer(participantId: String): SessionDescription? {
        val peerConnection = peerConnections[participantId] ?: return null
        
        return try {
            val constraints = MediaConstraints()
            val sdp = peerConnection.createAnswer(constraints)
            peerConnection.setLocalDescription(sdp)
            sdp
        } catch (e: Exception) {
            null
        }
    }

    override fun setRemoteDescription(participantId: String, sessionDescription: SessionDescription): Boolean {
        return try {
            peerConnections[participantId]?.setRemoteDescription(sessionDescription)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addLocalStream(participantId: String, stream: MediaStream): Boolean {
        return try {
            peerConnections[participantId]?.addStream(stream)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun removePeerConnection(participantId: String) {
        peerConnections[participantId]?.close()
        peerConnections.remove(participantId)
        
        val currentStates = _connectionStates.value.toMutableMap()
        currentStates.remove(participantId)
        _connectionStates.value = currentStates
    }

    override fun hasPeerConnection(participantId: String): Boolean {
        return peerConnections.containsKey(participantId)
    }

    override fun closeAllConnections() {
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        _connectionStates.value = emptyMap()
    }

    private fun initializePeerConnectionFactory(): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(null, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(null))
            .createPeerConnectionFactory()
    }
}