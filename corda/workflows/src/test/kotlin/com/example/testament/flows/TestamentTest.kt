package com.example.testament.flows

import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kong.unirest.HttpResponse
import kong.unirest.HttpStatus
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.json.JSONObject
import net.corda.test.dev.network.Credentials
import net.corda.test.dev.network.TestNetwork
import net.corda.test.dev.network.withFlow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

private const val NETWORK = "testament-network"
private const val PROVIDER_NODE = "TestamentProvider"
private const val PROVIDER_USER = "testamentadmin"
private const val PROVIDER_PASSWORD = "Password1!"

class TestamentTest {

    @BeforeAll
    fun beforeAll() {
        TestNetwork.forNetwork(NETWORK).verify {
            hasNode(PROVIDER_NODE).withFlow<IssueTestamentFlow>()
            hasNode("Government").withFlow<IssueTestamentFlow>()
        }
    }

    @Test
    fun `Should issue testament`() {
        TestNetwork.forNetwork(NETWORK).use {
            getNode(PROVIDER_NODE).httpRpc(Credentials(PROVIDER_USER, PROVIDER_PASSWORD)) {
                val issuerId = UUID.randomUUID().toString()
                val inheritors = mapOf("1" to 6000, "2" to 4000)
                val flowId = with(
                    startFlow(
                        flowName = IssueTestamentFlow::class.java.name,
                        parametersInJson = mapOf(
                            "issuer" to issuerId,
                            "inheritors" to inheritors,
                        ).toJson()
                    )
                ) {
                    status shouldBe HttpStatus.OK
                    val flowId = body.`object`["flowId"] as JSONObject
                    flowId shouldNotBe null
                    flowId["uuid"] as String
                }
                eventually {
                    with(retrieveOutcome(flowId)) {
                        status shouldBe HttpStatus.OK
                        body.`object`.get("status") shouldBe "COMPLETED"
                    }
                }
                with(retrieveOutcome(flowId)) {
                    status shouldBe HttpStatus.OK
                    body.`object`.get("status") shouldBe "COMPLETED"
                }
                val stored = retrieveTestament(issuerId)
                stored["issuer"] shouldBe issuerId
                stored.getJSONObject("inheritors").toMap() shouldBe inheritors
            }
        }
    }

    private fun Any.toJson(): String = Gson().toJson(this)

    private fun startFlow(
        flowName: String,
        parametersInJson: String,
        clientId: String = UUID.randomUUID().toString(),
    ): HttpResponse<JsonNode> {
        val body = mapOf(
            "rpcStartFlowRequest" to mapOf(
                "flowName" to flowName,
                "clientId" to clientId,
                "parameters" to mapOf("parametersInJson" to parametersInJson)
            )
        )
        val request = Unirest.post("flowstarter/startflow")
            .header("Content-Type", "application/json")
            .body(body)

        return request.asJson()
    }

    private fun retrieveOutcome(flowId: String): HttpResponse<JsonNode> {
        val request = Unirest.get("flowstarter/flowoutcome/$flowId")
            .header("Content-Type", "application/json")
        return request.asJson()
    }

    private fun retrieveTestament(issuerId: String): JSONObject {
        val body = mapOf(
            "request" to mapOf(
                "namedParameters" to mapOf(
                    "issuerId" to mapOf("parametersInJson" to issuerId.toJson())
                ),
                "queryName" to "TestamentSchemaV1.PersistentTestament.findByIssuerId",
                "postProcessorName" to "com.example.testament.states.TestamentPostProcessor",
            ),
            "context" to mapOf(
                "awaitForResultTimeout" to "PT15M",
                "currentPosition" to -1,
                "maxCount" to 10
            ),
        )
        val request = Unirest.post("persistence/query")
            .header("Content-Type", "application/json")
            .body(body)

        return JSONObject(
            request.asJson().body.`object`
                .getJSONArray("positionedValues")
                .getJSONObject(0)
                .getJSONObject("value")
                .getString("json")
        )
    }

    private inline fun <R> eventually(
        duration: Duration = Duration.ofSeconds(5),
        waitBetween: Duration = Duration.ofMillis(100),
        waitBefore: Duration = waitBetween,
        test: () -> R,
    ): R {
        val end = System.nanoTime() + duration.toNanos()
        var times = 0
        var lastFailure: AssertionError? = null

        if (!waitBefore.isZero) Thread.sleep(waitBefore.toMillis())

        while (System.nanoTime() < end) {
            try {
                return test()
            } catch (e: AssertionError) {
                if (!waitBetween.isZero) Thread.sleep(waitBetween.toMillis())
                lastFailure = e
            }
            times++
        }

        throw AssertionError("Test failed with \"${lastFailure?.message}\" after $duration; attempted $times times")
    }

}