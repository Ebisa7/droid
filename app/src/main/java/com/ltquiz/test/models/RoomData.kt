package com.ltquiz.test.models

import kotlinx.serialization.Serializable

/**
 * Data contained in QR codes for room joining.
 */
@Serializable
data class RoomData(
    val roomId: String,
    val serverAddress: String
)