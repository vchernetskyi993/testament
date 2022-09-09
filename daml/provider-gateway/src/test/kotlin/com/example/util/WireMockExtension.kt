package com.example.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector


@Target(AnnotationTarget.FIELD)
annotation class InjectWireMock

class WireMockExtension : QuarkusTestResourceLifecycleManager {
    companion object {
        private const val USERNAME = "test-user"
        private const val PASSWORD = "test-password"
        const val AUTH_TOKEN = "test-auth-token"
    }

    private val wireMockServer = WireMockServer(options().dynamicPort())

    override fun start(): Map<String, String> {
        wireMockServer.start()

        WireMock.configureFor(wireMockServer.port())

        mockAuth()

        return mapOf(
            "quarkus.rest-client.auth.url" to wireMockServer.baseUrl(),
            "quarkus.rest-client.json.url" to wireMockServer.baseUrl(),
            "daml.ledger.auth.username" to USERNAME,
            "daml.ledger.auth.password" to PASSWORD,
        )
    }

    override fun stop() {
        wireMockServer.stop()
    }

    override fun inject(testInjector: TestInjector) {
        testInjector.injectIntoFields(
            wireMockServer,
            TestInjector.Annotated(
                InjectWireMock::class.java,
            )
        )
    }
}