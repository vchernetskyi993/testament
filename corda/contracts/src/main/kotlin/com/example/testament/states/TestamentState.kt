package com.example.testament.states

import com.example.testament.contracts.TestamentContract
import com.example.testament.schema.TestamentSchemaV1
import com.google.gson.Gson
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema
import java.util.UUID

@BelongsToContract(TestamentContract::class)
data class TestamentState(
    val issuer: String,
    val inheritors: Map<String, Int>,
    val updater: Party,
    val signers: Collection<Party>,
    val revoked: Boolean = false,
    val announced: Boolean = false,
    val executed: Boolean = false,
) : LinearState, JsonRepresentable, QueryableState, ToDto<TestamentStateDto> {
    override val participants = listOf(listOf(updater), signers).flatten()

    override fun generateMappedObject(schema: MappedSchema) = when (schema) {
        is TestamentSchemaV1 -> TestamentSchemaV1.PersistentTestament(
            issuer,
        )
        else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas() = listOf(TestamentSchemaV1)

    override val linearId = UniqueIdentifier()

    override fun toJsonString(): String = Gson().toJson(toDto())

    override fun toDto() = TestamentStateDto(
        linearId.id,
        issuer,
        inheritors,
        updater.name.toString(),
        signers.map { it.name.toString() },
        revoked,
        announced,
        executed,
    )
}

@CordaSerializable
data class TestamentStateDto(
    val id: UUID,
    val issuer: String,
    val inheritors: Map<String, Int>,
    val updater: String,
    val signers: Collection<String>,
    val revoked: Boolean,
    val announced: Boolean,
    val executed: Boolean,
)
