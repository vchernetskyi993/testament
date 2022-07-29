package com.example.testament.contracts

import com.example.testament.PROVIDER_ORG
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
        requireThat {
            "Only $PROVIDER_ORG can issue testaments" using
                    (output.provider.name.organisation == PROVIDER_ORG)
            "Inheritors should not be empty" using output.inheritors.isNotEmpty()
            "Shares should sum up to 10000" using (output.inheritors.values.sum() == 10000)
        }
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing testament" using tx.inputStates.isEmpty()
            }
            is Commands.Update -> requireThat {
                "Input should be single TestamentState" using (tx.inputStates.single() is TestamentState)
            }
        }
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Update : Commands
        class Revoke : Commands
    }
}