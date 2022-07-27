package com.example.testament.contracts

import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireSingleCommand
import net.corda.v5.ledger.transactions.LedgerTransaction

class AccountContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Store -> {}
        }
    }

    interface Commands : CommandData {
        class Store : Commands
    }
}
