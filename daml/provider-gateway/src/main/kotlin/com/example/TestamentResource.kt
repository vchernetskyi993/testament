package com.example

import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path

@Path("/testaments")
class TestamentResource(
    val service: TestamentService,
) {
    @POST
    suspend fun issueTestament(testament: Testament) = service.issueTestament(testament)

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
