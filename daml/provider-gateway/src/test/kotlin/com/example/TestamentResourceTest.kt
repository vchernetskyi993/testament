package com.example

import com.daml.ledger.api.v1.CommandServiceGrpc
import com.daml.ledger.api.v1.CommandsOuterClass
import com.daml.ledger.javaapi.data.DamlGenMap
import com.daml.ledger.javaapi.data.DamlRecord
import com.daml.ledger.javaapi.data.DamlRecord.Field
import com.daml.ledger.javaapi.data.ExerciseByKeyCommand
import com.daml.ledger.javaapi.data.ExerciseCommand
import com.daml.ledger.javaapi.data.Int64
import com.daml.ledger.javaapi.data.Party
import com.daml.ledger.javaapi.data.Text
import com.example.main.factory.TestamentFactory
import com.example.main.testament.Testament
import com.example.util.GrpcMockExtension
import com.example.util.InjectGrpcMock
import com.example.util.InjectWireMock
import com.example.util.WireMockExtension
import com.example.util.WireMockExtension.Companion.AUTH_TOKEN
import com.example.util.mockAuth
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.google.protobuf.Empty
import com.jayway.jsonpath.matchers.JsonPathMatchers.isJson
import com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath
import io.grpc.Status
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse
import org.grpcmock.GrpcMock
import org.grpcmock.GrpcMock.calledMethod
import org.grpcmock.GrpcMock.times
import org.grpcmock.GrpcMock.unaryMethod
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@TestProfile(TestamentResourceTest.Profile::class)
@QuarkusTestResource(WireMockExtension::class)
@QuarkusTestResource(GrpcMockExtension::class)
class TestamentResourceTest {
    companion object {
        private const val APP_ID = "test-app-id"
        private const val PARTY = "test-party"
        private const val FACTORY_ID = "test-factory-id"
        private const val GOVERNMENT = "test-government"

        private val firstInheritor = UUID.randomUUID().toString() to 4500
        private val secondInheritor = UUID.randomUUID().toString() to 5500
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "daml.app-id" to APP_ID,
            "daml.party" to PARTY,
            "daml.factory-id" to FACTORY_ID,
            "daml.government" to GOVERNMENT,
            "quarkus.http.test-port" to "0",
        )
    }

    @InjectGrpcMock
    lateinit var grpcMock: GrpcMock

    @InjectWireMock
    lateinit var wireMockServer: WireMockServer

    @BeforeAll
    fun beforeAll() {
        GrpcMock.configureFor(grpcMock)
        WireMock.configureFor(wireMockServer.port())
    }

    @BeforeEach
    fun beforeEach() {
        GrpcMock.resetMappings()
        WireMock.reset()
        mockAuth()
    }

    @Test
    fun `Should fetch testament`() {
        // given
        val issuer = UUID.randomUUID().toString()
        WireMock.stubFor(
            post("/fetch")
                .withHeader("Authorization", equalTo("Bearer $AUTH_TOKEN"))
                .withRequestBody(matchingJsonPath("$.key._1", equalTo(GOVERNMENT)))
                .withRequestBody(matchingJsonPath("$.key._2", equalTo(issuer)))
                .willReturn(
                    okJson(
                        """{
                       |"result": {
                       |  "payload": {
                       |    "issuer": "$issuer",
                       |    "inheritors": [
                       |      ["${firstInheritor.first}", ${firstInheritor.second}], 
                       |      ["${secondInheritor.first}", ${secondInheritor.second}]
                       |    ],
                       |    "announced": true
                       |  }
                       |}
                       |}""".trimMargin()
                    )
                )
        )

        // when+then
        fetchTestament(issuer)
            .statusCode(200)
            .body(
                isJson(
                    allOf(
                        withJsonPath("$.issuer", `is`(issuer)),
                        withJsonPath("$.announced", `is`(true)),
                        withJsonPath("$.executed", `is`(false)),
                        withJsonPath(
                            "$.inheritors.${firstInheritor.first}",
                            `is`(firstInheritor.second)
                        ),
                        withJsonPath(
                            "$.inheritors.${secondInheritor.first}",
                            `is`(secondInheritor.second)
                        ),
                    )
                )
            )
    }

    @Test
    fun `Should return not found for non-existing testament`() {
        fetchTestament(UUID.randomUUID().toString()).statusCode(404)
    }

    @Test
    fun `Should issue testament`() {
        // given
        val issuer = UUID.randomUUID().toString()
        GrpcMock.stubFor(
            unaryMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .willReturn(Empty.getDefaultInstance())
        )

        // when
        issueTestament(issuer).statusCode(204)

        // then
        var command: CommandsOuterClass.Commands? = null
        GrpcMock.verifyThat(
            calledMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .withHeader("Authorization", "Bearer $AUTH_TOKEN")
                .withRequest {
                    command = it.commands
                    true
                },
            times(1)
        )
        command?.applicationId shouldBe APP_ID
        command?.party shouldBe PARTY
        command?.commandsList?.shouldHaveSingleElement(
            ExerciseCommand(
                TestamentFactory.TEMPLATE_ID,
                FACTORY_ID,
                "IssueTestament",
                DamlRecord(
                    Field("issuer", Text(issuer)),
                    Field(
                        "inheritors", DamlGenMap.of(
                            mapOf(
                                Text(firstInheritor.first) to Int64(
                                    firstInheritor.second.toLong()
                                ),
                                Text(secondInheritor.first) to Int64(
                                    secondInheritor.second.toLong()
                                )
                            )
                        )
                    ),
                ),
            ).toProtoCommand()
        )
    }

    @Test
    fun `Should return bad request for existing testament on issue`() {
        // given
        val issuer = UUID.randomUUID().toString()
        GrpcMock.stubFor(
            unaryMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .willReturn(Status.ALREADY_EXISTS)
        )

        // when+then
        issueTestament(issuer).statusCode(400)
    }

    @Test
    fun `Should update testament`() {
        // given
        val issuer = UUID.randomUUID().toString()
        GrpcMock.stubFor(
            unaryMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .willReturn(Empty.getDefaultInstance())
        )

        // when
        updateTestament(issuer).statusCode(204)

        // then
        var command: CommandsOuterClass.Commands? = null
        GrpcMock.verifyThat(
            calledMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .withHeader("Authorization", "Bearer $AUTH_TOKEN")
                .withRequest {
                    command = it.commands
                    true
                },
            times(1)
        )
        command?.applicationId shouldBe APP_ID
        command?.party shouldBe PARTY
        command?.commandsList?.shouldHaveSingleElement(
            ExerciseByKeyCommand(
                Testament.TEMPLATE_ID,
                DamlRecord(
                    Field("_1", Party(GOVERNMENT)),
                    Field("_2", Text(issuer)),
                ),
                "UpdateInheritors",
                DamlRecord(
                    Field(
                        "updatedInheritors", DamlGenMap.of(
                            mapOf(
                                Text(firstInheritor.first) to Int64(
                                    firstInheritor.second.toLong()
                                ),
                                Text(secondInheritor.first) to Int64(
                                    secondInheritor.second.toLong()
                                ),
                            )
                        )
                    )
                ),
            ).toProtoCommand()
        )
    }

    @Test
    fun `Should return not found for non-existing testament on update`() {
        // given
        val issuer = UUID.randomUUID().toString()
        GrpcMock.stubFor(
            unaryMethod(CommandServiceGrpc.getSubmitAndWaitMethod())
                .willReturn(Status.NOT_FOUND)
        )

        // when+then
        updateTestament(issuer).statusCode(404)
    }

    @Test
    fun `Should revoke testament`() {
    }

    @Test
    fun `Should return not found for non-existing testament on revoke`() {
    }

    private fun fetchTestament(issuer: String): ValidatableResponse =
        given()
            .pathParam("issuer", issuer)
            .`when`().get("/testaments/{issuer}")
            .then()

    private fun issueTestament(issuer: String): ValidatableResponse =
        given()
            .header("Content-Type", "application/json")
            .body(
                """
                {
                  "issuer": "$issuer",
                  "inheritors": {
                    "${firstInheritor.first}": ${firstInheritor.second},
                    "${secondInheritor.first}": ${secondInheritor.second}
                  }
                }
            """.trimIndent()
            )
            .`when`().post("/testaments")
            .then()

    private fun updateTestament(issuer: String): ValidatableResponse =
        given()
            .header("Content-Type", "application/json")
            .pathParam("issuer", issuer)
            .body(
                """
                {
                  "${firstInheritor.first}": ${firstInheritor.second},
                  "${secondInheritor.first}": ${secondInheritor.second}
                }
            """.trimIndent()
            )
            .`when`().put("/testaments/{issuer}")
            .then()
}