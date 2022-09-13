package com.example

import com.example.plugins.configureCors
import com.example.plugins.configureRouting
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureRouting(environment.config.getCredentials())
    configureCors()
}
