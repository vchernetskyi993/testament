package com.example

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.jboss.resteasy.reactive.ClientWebApplicationException
import org.jboss.resteasy.reactive.RestResponse.StatusCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


private val logger: Logger = LoggerFactory.getLogger("com.example.ExceptionsKt")

@Provider
class ClientWebApplicationExceptionHandler : ExceptionMapper<ClientWebApplicationException> {
    override fun toResponse(exception: ClientWebApplicationException): Response {
        logger.error(exception.message, exception)
        val status = exception.response?.status ?: StatusCode.INTERNAL_SERVER_ERROR
        return Response.status(status).build()
    }

}

@Provider
class ExecutionExceptionHandler : ExceptionMapper<ExecutionException> {

    override fun toResponse(exception: ExecutionException): Response {
        logger.error(exception.message, exception)
        when (val cause = getCause(exception)) {
            is StatusRuntimeException -> {
                val status = when (cause.status.code) {
                    Status.Code.ALREADY_EXISTS -> StatusCode.BAD_REQUEST
                    Status.Code.NOT_FOUND -> StatusCode.NOT_FOUND
                    else -> StatusCode.INTERNAL_SERVER_ERROR
                }
                return Response.status(status).build()
            }
        }
        return Response.status(StatusCode.INTERNAL_SERVER_ERROR).build()
    }

    private fun getCause(e: Throwable): Throwable =
        when (e) {
            is ExecutionException -> getCause(e.cause!!)
            else -> e
        }
}
