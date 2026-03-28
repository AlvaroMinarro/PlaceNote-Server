package com.placenote.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String, val service: String)

@Serializable
data class ApiInfoResponse(
    val name: String,
    val version: String,
    val apiVersion: String,
    val documentation: String,
)

@Serializable
data class NotImplementedBody(val detail: String)

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok", service = "placeNote-server"))
        }
        route("/api/v1") {
            get {
                call.respond(
                    ApiInfoResponse(
                        name = "PlaceNote API",
                        version = "0.1.0-SNAPSHOT",
                        apiVersion = "v1",
                        documentation = "See docs/api/openapi.yaml in the PlaceNote-Server repository",
                    ),
                )
            }
        }
        get("/api/v1/me") {
            call.respond(
                HttpStatusCode.NotImplemented,
                NotImplementedBody(detail = "Authentication and user profile are not implemented yet."),
            )
        }
    }
}
