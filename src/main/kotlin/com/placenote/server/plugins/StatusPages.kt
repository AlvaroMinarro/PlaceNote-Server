package com.placenote.server.plugins

import com.placenote.server.api.ApiException
import com.placenote.server.api.ProblemDetails
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.statusCode,
                ProblemDetails(
                    status = cause.statusCode.value,
                    detail = cause.message,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ProblemDetails(
                    status = 500,
                    detail = "Error interno",
                ),
            )
        }
    }
}
