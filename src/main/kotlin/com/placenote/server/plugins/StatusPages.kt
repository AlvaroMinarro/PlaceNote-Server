package com.placenote.server.plugins

import com.placenote.server.api.ApiException
import com.placenote.server.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respondApiError(
                cause.statusCode,
                cause.message ?: "Error",
                cause.statusCode.value,
            )
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respondApiError(
                HttpStatusCode.InternalServerError,
                "Error interno",
                HttpStatusCode.InternalServerError.value,
            )
        }
    }
}
