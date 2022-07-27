package com.example.testament.processor

import com.example.testament.states.AccountState
import com.example.testament.states.AccountStateDto

object AccountPostProcessor : ToDtoPostProcessor<AccountState, AccountStateDto> {
    override val stateType = AccountState::class
}
