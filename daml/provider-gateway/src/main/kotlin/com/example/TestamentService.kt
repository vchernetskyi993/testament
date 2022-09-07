package com.example

import com.daml.ledger.rxjava.LedgerClient
import com.example.main.factory.TestamentFactory
import com.example.main.testament.Testament
import kotlinx.coroutines.rx2.await
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.UUID
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.NotFoundException

@ApplicationScoped
class TestamentService(
    private val props: DamlProperties,
    private val ledgerClient: LedgerClient,
    private val auth: AuthService,
    @RestClient private val jsonClient: JsonApiClient,
) {
    private val factory = TestamentFactory.ContractId(props.factoryId())

    suspend fun issueTestament(testament: TestamentDto) {
        ledgerClient.commandClient.submitAndWait(
            UUID.randomUUID().toString(),
            props.appId(),
            UUID.randomUUID().toString(),
            props.party(),
            listOf(
                factory.exerciseIssueTestament(
                    testament.issuer,
                    testament.inheritors.mapValues { (_, s) -> s.toLong() }
                )
            ),
            auth.jwt(),
        ).await()
    }

    suspend fun fetchTestament(issuer: String): TestamentDto {
        val response = jsonClient.fetchTestament(
            FetchTestamentRequest(
                testamentTemplateId(),
                TestamentKey(props.government(), issuer)
            ),
            "Bearer ${auth.jwt()}",
        )
        return response.result?.payload?.let {
            TestamentDto(it.issuer, it.inheritors.toMap(), it.announced, it.executed)
        } ?: throw NotFoundException()
    }

    suspend fun updateTestament(issuer: String, inheritors: Map<String, Int>): Unit =
        TODO()

    suspend fun revokeTestament(issuer: String): Unit = TODO()

    private fun testamentTemplateId(): String =
        "${Testament.TEMPLATE_ID.moduleName}:${Testament.TEMPLATE_ID.entityName}"
}