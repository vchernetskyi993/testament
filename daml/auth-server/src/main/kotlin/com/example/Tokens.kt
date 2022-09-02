package com.example

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID


private val jwk: RSAKey = RSAKeyGenerator(2048)
    .keyUse(KeyUse.SIGNATURE)
    .keyID(UUID.randomUUID().toString())
    .generate()

private val signer = RSASSASigner(jwk)

val jwks = JWKSet(jwk)

fun jwt(settings: Credentials): String {
    val claimsSet = JWTClaimsSet.Builder()
        .subject(settings.user)
        .claim("scope", "daml_ledger_api")
        .expirationTime(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
        .build()
    val signedJWT = SignedJWT(
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(jwk.keyID)
            .build(),
        claimsSet
    )
    signedJWT.sign(signer)
    return signedJWT.serialize()
}
