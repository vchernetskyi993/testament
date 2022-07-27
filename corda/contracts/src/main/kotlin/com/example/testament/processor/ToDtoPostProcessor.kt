package com.example.testament.processor

import com.example.testament.states.ToDto
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.StateAndRefPostProcessor
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface ToDtoPostProcessor<T : ToDto<R>, R> : StateAndRefPostProcessor<R> {
    val stateType: KClass<T>

    override val name: String get() = javaClass.name

    override val availableForRPC get() = true

    override fun postProcess(inputs: Stream<StateAndRef<ContractState>>): Stream<R> {
        return inputs
            .map { it.state.data }
            .filter(stateType::isInstance)
            .map(stateType::cast)
            .map { it.toDto() }
    }
}
