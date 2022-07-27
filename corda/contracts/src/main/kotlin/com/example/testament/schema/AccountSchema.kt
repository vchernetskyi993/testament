package com.example.testament.schema

import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.NamedQuery
import javax.persistence.Table

object AccountSchema

object AccountSchemaV1 : MappedSchema(
    schemaFamily = AccountSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentAccount::class.java)
) {

    override val migrationResource: String
        get() = "account.changelog-master"

    @Entity
    @NamedQuery(
        name = PersistentAccount.BY_HOLDER,
        query = "from com.example.testament.schema.AccountSchemaV1\$PersistentAccount where holderId = :holderId"
    )
    @Table(name = "account_states")
    class PersistentAccount(
        @Column(name = "holder_id")
        var holderId: String,
    ) : PersistentState() {
        companion object {
            const val BY_HOLDER = "AccountSchemaV1.PersistentAccount.findByHolderId"
        }
        // Default constructor required by hibernate.
        constructor() : this("")
    }
}
