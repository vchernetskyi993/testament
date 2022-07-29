package com.example.testament.flows

import com.example.testament.BANK_ORG
import com.example.testament.JustSignFlowAcceptor
import com.example.testament.TransactionHelper
import com.example.testament.contracts.AccountContract
import com.example.testament.government
import com.example.testament.latestState
import com.example.testament.schema.AccountSchemaV1
import com.example.testament.states.AccountState
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
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

@InitiatingFlow
@StartableByRPC
class WithdrawGoldFlow @JsonConstructor constructor(
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
        val input = jsonMarshallingService.parseJson<GoldInput>(params.parametersInJson)

        val bank = flowIdentity.ourIdentity
        requireThat {
            "Only $BANK_ORG can withdraw gold" using (bank.name.organisation == BANK_ORG)
        }
        val government = identityService.government()
        val existingAccount = persistenceService.latestState<AccountState>(
            AccountSchemaV1.PersistentAccount.BY_HOLDER,
            mapOf("holderId" to input.holder),
        )
        val existingAmount = existingAccount?.state?.data?.amount ?: 0.toBigInteger()

        val accountState = AccountState(
            input.holder,
            existingAmount - input.amount.toBigInteger(),
            bank,
            government,
        )
        val txCommand = Command(
            AccountContract.Commands.Withdraw(),
            listOf(bank.owningKey, government.owningKey)
        )

        return TransactionHelper(
            notaryLookup,
            transactionBuilderFactory,
            flowMessaging,
            flowEngine,
            jsonMarshallingService,
        ).sign(
            command = txCommand,
            approver = government,
            input = existingAccount,
            output = accountState,
            contract = AccountContract::class,
        )
    }
}

@InitiatedBy(WithdrawGoldFlow::class)
class WithdrawGoldFlowAcceptor(otherPartySession: FlowSession) :
    JustSignFlowAcceptor(otherPartySession)
