package com.example.testament.flows

import com.example.testament.processor.AccountPostProcessor
import com.example.testament.processor.TestamentPostProcessor
import com.example.testament.processor.ToDtoPostProcessor
import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase
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
import kotlin.reflect.KClass

private const val NETWORK = "testament-network"
private const val PROVIDER = "TestamentProvider"
private const val BANK = "Bank"
private const val GOVERNMENT = "Government"

private val credentials = mapOf(
    PROVIDER to Credentials("testamentadmin", "Password1!"),
    BANK to Credentials("bankadmin", "Password1!"),
    GOVERNMENT to Credentials("govadmin", "Password1!"),
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
            issueTestament(issuerId, inheritors)

            // then
            val stored = retrieveTestament(issuerId)
            stored["issuer"] shouldBe issuerId
            stored.getJSONObject("inheritors").toMap() shouldBe inheritors
        }

        @Test
        fun `Should check inheritors non empty`() = withNode(PROVIDER) {
            // given+when
            issueTestament(UUID.randomUUID().toString(), mapOf()) {
                // then
                failure("empty")
            }
        }

        @Test
        fun `Should check shares total is 100 percent`() = withNode(PROVIDER) {
            // given+when
            issueTestament(
                UUID.randomUUID().toString(),
                mapOf("1" to 6000, "2" to 4500),
            ) {
                // then
                failure("10000")
            }
        }

        @Test
        fun `Should check testament isn't already present`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)

            // when
            issueTestament(issuerId, inheritors) {
                // then
                failure("already exists")
            }
        }

        @Test
        fun `Only provider should issue testaments`() = withNode(BANK) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)

            // when
            issueTestament(issuerId, inheritors) {
                // then
                failure(PROVIDER)
            }
        }

        @Test
        fun `Should be able to issue again after revocation`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)
            revokeTestament(issuerId)

            // when
            issueTestament(issuerId, inheritors)

            // then
            val stored = retrieveTestament(issuerId)
            stored["issuer"] shouldBe issuerId
            stored.getJSONObject("inheritors").toMap() shouldBe inheritors
            stored["revoked"] shouldBe false
        }
    }

    @Nested
    inner class UpdateTestament {
        @Test
        fun `Should update testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)
            val updatedInheritors = mapOf("1" to 5000, "2" to 2000, "3" to 3000)

            // when
            updateTestament(issuerId, updatedInheritors)

            // then
            val stored = retrieveTestament(issuerId, 0)
            stored["issuer"] shouldBe issuerId
            stored.getJSONObject("inheritors").toMap() shouldBe updatedInheritors
        }

        @Test
        fun `Should assert inheritors not empty`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)

            // when
            updateTestament(issuerId, mapOf()) {
                // then
                failure("empty")
            }
        }

        @Test
        fun `Should assert shares total is 100 percent`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)
            val updatedInheritors = mapOf("1" to 5000, "2" to 2000, "3" to 5000)

            // when
            updateTestament(issuerId, updatedInheritors) {
                // then
                failure("10000")
            }
        }

        @Test
        fun `Only provider should update testaments`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }
            val updatedInheritors = mapOf("1" to 5000, "2" to 2000, "3" to 5000)

            withNode(BANK) {
                updateTestament(issuerId, updatedInheritors) {
                    // then
                    failure()
                }
            }
        }

        @Test
        fun `Should not update revoked testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)
            val updatedInheritors = mapOf("1" to 5000, "2" to 2000, "3" to 3000)
            revokeTestament(issuerId)

            // when
            updateTestament(issuerId, updatedInheritors) {
                // then
                failure("revoked")
            }
        }

        @Test
        fun `Should not update announced testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            val updatedInheritors = mapOf("1" to 5000, "2" to 2000, "3" to 3000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            withNode(PROVIDER) {
                // when
                updateTestament(issuerId, updatedInheritors) {
                    // then
                    failure("announced")
                }
            }
        }

        private fun updateTestament(
            issuerId: String,
            inheritors: Map<String, Int>,
            outcome: FlowId.() -> Unit = { success() },
        ) {
            startFlow(
                flowName = UpdateTestamentFlow::class.java.name,
                parametersInJson = mapOf(
                    "issuer" to issuerId,
                    "inheritors" to inheritors,
                ).toJson(),
                outcome = outcome,
            )
        }
    }

    @Nested
    inner class RevokeTestament {
        @Test
        fun `Should revoke testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)

            // when
            revokeTestament(issuerId)

            // then
            val stored = retrieveTestament(issuerId, 0)
            stored["issuer"] shouldBe issuerId
            stored.getJSONObject("inheritors").toMap() shouldBe inheritors
            stored["revoked"] shouldBe true
        }

        @Test
        fun `Only provider should revoke testaments`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }

            // when
            withNode(BANK) {
                revokeTestament(issuerId) {
                    // then
                    failure()
                }
            }
        }

        @Test
        fun `Should not revoke revoked testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)
            revokeTestament(issuerId)

            // when
            revokeTestament(issuerId) {
                // then
                failure("revoked")
            }
        }

        @Test
        fun `Should not revoke announced testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            withNode(PROVIDER) {
                // when
                revokeTestament(issuerId) {
                    // then
                    failure("announced")
                }
            }
        }
    }

    @Nested
    inner class StoreGold {
        @Test
        fun `Should store gold`() = withNode(BANK) {
            // given
            val holderId = UUID.randomUUID().toString()
            val amount = 3000

            // when
            storeGold(holderId, amount)

            // then
            val stored = retrieveAccount(holderId)
            stored["holder"] shouldBe holderId
            stored["amount"] shouldBe amount.toString()
        }

        @Test
        fun `Should add gold to existing account`() = withNode(BANK) {
            // given
            val holderId = UUID.randomUUID().toString()
            val initial = 3000
            storeGold(holderId, initial)
            val additional = 2000

            // when
            storeGold(holderId, additional)

            // then
            val stored = retrieveAccount(holderId, 0)
            stored["holder"] shouldBe holderId
            stored["amount"] shouldBe (initial + additional).toString()
        }

        @Test
        fun `Should not store gold outside of bank`() = withNode(PROVIDER) {
            // given+when
            storeGold(UUID.randomUUID().toString(), 3000) {
                // then
                failure(BANK)
            }
        }

        @Test
        fun `Should not store gold to announced testament account`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            // when
            withNode(BANK) {
                storeGold(issuerId, 3000) {
                    // then
                    failure("announced")
                }
            }
        }
    }

    @Nested
    inner class WithdrawGold {
        @Test
        fun `Should withdraw gold`() = withNode(BANK) {
            // given
            val holderId = UUID.randomUUID().toString()
            val possession = 3000
            storeGold(holderId, possession)
            val withdraw = 2000

            // when
            withdrawGold(holderId, withdraw)

            // then
            val stored = retrieveAccount(holderId, 0)
            stored["holder"] shouldBe holderId
            stored["amount"] shouldBe (possession - withdraw).toString()
        }

        @Test
        fun `Should withdraw all gold`() = withNode(BANK) {
            // given
            val holderId = UUID.randomUUID().toString()
            val possession = 3000
            storeGold(holderId, possession)

            // when
            withdrawGold(holderId, possession)

            // then
            val stored = retrieveAccount(holderId, 0)
            stored["holder"] shouldBe holderId
            stored["amount"] shouldBe "0"
        }

        @Test
        fun `Should not withdraw below 0`() = withNode(BANK) {
            // given
            val holderId = UUID.randomUUID().toString()
            val possession = 1000
            storeGold(holderId, possession)
            val withdraw = 2000

            // when
            withdrawGold(holderId, withdraw) {
                // then
                failure("not enough")
            }
        }

        @Test
        fun `Should not withdraw gold outside of bank`() {
            // given
            withNode(BANK) {
                val holderId = UUID.randomUUID().toString()
                val possession = 1000
                storeGold(holderId, possession)
            }

            withNode(PROVIDER) {
                // when
                withdrawGold(UUID.randomUUID().toString(), 1000) {
                    // then
                    failure(BANK)
                }
            }
        }

        private fun withdrawGold(
            holderId: String,
            amount: Int,
            outcome: FlowId.() -> Unit = { success() },
        ) {
            startFlow(
                flowName = WithdrawGoldFlow::class.java.name,
                parametersInJson = mapOf(
                    "holder" to holderId,
                    "amount" to amount.toString(),
                ).toJson(),
                outcome = outcome,
            )
        }
    }

    @Nested
    inner class AnnounceTestament {

        @Test
        fun `Should announce testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }

            // when
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            // then
            withNode(BANK) {
                val stored = retrieveTestament(issuerId)
                stored["issuer"] shouldBe issuerId
                stored.getJSONObject("inheritors").toMap() shouldBe inheritors
                stored["announced"] shouldBe true
            }
        }

        @Test
        fun `Only government should announce testament`() = withNode(PROVIDER) {
            // given
            val issuerId = UUID.randomUUID().toString()

            val inheritors = mapOf("1" to 6000, "2" to 4000)
            issueTestament(issuerId, inheritors)

            // when
            announceTestament(issuerId) {
                // then
                failure(GOVERNMENT)
            }
        }

        @Test
        fun `Should not announce announced testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)

                // when
                announceTestament(issuerId) {
                    // then
                    failure("announced")
                }
            }
        }

        @Test
        fun `Should not announce revoked testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val inheritors = mapOf("1" to 6000, "2" to 4000)
            withNode(PROVIDER) {
                issueTestament(issuerId, inheritors)
                revokeTestament(issuerId)
            }

            withNode(GOVERNMENT) {
                // when
                announceTestament(issuerId) {
                    // then
                    failure("revoked")
                }
            }
        }
    }

    @Nested
    inner class ExecuteTestament {
        @Test
        fun `Should execute testament`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            val firstInheritor = UUID.randomUUID().toString()
            val secondInheritor = UUID.randomUUID().toString()
            withNode(PROVIDER) {
                issueTestament(issuerId, mapOf(firstInheritor to 6000, secondInheritor to 4000))
            }
            withNode(BANK) {
                storeGold(issuerId, 3000)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            withNode(BANK) {
                // when
                executeTestament(issuerId)

                // then
                retrieveTestament(issuerId, 2)["executed"] shouldBe true
                retrieveAccount(firstInheritor)["amount"] shouldBe "1800"
                retrieveAccount(secondInheritor)["amount"] shouldBe "1200"
                retrieveAccount(issuerId, 1)["amount"] shouldBe "0"
            }
        }

        @Test
        fun `Only bank should execute testaments`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            withNode(PROVIDER) {
                issueTestament(issuerId, mapOf("1" to 6000, "2" to 4000))
            }
            withNode(BANK) {
                storeGold(issuerId, 3000)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            withNode(GOVERNMENT) {
                // when
                executeTestament(issuerId) {
                    // then
                    failure(BANK)
                }
            }
        }

        @Test
        fun `Should not execute if not announced`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            withNode(PROVIDER) {
                issueTestament(issuerId, mapOf("1" to 6000, "2" to 4000))
            }
            withNode(BANK) {
                storeGold(issuerId, 3000)

                // when
                executeTestament(issuerId) {
                    // then
                    failure()
                }
            }
        }

        @Test
        fun `Should not execute if already executed`() {
            // given
            val issuerId = UUID.randomUUID().toString()
            withNode(PROVIDER) {
                issueTestament(issuerId, mapOf("1" to 6000, "2" to 4000))
            }
            withNode(BANK) {
                storeGold(issuerId, 3000)
            }
            withNode(GOVERNMENT) {
                announceTestament(issuerId)
            }

            withNode(BANK) {
                executeTestament(issuerId)

                // when
                executeTestament(issuerId) {
                    // then
                    failure("executed")
                }
            }
        }

        private fun executeTestament(
            issuerId: String,
            outcome: FlowId.() -> Unit = { success() },
        ) {
            startFlow(
                flowName = ExecuteTestamentFlow::class.java.name,
                parametersInJson = mapOf(
                    "issuer" to issuerId,
                ).toJson(),
                outcome = outcome,
            )
        }
    }

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

    private fun issueTestament(
        issuerId: String,
        inheritors: Map<String, Int>,
        outcome: FlowId.() -> Unit = { success() },
    ) {
        startFlow(
            flowName = IssueTestamentFlow::class.java.name,
            parametersInJson = mapOf(
                "issuer" to issuerId,
                "inheritors" to inheritors,
            ).toJson(),
            outcome = outcome,
        )
    }

    private fun revokeTestament(
        issuerId: String,
        outcome: FlowId.() -> Unit = { success() },
    ) {
        startFlow(
            flowName = RevokeTestamentFlow::class.java.name,
            parametersInJson = mapOf(
                "issuer" to issuerId,
            ).toJson(),
            outcome = outcome,
        )
    }

    private fun announceTestament(
        issuerId: String,
        outcome: FlowId.() -> Unit = { success() },
    ) {
        startFlow(
            flowName = AnnounceTestamentFlow::class.java.name,
            parametersInJson = mapOf(
                "issuer" to issuerId,
            ).toJson(),
            outcome = outcome,
        )
    }

    private fun storeGold(
        holderId: String,
        amount: Int,
        outcome: FlowId.() -> Unit = { success() },
    ) {
        startFlow(
            flowName = StoreGoldFlow::class.java.name,
            parametersInJson = mapOf(
                "holder" to holderId,
                "amount" to amount.toString(),
            ).toJson(),
            outcome = outcome,
        )
    }

    private fun Any.toJson(): String = Gson().toJson(this)

    private fun startFlow(
        flowName: String,
        parametersInJson: String,
        clientId: String = UUID.randomUUID().toString(),
        outcome: FlowId.() -> Unit = { success() },
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

    private fun FlowId.success() {
        eventually {
            with(retrieveOutcome()) {
                status shouldBe HttpStatus.OK
                body.`object`["status"] shouldBe "COMPLETED"
            }
        }
    }

    private fun FlowId.failure(message: String = "") {
        eventually {
            with(retrieveOutcome()) {
                status shouldBe HttpStatus.OK
                body.`object`["status"] shouldBe "FAILED"
                body.`object`.getJSONObject("exceptionDigest")
                    .getString("message") shouldContainIgnoringCase message
            }
        }
    }

    private fun FlowId.retrieveOutcome(): HttpResponse<JsonNode> {
        val request = Unirest.get("flowstarter/flowoutcome/$this")
            .header("Content-Type", "application/json")
        return request.asJson()
    }

    private fun retrieveTestament(issuerId: String, position: Int = -1): JSONObject =
        retrieveState(
            "TestamentSchemaV1.PersistentTestament.findByIssuerId",
            mapOf(
                "issuerId" to issuerId
            ),
            TestamentPostProcessor::class,
            position,
        )

    private fun retrieveAccount(holderId: String, position: Int = -1): JSONObject =
        retrieveState(
            "AccountSchemaV1.PersistentAccount.findByHolderId",
            mapOf(
                "holderId" to holderId
            ),
            AccountPostProcessor::class,
            position,
        )

    private fun retrieveState(
        query: String,
        params: Map<String, Any>,
        processor: KClass<out ToDtoPostProcessor<*, *>>,
        position: Int = -1,
    ): JSONObject {
        val body = mapOf(
            "request" to mapOf(
                "namedParameters" to
                        params.mapValues { mapOf("parametersInJson" to it.value.toJson()) },
                "queryName" to query,
                "postProcessorName" to processor.qualifiedName
            ),
            "context" to mapOf(
                "awaitForResultTimeout" to "PT15M",
                "currentPosition" to position,
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