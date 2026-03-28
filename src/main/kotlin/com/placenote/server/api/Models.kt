package com.placenote.server.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
data class ProblemDetails(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long? = null,
)

@Serializable
data class UserPatch(
    val name: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class FriendshipDto(
    val id: String,
    val userAId: String,
    val userBId: String,
    val state: String,
)

@Serializable
data class FriendshipCreate(
    val targetUserId: String,
)

/** Body para `POST /api/v1/friends/request` (`user_id` en JSON). */
@Serializable
data class FriendRequestBody(
    @SerialName("user_id") val userId: String,
)

@Serializable
data class DeleteSuccess(
    val deleted: Boolean = true,
)

@Serializable
data class FriendshipPatch(
    val state: String,
)

@Serializable
data class FriendshipPage(
    val items: List<FriendshipDto>,
    val nextCursor: String? = null,
)

@Serializable
data class ReviewDto(
    val id: String,
    val nombreSitio: String,
    val latitud: Double,
    val longitud: Double,
    val fotoUrl: String? = null,
    val ocrTextoCrudo: String? = null,
    val precioTotal: Double? = null,
    val fechaVisita: String,
    val creadorId: String,
    val modifiedAt: String? = null,
)

@Serializable
data class ReviewCreate(
    val id: String,
    val nombreSitio: String,
    val latitud: Double,
    val longitud: Double,
    val fotoUrl: String? = null,
    val ocrTextoCrudo: String? = null,
    val precioTotal: Double? = null,
    val fechaVisita: String,
)

@Serializable
data class ReviewPage(
    val items: List<ReviewDto>,
    val nextCursor: String? = null,
)

@Serializable
data class RatingDto(
    val reviewId: String,
    val userId: String,
    val nota: Double,
    val comentarioPrivado: String? = null,
    val modifiedAt: String? = null,
)

@Serializable
data class RatingUpsert(
    val nota: Double,
    val comentarioPrivado: String? = null,
)

@Serializable
data class SyncPushRequest(
    val operations: List<SyncOperationDto>,
)

@Serializable
data class SyncOperationDto(
    val id: String,
    val nombreEntidad: String,
    val idEntidad: String,
    val accion: String,
    val clientTimestamp: String,
    val payload: JsonElement? = null,
)

@Serializable
data class SyncPushResult(
    val operationId: String,
    val status: String,
    val serverEntity: JsonElement? = null,
)

@Serializable
data class SyncPushResponse(
    val results: List<SyncPushResult>,
)

@Serializable
data class SyncPullResponse(
    val changes: List<SyncOperationDto>,
    val nextCursor: String,
)
