package com.example

import io.ktor.server.config.ApplicationConfig

data class NodeSettings(
    val node: String,
    val user: String,
    val password: String,
)

fun ApplicationConfig.getNodeSettings() =
    NodeSettings(
        property("node.name").getString(),
        property("node.user").getString(),
        property("node.password").getString(),
    )
