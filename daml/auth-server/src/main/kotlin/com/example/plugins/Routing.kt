package com.example.plugins

import com.example.Credentials
import com.example.TokenResponse
import com.example.jwks
import com.example.jwt
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting(credentials: Credentials) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        post("/authenticate") {
            val body = call.receive<Credentials>()
            when {
                body != credentials -> call.respond(HttpStatusCode.Unauthorized)
                else -> call.respond(TokenResponse(jwt(credentials)))
            }
        }
        get("/jwks") {
            call.respondText(jwks.toString(), ContentType.Application.Json)
        }
    }
}
