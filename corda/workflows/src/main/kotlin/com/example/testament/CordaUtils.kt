package com.example.testament

import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowException
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import kotlin.reflect.KClass

open class JustSignFlowAcceptor(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {}
        }
        val txId = flowEngine.subFlow(signTransactionFlow).id
        return flowEngine.subFlow(ReceiveFinalityFlow(otherPartySession, txId))
    }
}

class TransactionHelper(
    private val notaryLookup: NotaryLookupService,
    private val transactionBuilderFactory: TransactionBuilderFactory,
    private val flowMessaging: FlowMessaging,
    private val flowEngine: FlowEngine,
    private val jsonMarshallingService: JsonMarshallingService,
) {
    @Suspendable
    fun sign(
        command: Command<*>,
        approver: Party,
        input: StateAndRef<*>? = null,
        output: ContractState? = null,
        contract: KClass<out Contract>? = null,
    ): SignedTransactionDigest {
        // Stage 1.
        // Generate an unsigned transaction.
        val notary = notaryLookup.getNotary(CordaX500Name.parse("C=US, L=New York, O=Notary"))!!
        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addCommand(command)

        if (input != null) {
            txBuilder.addInputState(input)
        }

        if (output != null) {
            txBuilder.addOutputState(
                output,
                contract?.qualifiedName
                    ?: throw FlowException("Contract class argument is required for output state")
            )
        }

        // Stage 2.
        // Verify that the transaction is valid.
        txBuilder.verify()

        // Stage 3.
        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Stage 4.
        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = flowMessaging.initiateFlow(approver)
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
            notarisedTx.tx.outputStates.map(jsonMarshallingService::formatJson),
            notarisedTx.sigs
        )
    }
}
