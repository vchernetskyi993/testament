package com.example.testament

import com.example.testament.schema.AccountSchemaV1
import com.example.testament.schema.TestamentSchemaV1
import com.example.testament.states.AccountState
import com.example.testament.states.TestamentState
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor

@Suspendable
fun PersistenceService.account(holder: String) = latestState<AccountState>(
    AccountSchemaV1.PersistentAccount.BY_HOLDER,
    mapOf("holderId" to holder),
)

@Suspendable
fun PersistenceService.testament(issuer: String) = latestState<TestamentState>(
    TestamentSchemaV1.PersistentTestament.BY_ISSUER,
    mapOf("issuerId" to issuer),
)

private inline fun <reified T : ContractState> PersistenceService.latestState(
    query: String,
    params: Map<String, Any>,
): StateAndRef<T>? {
    var pollResult: Cursor.PollResult<StateAndRef<T>>? = null
    while (pollResult?.isLastResult != true) {
        pollResult = query<StateAndRef<T>>(
            query,
            params,
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
        ).poll(100, 20.seconds)
    }
    return pollResult.values.lastOrNull()
}
