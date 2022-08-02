package com.example.testament.contracts

import com.example.testament.GOV_ORG
import com.example.testament.PROVIDER_ORG
import com.example.testament.states.TestamentState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.Requirements
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.inputsOfType
import net.corda.v5.ledger.transactions.outputsOfType

class TestamentContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val output = tx.outputsOfType<TestamentState>().single()
        requireThat {
            "Inheritors should not be empty" using output.inheritors.isNotEmpty()
            "Shares should sum up to 10000" using (output.inheritors.values.sum() == 10000)
        }
        when (tx.commands.single().value) {
            is Commands.Issue -> requireThat {
                "Testament for the issuer already exists." using (
                        tx.inputStates.isEmpty()
                                || tx.inputsOfType<TestamentState>().single().revoked
                        )
                updaterIs(output, PROVIDER_ORG)
            }
            is Commands.Update -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                updaterIs(output, PROVIDER_ORG)
            }
            is Commands.Revoke -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                "Should become revoked" using output.revoked
                updaterIs(output, PROVIDER_ORG)
            }
            is Commands.Announce -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                "Should become announced" using output.announced
                updaterIs(output, GOV_ORG)
            }
            is Commands.Execute -> requireThat {
                txHasSingleInput(tx)
                val input = tx.inputsOfType<TestamentState>().single()
                "Should be announced" using input.announced
                "Should not be executed" using !input.executed
                "Should become executed" using output.executed
            }
            is AccountContract.Commands.Store -> requireThat {
                txHasSingleInput(tx)
                val input = tx.inputsOfType<TestamentState>().single()
                "Testament should not be announced" using !input.announced
                "Testament should not change" using (input == output)
            }
        }
    }

    private fun Requirements.txHasSingleInput(tx: LedgerTransaction) {
        "Input should be single TestamentState" using (tx.inputStates.single() is TestamentState)
    }

    private fun Requirements.isNotRevoked(tx: LedgerTransaction) {
        "Input should not be revoked" using !tx.inputsOfType<TestamentState>().single().revoked
    }

    private fun Requirements.isNotAnnounced(tx: LedgerTransaction) {
        "Input should not be announced" using !tx.inputsOfType<TestamentState>().single().announced
    }

    private fun Requirements.updaterIs(output: TestamentState, updater: String) {
        "Should be updated by $updater" using
                (output.updater.name.organisation == updater)
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Update : Commands
        class Revoke : Commands
        class Announce : Commands
        class Execute : Commands
    }
}