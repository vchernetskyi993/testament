package com.example.testament.states

import com.example.testament.contracts.AccountContract
import com.example.testament.schema.AccountSchemaV1
import com.google.gson.Gson
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema
import java.math.BigInteger
import java.util.UUID

@BelongsToContract(AccountContract::class)
data class AccountState(
    val holder: String,
    val amount: BigInteger,
    val bank: Party,
    val signer: Party,
) : LinearState, JsonRepresentable, QueryableState, ToDto<AccountStateDto> {
    override val participants = listOf(bank, signer)

    override fun generateMappedObject(schema: MappedSchema) = when (schema) {
        is AccountSchemaV1 -> AccountSchemaV1.PersistentAccount(holder)
        else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas() = listOf(AccountSchemaV1)

    override val linearId = UniqueIdentifier()

    override fun toJsonString(): String = Gson().toJson(toDto())

    override fun toDto() = AccountStateDto(
        linearId.id,
        holder,
        amount.toString(),
        bank.name.toString(),
        signer.name.toString(),
    )
}

@CordaSerializable
data class AccountStateDto(
    val id: UUID,
    val holder: String,
    val amount: String,
    val provider: String,
    val signer: String,
)
