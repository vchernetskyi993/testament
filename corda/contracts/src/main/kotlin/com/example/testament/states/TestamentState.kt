package com.example.testament.states

import com.example.testament.contracts.TestamentContract
import com.example.testament.schema.TestamentSchemaV1
import com.google.gson.Gson
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema
import java.util.UUID

@BelongsToContract(TestamentContract::class)
data class TestamentState(
    val issuer: Long,
    val inheritors: Map<Long, Int>,
    val provider: Party,
    val approver: Party,
    val executed: Boolean = false,
) : LinearState, JsonRepresentable, QueryableState {
    override val participants = listOf(provider, approver)

    override fun generateMappedObject(schema: MappedSchema) = when (schema) {
        is TestamentSchemaV1 -> TestamentSchemaV1.PersistentTestament(
            issuer,
        )
        else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas() = listOf(TestamentSchemaV1)

    override val linearId = UniqueIdentifier(externalId = issuer.toString())

    override fun toJsonString(): String = Gson()
        .toJson(
            TestamentStateDto(
                linearId.id,
                linearId.externalId!!,
                inheritors.mapKeys { it.key.toString() },
                provider.name.toString(),
                approver.name.toString(),
                executed
            )
        )
}

data class TestamentStateDto(
    val id: UUID,
    val issuer: String,
    val inheritors: Map<String, Int>,
    val provider: String,
    val approver: String,
    val executed: Boolean,
)
