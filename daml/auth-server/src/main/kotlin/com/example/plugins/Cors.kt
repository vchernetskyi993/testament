package com.example.plugins

import com.example.getCors
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

data class CorsConf(
    val hosts: List<String>,
)

fun Application.configureCors() {
    val config = environment.config.getCors()
    install(CORS) {
        config.hosts.forEach { allowHost(it) }
        allowHeader(HttpHeaders.ContentType)
    }
}
