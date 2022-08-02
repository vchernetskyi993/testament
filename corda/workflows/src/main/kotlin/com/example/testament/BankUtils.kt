package com.example.testament

import com.example.testament.contracts.AccountContract
import com.example.testament.contracts.TestamentContract
import com.example.testament.flows.GoldInput
import com.example.testament.states.AccountState
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.math.BigInteger

class GoldFlowHelper(
    private val flowEngine: FlowEngine,
    private val flowIdentity: FlowIdentity,
    private val flowMessaging: FlowMessaging,
    private val transactionBuilderFactory: TransactionBuilderFactory,
    private val identityService: IdentityService,
    private val notaryLookup: NotaryLookupService,
    private val jsonMarshallingService: JsonMarshallingService,
    private val persistenceService: PersistenceService,
) {
    @Suspendable
    fun process(
        params: RpcStartFlowRequestParameters,
        command: AccountContract.Commands,
        operation: (BigInteger, BigInteger) -> BigInteger,
    ): SignedTransactionDigest {
        val input = jsonMarshallingService.parseJson<GoldInput>(params.parametersInJson)

        val bank = flowIdentity.ourIdentity
        val government = identityService.government()
        val testament = persistenceService.testament(input.holder)
        val existingAccount = persistenceService.account(input.holder)
        val existingAmount = existingAccount?.state?.data?.amount ?: 0.toBigInteger()

        val accountState = AccountState(
            input.holder,
            operation(existingAmount, input.amount.toBigInteger()),
            bank,
            government,
        )
        val signers = setOf(government) + input.signers.mapNotNull {
            identityService.partyFromName(
                CordaX500Name.parse(it)
            )
        }
        val txCommand = Command(command, signers.map { it.owningKey }.toList())

        return TransactionHelper(
            notaryLookup,
            transactionBuilderFactory,
            flowMessaging,
            flowEngine,
            jsonMarshallingService,
        ).sign(
            command = txCommand,
            signers = signers,
            inputs = listOfNotNull(existingAccount, testament),
            outputs = listOfNotNull(accountState, testament?.state?.data),
            contracts = listOf(AccountContract::class, TestamentContract::class),
        )
    }
}
