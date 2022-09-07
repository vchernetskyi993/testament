package com.example

import com.daml.ledger.rxjava.DamlLedgerClient
import com.daml.ledger.rxjava.LedgerClient
import io.quarkus.runtime.Startup
import io.smallrye.config.ConfigMapping
import kotlinx.coroutines.runBlocking
import javax.enterprise.inject.Produces
import javax.inject.Singleton


@ConfigMapping(prefix = "daml")
interface DamlProperties {
    fun appId(): String
    fun party(): String
    fun factoryId(): String
    fun government(): String
}

@ConfigMapping(prefix = "daml.ledger")
interface LedgerProperties {
    fun host(): String
    fun port(): Int
}


class DamlConfig {

    @Startup
    @Singleton
    @Produces
    fun ledgerClient(props: LedgerProperties, auth: AuthService): LedgerClient {
        val client = DamlLedgerClient.newBuilder(props.host(), props.port())
            .withAccessToken(runBlocking { auth.jwt() })
            .build()
        client.connect()
        return client
    }

}