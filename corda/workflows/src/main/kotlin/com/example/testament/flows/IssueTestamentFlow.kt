package com.example.testament.flows

import com.example.testament.contracts.TestamentContract
import com.example.testament.states.TestamentState
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

data class TestamentInput(
    val issuer: String,
    val inheritors: Map<String, Int>,
)

@InitiatingFlow
@StartableByRPC
class IssueTestamentFlow @JsonConstructor constructor(
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
        val input = jsonMarshallingService.parseJson<TestamentInput>(params.parametersInJson)

        val provider = flowIdentity.ourIdentity
        // TODO: try requireThat here
        if (provider.name.organisation != "TestamentProvider") {
            throw FlowException("Only TestamentProvider can issue testaments")
        }
        // TODO: check that testament owner is unique
        //  can we use externalId?
        val notary = notaryLookup.getNotary(CordaX500Name.parse("C=US, L=New York, O=Notary"))!!
        val government = identityService.partyFromName(CordaX500Name.parse("C=US, L=New York, O=Government"))!!

        // Stage 1.
        // Generate an unsigned transaction.
        val testamentState = TestamentState(
            input.issuer.toLong(),
            input.inheritors.mapKeys { it.key.toLong() },
            provider,
            government,
        )
        val txCommand = Command(
            TestamentContract.Commands.Issue(),
            listOf(provider.owningKey, government.owningKey)
        )
        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addOutputState(testamentState, TestamentContract.ID)
            .addCommand(txCommand)


        // Stage 2.
        // Verify that the transaction is valid.
        txBuilder.verify()

        // Stage 3.
        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Stage 4.
        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = flowMessaging.initiateFlow(government)
        val fullySignedTx = flowEngine.subFlow(
            CollectSignaturesFlow(
                partSignedTx, setOf(otherPartySession),
            )
        )

        // Stage 5.
        // Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(
            FinalityFlow(
                fullySignedTx, setOf(otherPartySession),
            )
        )

        return SignedTransactionDigest(
            notarisedTx.id,
            notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
            notarisedTx.sigs
        )
    }

}

@InitiatedBy(IssueTestamentFlow::class)
class IssueTestamentFlowAcceptor(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    fun isValid(stx: SignedTransaction) {
        requireThat {
            val output = stx.tx.outputs.single().data
            "This must be TestamentState transaction." using (output is TestamentState)
        }
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = isValid(stx)
        }
        val txId = flowEngine.subFlow(signTransactionFlow).id
        return flowEngine.subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
    }
}