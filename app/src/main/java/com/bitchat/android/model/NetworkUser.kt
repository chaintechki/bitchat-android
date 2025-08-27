package com.bitchat.android.model

/**
 * Represents a peer available in the local network for chat or calls.
 * Contains basic identification and last known location.
 */
data class NetworkUser(
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double
)
