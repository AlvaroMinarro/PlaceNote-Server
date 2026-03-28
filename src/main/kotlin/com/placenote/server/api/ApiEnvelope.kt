package com.placenote.server.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** JSON compartido para serializar envoltorios y payloads `data`. */
val apiJson = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class ApiEnvelopeSuccess(
    val status: String = "success",
    val data: JsonElement,
)

@Serializable
data class ApiEnvelopeError(
    val status: String = "error",
    val message: String,
    val code: Int,
)

suspend inline fun <reified T> ApplicationCall.respondApiSuccess(
    data: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val element: JsonElement = apiJson.encodeToJsonElement(data)
    respond(status, ApiEnvelopeSuccess(data = element))
}

suspend fun ApplicationCall.respondApiError(
    httpStatus: HttpStatusCode,
    message: String,
    code: Int = httpStatus.value,
) {
    respond(httpStatus, ApiEnvelopeError(message = message, code = code))
}
