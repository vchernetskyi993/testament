package com.example.util

import com.daml.ledger.api.v1.LedgerIdentityServiceGrpc
import com.daml.ledger.api.v1.LedgerIdentityServiceOuterClass.GetLedgerIdentityResponse
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import org.grpcmock.GrpcMock
import org.grpcmock.GrpcMock.unaryMethod


@Target(AnnotationTarget.FIELD)
annotation class InjectGrpcMock

class GrpcMockExtension : QuarkusTestResourceLifecycleManager {

    private val grpcMock = GrpcMock.grpcMock().build()

    override fun start(): Map<String, String> {
        grpcMock.start()

        GrpcMock.configureFor(grpcMock)

        GrpcMock.stubFor(
            unaryMethod(LedgerIdentityServiceGrpc.getGetLedgerIdentityMethod())
                .willReturn(GetLedgerIdentityResponse.getDefaultInstance())
        )

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