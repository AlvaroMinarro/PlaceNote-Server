package com.placenote.server.plugins

import com.placenote.server.config.JwtConfig
import com.placenote.server.security.JwtService
import com.placenote.server.security.UserIdPrincipal
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import java.util.UUID

fun Application.configureJwtAuth(jwtConfig: JwtConfig, jwtService: JwtService) {
    install(Authentication) {
        jwt("jwt") {
            realm = jwtConfig.realm
            verifier(jwtService.verifier)
            validate { credential ->
                val sub = credential.payload.subject ?: return@validate null
                UserIdPrincipal(UUID.fromString(sub))
            }
        }
    }
}
