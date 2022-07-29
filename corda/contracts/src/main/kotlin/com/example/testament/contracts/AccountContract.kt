package com.example.testament.contracts

import com.example.testament.states.AccountState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.outputsOfType

class AccountContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<AccountState>().single()
        requireThat {
            "Not enough gold in possession" using (state.amount >= 0.toBigInteger())
        }
    }

    interface Commands : CommandData {
        class Store : Commands
        class Withdraw : Commands
    }
}
