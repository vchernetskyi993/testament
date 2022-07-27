package com.example.testament.contracts

import com.example.testament.states.TestamentState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireSingleCommand
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.outputsOfType

class TestamentContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputsOfType<TestamentState>().single()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing testament" using tx.inputStates.isEmpty()
                "Inheritors should not be empty" using output.inheritors.isNotEmpty()
                "Shares should sum up to 10000" using (output.inheritors.values.sum() == 10000)
            }
        }
    }

    interface Commands : CommandData {
        class Issue : Commands
    }
}