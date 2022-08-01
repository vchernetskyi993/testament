package com.example.testament.flows

import com.example.testament.JustSignFlowAcceptor
import com.example.testament.TransactionHelper
import com.example.testament.contracts.TestamentContract
import com.example.testament.government
import com.example.testament.latestState
import com.example.testament.schema.TestamentSchemaV1
import com.example.testament.states.TestamentState
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

data class RevokeTestamentInput(
    val issuer: String,
)

@InitiatingFlow
@StartableByRPC
class RevokeTestamentFlow @JsonConstructor constructor(
    private val params: RpcStartFlowRequestParameters,
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var notaryLookup: NotaryLookupService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val input = jsonMarshallingService.parseJson<RevokeTestamentInput>(params.parametersInJson)

        val provider = flowIdentity.ourIdentity
        val government = identityService.government()

        val existing = persistenceService.latestState<TestamentState>(
            TestamentSchemaV1.PersistentTestament.BY_ISSUER,
            mapOf("issuerId" to input.issuer),
        )
        val revoked = existing?.state?.data?.copy(
            provider = flowIdentity.ourIdentity,
            revoked = true
        )

        val txCommand = Command(
            TestamentContract.Commands.Revoke(),
            listOf(provider.owningKey, government.owningKey)
        )

        return TransactionHelper(
            notaryLookup,
            transactionBuilderFactory,
            flowMessaging,
            flowEngine,
            jsonMarshallingService
        ).sign(
            command = txCommand,
            approver = government,
            input = existing,
            output = revoked,
            contract = TestamentContract::class,
        )
    }
}

@InitiatedBy(RevokeTestamentFlow::class)
class RevokeTestamentFlowAcceptor(otherPartySession: FlowSession) :
    JustSignFlowAcceptor(otherPartySession)
