package com.example

import com.example.util.GrpcMockExtension
import com.example.util.WireMockExtension
import com.jayway.jsonpath.matchers.JsonPathMatchers.isJson
import com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@TestProfile(TestamentResourceTest.Profile::class)
@QuarkusTestResource(WireMockExtension::class, parallel = true)
@QuarkusTestResource(GrpcMockExtension::class, parallel = true)
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


    @Test
    fun `Should fetch testament`() {
        val issuer = UUID.randomUUID().toString()
        val firstInheritor = UUID.randomUUID().toString() to 4500
        val secondInheritor = UUID.randomUUID().toString() to 5500

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