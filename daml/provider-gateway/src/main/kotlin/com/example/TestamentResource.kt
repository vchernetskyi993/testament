package com.example

import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path

data class TestamentDto(
    val issuer: String,
    val inheritors: Map<String, Int>,
    val announced: Boolean = false,
    val executed: Boolean = false,
)

@Path("/testaments")
class TestamentResource(
    val service: TestamentService,
) {
    @POST
    suspend fun issueTestament(testament: TestamentDto) = service.issueTestament(testament)

    @GET
    @Path("/{issuer}")
    suspend fun fetchTestament(issuer: String) = service.fetchTestament(issuer)

    @PUT
    @Path("/{issuer}")
    suspend fun updateTestament(issuer: String, inheritors: Map<String, Int>) =
        service.updateTestament(issuer, inheritors)

    @DELETE
    @Path("/{issuer}")
    suspend fun revokeTestament(issuer: String) = service.revokeTestament(issuer)
}
