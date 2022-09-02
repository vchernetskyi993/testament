package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val participant: String,
    val user: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    val token: String,
)
