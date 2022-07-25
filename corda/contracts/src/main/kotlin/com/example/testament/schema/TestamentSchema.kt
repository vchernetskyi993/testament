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
        name = "TestamentSchemaV1.PersistentTestament.findByIssuerId",
        query = "from com.example.testament.schema.TestamentSchemaV1\$PersistentTestament where issuerId = :issuerId"
    )
    @Table(name = "testament_states")
    class PersistentTestament(
        @Column(name = "issuer_id")
        var issuerId: Long?,

//        @Column(name="planetary_only")
//        var planetaryOnly: Boolean,
//
//        @Column(name = "launcher")
//        var launcherName: String,
//
//        @Column(name = "target")
//        var targetName: String,
//
//        @Column(name = "linear_id")
//        @Convert(converter = UUIDConverter::class)
//        var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(null)
    }
}
