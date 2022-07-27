package com.example.testament.schema

import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.NamedQuery
import javax.persistence.Table

object TestamentSchema

object TestamentSchemaV1 : MappedSchema(
    schemaFamily = TestamentSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentTestament::class.java)
) {

    override val migrationResource: String
        get() = "testament.changelog-master"

    @Entity
    @NamedQuery(
        name = PersistentTestament.BY_ISSUER,
        query = "from com.example.testament.schema.TestamentSchemaV1\$PersistentTestament where issuerId = :issuerId"
    )
    @Table(name = "testament_states")
    class PersistentTestament(
        @Column(name = "issuer_id")
        var issuerId: String,
    ) : PersistentState() {
        companion object {
            const val BY_ISSUER = "TestamentSchemaV1.PersistentTestament.findByIssuerId"
        }
        // Default constructor required by hibernate.
        constructor() : this("")
    }
}
