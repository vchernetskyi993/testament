package com.example

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
import com.jayway.jsonpath.matchers.JsonPathMatchers.isJson
import com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.grpcmock.GrpcMock
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
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "daml.app-id" to APP_ID,
            "daml.party" to PARTY,
            "daml.factory-id" to FACTORY_ID,
            "daml.government" to GOVERNMENT,
        )
    }

    @InjectGrpcMock
    lateinit var grpcMock: GrpcMock

    @InjectWireMock
    lateinit var wireMockServer: WireMockServer

    @BeforeAll
    fun beforeAll() {
        GrpcMock.configureFor(grpcMock.port)
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
        val issuer = UUID.randomUUID().toString()
        val firstInheritor = UUID.randomUUID().toString() to 4500
        val secondInheritor = UUID.randomUUID().toString() to 5500
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

        given()
            .pathParam("issuer", issuer)
            .`when`().get("/testaments/{issuer}")
            .then()
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
        given()
            .pathParam("issuer", UUID.randomUUID().toString())
            .`when`().get("/testaments/{issuer}")
            .then()
            .statusCode(404)
    }

    @Test
    fun `Should issue testament`() {
    }

    @Test
    fun `Should return bad request for existing testament on issue`() {
    }

    @Test
    fun `Should update testament`() {
    }

    @Test
    fun `Should return not found for non-existing testament on update`() {
    }

    @Test
    fun `Should revoke testament`() {
    }

    @Test
    fun `Should return not found for non-existing testament on revoke`() {
    }
}