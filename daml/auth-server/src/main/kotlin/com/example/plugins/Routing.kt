package com.example.plugins

import com.example.NodeSettings
import com.example.jwks
import com.example.jwt
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting(nodeSettings: NodeSettings) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        post("/authenticate") {
            call.respond(mapOf("token" to jwt(nodeSettings)))
        }
        get("/jwks") {
            call.respondText(jwks.toString(), ContentType.Application.Json)
        }
    }
}
