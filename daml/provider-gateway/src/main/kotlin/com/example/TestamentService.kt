package com.example

import com.daml.ledger.rxjava.LedgerClient
import com.example.main.factory.TestamentFactory
import kotlinx.coroutines.rx2.await
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class TestamentService(
    val client: LedgerClient,
) {
    suspend fun issueTestament(testament: Testament) {
        client.commandClient.submitAndWait(
            UUID.randomUUID().toString(),
            TODO("app id"),
            UUID.randomUUID().toString(),
            TODO("party id"),
            listOf(
                TestamentFactory.ContractId(TODO("contract id")).exerciseIssueTestament(
                    testament.issuer,
                    testament.inheritors.mapValues { (i, s) -> s.toLong() })
            ),
        ).await()
    }

    suspend fun fetchTestament(issuer: String): Testament = Testament("", mapOf())

    suspend fun updateTestament(issuer: String, inheritors: Map<String, Int>): Unit =
        TODO()

    suspend fun revokeTestament(issuer: String): Unit = TODO()
}