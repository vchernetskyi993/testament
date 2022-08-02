package com.example.testament.flows

data class TestamentDataInput(
    val issuer: String,
    val inheritors: Map<String, Int>,
)

data class TestamentIssuerInput(
    val issuer: String,
)

data class GoldInput(
    val holder: String,
    val amount: String,
    val signers: Collection<String> = listOf(),
)
