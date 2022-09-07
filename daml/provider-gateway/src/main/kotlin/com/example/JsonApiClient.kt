package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path

data class FetchTestamentRequest(
    val templateId: String,
    val key: TestamentKey,
)

data class TestamentKey(
    @JsonProperty("_1")
    val government: String,
    @JsonProperty("_2")
    val issuer: String,
)

data class FetchTestamentResponse(
    val result: TestamentResult?,
)

data class TestamentResult(
    val payload: TestamentPayload,
)

data class TestamentPayload(
    val issuer: String,
    @JsonDeserialize(using = InheritorsDeserializer::class)
    val inheritors: List<Pair<String, Int>>,
    val announced: Boolean = false,
    val executed: Boolean = false,
)

object InheritorsDeserializer : JsonDeserializer<List<Pair<String, Int>>>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): List<Pair<String, Int>> =
        p.readValueAsTree<ArrayNode>()
            .map { Pair(it[0].asText(), it[1].asInt()) }
}

@RegisterRestClient(configKey = "json")
interface JsonApiClient {
    @POST
    @Path("/fetch")
    suspend fun fetchTestament(
        input: FetchTestamentRequest,
        @HeaderParam("Authorization") jwt: String,
    ): FetchTestamentResponse
}
