package com.example

import com.fasterxml.jackson.databind.ObjectMapper
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path

@Path("/testaments/{issuer}")
class TestamentResource(
    val mapper: ObjectMapper,
) {
    @GET
    fun fetchTestament(issuer: String) = "Retrieving testament for $issuer"

    @PUT
    fun updateTestament(issuer: String, inheritors: Map<String, Int>) =
        "Updating inheritors for $issuer with ${mapper.writeValueAsString(inheritors)}"

    @DELETE
    fun revokeTestament(issuer: String) = "Revoking testament of $issuer"
}
