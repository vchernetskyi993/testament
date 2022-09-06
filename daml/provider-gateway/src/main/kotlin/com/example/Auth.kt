package com.example

import io.smallrye.config.ConfigMapping
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.POST
import javax.ws.rs.Path

data class JwtResponse(
    val token: String,
)

data class LoginRequest(
    val user: String,
    val password: String,
)

@ConfigMapping(prefix = "daml.ledger.auth")
interface LedgerAuthProperties {
    fun username(): String
    fun password(): String
}

@RegisterRestClient(configKey = "auth")
interface AuthClient {
    @POST
    @Path("/authenticate")
    suspend fun authenticate(input: LoginRequest): JwtResponse
}

@ApplicationScoped
class AuthService(
    val props: LedgerAuthProperties,
    @RestClient val client: AuthClient,
) {
    suspend fun jwt(): String =
        client.authenticate(LoginRequest(props.username(), props.password())).token
}
