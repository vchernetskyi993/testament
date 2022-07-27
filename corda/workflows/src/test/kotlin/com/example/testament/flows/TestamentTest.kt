package com.example.testament.flows

import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kong.unirest.HttpResponse
import kong.unirest.HttpStatus
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONObject
import net.corda.test.dev.network.Credentials
import net.corda.test.dev.network.TestNetwork
import net.corda.test.dev.network.withFlow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

private const val NETWORK = "testament-network"
private const val PROVIDER = "TestamentProvider"

private val credentials = mapOf(
    PROVIDER to Credentials("testamentadmin", "Password1!"),
)

typealias FlowId = String

class TestamentTest {

    @BeforeAll
    fun beforeAll() {
        TestNetwork.forNetwork(NETWORK).verify {
            hasNode(PROVIDER).withFlow<IssueTestamentFlow>()
            hasNode("Government").withFlow<IssueTestamentFlow>()
        }
    }

    @Nested
    inner class IssueTestament {
        @Test
        fun `Should issue testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)

            // when
            startFlow(
                flowName = IssueTestamentFlow::class.java.name,
                parametersInJson = mapOf(
                    "issuer" to issuerId,
                    "inheritors" to inheritors,
                ).toJson(),
            )

            // then
            val stored = retrieveTestament(issuerId)
            stored["issuer"] shouldBe issuerId
            stored.getJSONObject("inheritors").toMap() shouldBe inheritors
        }

        @Test
        fun `Should check inheritors non empty`() = withNode(PROVIDER) {
            // given+when
            startFlow(
                flowName = IssueTestamentFlow::class.java.name,
                parametersInJson = mapOf(
                    "issuer" to UUID.randomUUID().toString(),
                    "inheritors" to mapOf<String, Int>(),
                ).toJson()
            ) {
                // then
                failure(it, "empty")
            }
        }

        @Test
        fun `Should check shares total is 100 percent`() = withNode(PROVIDER) {
            // given+when
            startFlow(
                flowName = IssueTestamentFlow::class.java.name,
                parametersInJson = mapOf(
                    "issuer" to UUID.randomUUID().toString(),
                    "inheritors" to mapOf("1" to 6000, "2" to 4500),
                ).toJson(),
            ) {
                // then
                failure(it, "10000")
            }
        }

        @Test
        fun `Should check testament isn't already present`() = withNode(PROVIDER) {
            // given
            val input = mapOf(
                "issuer" to UUID.randomUUID().toString(),
                "inheritors" to mapOf("1" to 6000, "2" to 4000),
            ).toJson()
            startFlow(
                flowName = IssueTestamentFlow::class.java.name,
                parametersInJson = input,
            )

            // when
            startFlow(
                flowName = IssueTestamentFlow::class.java.name,
                parametersInJson = input,
            ) {
                // then
                failure(it, "already exists")
            }
        }
    }

    // TODO: execute testament

    // TODO: update testament

    // TODO: revoke testament

    private fun withNode(name: String, action: UnirestInstance.() -> Unit) {
        TestNetwork.forNetwork(NETWORK).use {
            getNode(name).httpRpc(
                credentials[name]
                    ?: throw IllegalStateException("No credentials for node $name")
            ) {
                action()
            }
        }
    }

    private fun Any.toJson(): String = Gson().toJson(this)

    private fun startFlow(
        flowName: String,
        parametersInJson: String,
        clientId: String = UUID.randomUUID().toString(),
        outcome: (FlowId) -> Unit = ::success,
    ) {
        val body = mapOf(
            "rpcStartFlowRequest" to mapOf(
                "flowName" to flowName,
                "clientId" to clientId,
                "parameters" to mapOf("parametersInJson" to parametersInJson)
            )
        )
        val response = Unirest.post("flowstarter/startflow")
            .header("Content-Type", "application/json")
            .body(body)
            .asJson()

        response.status shouldBe HttpStatus.OK
        val flowId = response.body.`object`.getJSONObject("flowId")
        flowId shouldNotBe null
        outcome(flowId.getString("uuid"))
    }

    private fun success(flowId: FlowId) {
        eventually {
            with(retrieveOutcome(flowId)) {
                status shouldBe HttpStatus.OK
                body.`object`["status"] shouldBe "COMPLETED"
            }
        }
    }

    private fun failure(flowId: FlowId, message: String) {
        eventually {
            with(retrieveOutcome(flowId)) {
                status shouldBe HttpStatus.OK
                body.`object`["status"] shouldBe "FAILED"
                body.`object`.getJSONObject("exceptionDigest")
                    .getString("message") shouldContain message
            }
        }
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