package com.example.testament.processor

import com.example.testament.states.TestamentState
import com.example.testament.states.TestamentStateDto
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.StateAndRefPostProcessor
import java.util.stream.Stream

class TestamentPostProcessor : StateAndRefPostProcessor<TestamentStateDto> {
    companion object {
        const val NAME = "com.example.testament.states.TestamentPostProcessor"
    }

    override val name = NAME

    override fun postProcess(inputs: Stream<StateAndRef<ContractState>>): Stream<TestamentStateDto> =
        inputs
            .filter { it.state.data is TestamentState }
            .map { (it.state.data as TestamentState).toDto() }

    override val availableForRPC = true
}
