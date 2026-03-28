package com.placenote.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.placenote.server.config.JwtConfig
import java.util.Date
import java.util.UUID

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)
    val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun createAccessToken(userId: UUID): String =
        JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + config.accessTtlSeconds * 1000))
            .sign(algorithm)

    fun decode(token: String): DecodedJWT = verifier.verify(token)
}
