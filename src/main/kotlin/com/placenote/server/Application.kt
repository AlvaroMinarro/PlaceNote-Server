package com.placenote.server

import com.placenote.server.config.AppConfig
import com.placenote.server.db.DatabaseFactory
import com.placenote.server.plugins.configureApiRouting
import com.placenote.server.plugins.configureJwtAuth
import com.placenote.server.plugins.configureStatusPages
import com.placenote.server.security.JwtService
import com.placenote.server.service.AuthService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnvironment()) {
    DatabaseFactory.init(config.database)

    val jwtService = JwtService(config.jwt)
    val authService = AuthService(config.jwt, jwtService)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
    configureStatusPages()
    configureJwtAuth(config.jwt, jwtService)
    configureApiRouting(authService)
}
