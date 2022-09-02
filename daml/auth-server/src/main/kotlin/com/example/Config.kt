package com.example

import io.ktor.server.config.ApplicationConfig

fun ApplicationConfig.getNodeSettings() =
    Credentials(
        property("credentials.participant").getString(),
        property("credentials.user").getString(),
        property("credentials.password").getString(),
    )
