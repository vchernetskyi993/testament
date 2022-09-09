package com.example.util

import com.github.tomakehurst.wiremock.client.WireMock

private const val USERNAME = "test-user"
private const val PASSWORD = "test-password"
const val AUTH_TOKEN = "test-auth-token"

fun mockAuth() {
    WireMock.stubFor(
        WireMock.post("/authenticate")
            .withRequestBody(
                WireMock.equalToJson("{\"user\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")
            )
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"token\":\"${AUTH_TOKEN}\"}")
            )
    )
}
