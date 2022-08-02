package com.example.testament.flows

import com.example.testament.JustSignFlowAcceptor
import com.example.testament.TransactionHelper
import com.example.testament.account
import com.example.testament.contracts.TestamentContract
import com.example.testament.government
import com.example.testament.provider
import com.example.testament.states.TestamentState
import com.example.testament.testament
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
import net.corda.v5.application.identity.Party
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

@InitiatingFlow
@StartableByRPC
class ExecuteTestamentFlow @JsonConstructor constructor(
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
        val input = jsonMarshallingService.parseJson<TestamentIssuerInput>(params.parametersInJson)

        val bank = flowIdentity.ourIdentity
        val government = identityService.government()
        val provider = identityService.provider()

        splitPossession(
            persistenceService.testament(input.issuer)?.state?.data,
            input.issuer,
            listOf(government, provider)
        )

        val existing = persistenceService.testament(input.issuer)
        val executed = existing?.state?.data?.copy(
            updater = bank,
            signers = listOf(government, provider),
            executed = true,
        )

        val txCommand = Command(
            TestamentContract.Commands.Execute(),
            listOf(bank.owningKey, government.owningKey, provider.owningKey)
        )

        return TransactionHelper(
            notaryLookup,
            transactionBuilderFactory,
            flowMessaging,
            flowEngine,
            jsonMarshallingService
        ).sign(
            command = txCommand,
            signers = listOf(government, provider),
            inputs = listOfNotNull(existing),
            outputs = listOfNotNull(executed),
            contracts = listOf(TestamentContract::class),
        )
    }

    @Suspendable
    private fun splitPossession(
        testament: TestamentState?,
        holder: String,
        parties: Collection<Party>,
    ) {
        val possession = persistenceService.account(holder)?.state?.data?.amount
        if (possession == null || possession <= 0.toBigInteger()) {
            return
        }
        val partiesStr = parties.map { it.name.toString() }
        testament?.inheritors?.forEach { (id, share) ->
            val transfer = possession * share.toBigInteger() / 10000.toBigInteger()
            flowEngine.subFlow(
                StoreGoldFlow(
                    RpcStartFlowRequestParameters(
                        jsonMarshallingService.formatJson(
                            GoldInput(id, transfer.toString(), partiesStr)
                        )
                    ),
                )
            )
            flowEngine.subFlow(
                WithdrawGoldFlow(
                    RpcStartFlowRequestParameters(
                        jsonMarshallingService.formatJson(
                            GoldInput(holder, transfer.toString(), partiesStr)
                        )
                    ),
                )
            )
        }
    }
}

@InitiatedBy(ExecuteTestamentFlow::class)
class ExecuteTestamentFlowAcceptor(otherPartySession: FlowSession) :
    JustSignFlowAcceptor(otherPartySession)
