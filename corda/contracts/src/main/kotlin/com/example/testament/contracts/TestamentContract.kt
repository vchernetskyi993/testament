package com.example.testament.contracts

import com.example.testament.GOV_ORG
import com.example.testament.PROVIDER_ORG
import com.example.testament.states.TestamentState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.Requirements
import net.corda.v5.ledger.contracts.requireSingleCommand
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.contracts.select
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.inputsOfType
import net.corda.v5.ledger.transactions.outputsOfType

class TestamentContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        if (tx.commands.select<AccountContract.Commands>().isNotEmpty()) {
            // no need to validate testament state for account transaction
            return
        }
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputsOfType<TestamentState>().single()
        requireThat {
            "Inheritors should not be empty" using output.inheritors.isNotEmpty()
            "Shares should sum up to 10000" using (output.inheritors.values.sum() == 10000)
        }
        when (command.value) {
            is Commands.Issue -> requireThat {
                "Testament for the issuer already exists." using (
                        tx.inputStates.isEmpty()
                                || tx.inputsOfType<TestamentState>().single().revoked
                        )
                updaterIsProvider(output)
            }
            is Commands.Update -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                updaterIsProvider(output)
            }
            is Commands.Revoke -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                "Should become revoked" using output.revoked
                updaterIsProvider(output)
            }
            is Commands.Announce -> requireThat {
                txHasSingleInput(tx)
                isNotRevoked(tx)
                isNotAnnounced(tx)
                "Should become announced" using output.announced
                updaterIsGovernment(output)
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

    private fun Requirements.updaterIsProvider(output: TestamentState) {
        "Only $PROVIDER_ORG can change testaments" using
                (output.updater.name.organisation == PROVIDER_ORG)
    }

    private fun Requirements.updaterIsGovernment(output: TestamentState) {
        "Only $GOV_ORG can announce testaments" using
                (output.updater.name.organisation == GOV_ORG)
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Update : Commands
        class Revoke : Commands
        class Announce : Commands
    }
}