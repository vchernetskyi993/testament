package com.example.testament.processor

import com.example.testament.states.TestamentState
import com.example.testament.states.TestamentStateDto

object TestamentPostProcessor : ToDtoPostProcessor<TestamentState, TestamentStateDto> {
    override val stateType = TestamentState::class
}
