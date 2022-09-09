package com.example

import org.jboss.resteasy.reactive.ClientWebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ClientWebApplicationExceptionHandler : ExceptionMapper<ClientWebApplicationException> {
    override fun toResponse(exception: ClientWebApplicationException): Response =
        Response.status(exception.response?.status ?: 500)
            .build()
}
