package com.example

data class Testament(
    val issuer: String,
    val inheritors: Map<String, Int>,
    val announced: Boolean = false,
    val executed: Boolean = false,
)
