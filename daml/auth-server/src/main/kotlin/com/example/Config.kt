package com.example

import com.example.plugins.CorsConf
import io.ktor.server.config.ApplicationConfig

fun ApplicationConfig.getCredentials() =
    Credentials(
        property("credentials.user").getString(),
        property("credentials.password").getString(),
    )

fun ApplicationConfig.getCors() =
    CorsConf(
        property("cors.hosts").getString().split(","),
    )
