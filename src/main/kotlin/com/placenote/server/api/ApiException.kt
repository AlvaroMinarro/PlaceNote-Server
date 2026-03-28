package com.placenote.server.api

import io.ktor.http.HttpStatusCode

class ApiException(
    val statusCode: HttpStatusCode,
    message: String,
) : Exception(message)
