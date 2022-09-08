package com.example.util

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import org.grpcmock.GrpcMock

annotation class InjectGrpcMock

class GrpcMockExtension : QuarkusTestResourceLifecycleManager {

    private val grpcMock = GrpcMock.grpcMock().build()

    override fun start(): Map<String, String> {
        grpcMock.start()

        return mapOf(
            "daml.ledger.host" to "localhost",
            "daml.ledger.port" to "${grpcMock.port}",
        )
    }

    override fun stop() {
        grpcMock.stop()
    }

    override fun inject(testInjector: TestInjector) {
        testInjector.injectIntoFields(
            grpcMock,
            TestInjector.AnnotatedAndMatchesType(
                InjectGrpcMock::class.java,
                GrpcMock::class.java
            )
        )
    }
}