package com.example

import io.ktor.server.config.ApplicationConfig

fun ApplicationConfig.getCredentials() =
    Credentials(
        property("credentials.user").getString(),
        property("credentials.password").getString(),
    )
