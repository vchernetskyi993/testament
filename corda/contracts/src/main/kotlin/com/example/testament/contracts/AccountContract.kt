package com.example.testament.contracts

import com.example.testament.BANK_ORG
import com.example.testament.states.AccountState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.outputsOfType

class AccountContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val output = tx.outputsOfType<AccountState>().single()
        requireThat {
            "Only $BANK_ORG can operate on gold" using (output.bank.name.organisation == BANK_ORG)
            "Not enough gold in possession" using (output.amount >= 0.toBigInteger())
        }
    }

    interface Commands : CommandData {
        class Store : Commands
        class Withdraw : Commands
    }
}
