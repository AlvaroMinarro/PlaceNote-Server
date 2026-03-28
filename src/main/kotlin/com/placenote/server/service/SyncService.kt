package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.FriendshipCreate
import com.placenote.server.api.FriendshipDto
import com.placenote.server.api.FriendshipPatch
import com.placenote.server.api.RatingDto
import com.placenote.server.api.ReviewCreate
import com.placenote.server.api.ReviewDto
import com.placenote.server.api.RatingUpsert
import com.placenote.server.api.SyncOperationDto
import com.placenote.server.api.SyncPullResponse
import com.placenote.server.api.SyncPushRequest
import com.placenote.server.api.SyncPushResponse
import com.placenote.server.api.SyncPushResult
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object SyncService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun push(userId: UUID, req: SyncPushRequest): SyncPushResponse {
        val results = req.operations.map { op ->
            runCatching { applyOne(userId, op) }
                .fold(
                    onSuccess = { entity -> SyncPushResult(op.id, "APPLIED", entity) },
                    onFailure = { SyncPushResult(op.id, "REJECTED", null) },
                )
        }
        return SyncPushResponse(results = results)
    }

    private suspend fun applyOne(userId: UUID, op: SyncOperationDto): JsonElement? {
        when (op.nombreEntidad) {
            "REVIEW" -> when (op.accion) {
                "INSERT" -> {
                    val payload = op.payload ?: throw ApiException(HttpStatusCode.BadRequest, "payload requerido")
                    val create = json.decodeFromJsonElement<ReviewCreate>(payload)
                    val dto = ReviewService.create(userId, create)
                    return json.encodeToJsonElement(ReviewDto.serializer(), dto)
                }
                "UPDATE" -> {
                    val payload = op.payload ?: throw ApiException(HttpStatusCode.BadRequest, "payload requerido")
                    val full = json.decodeFromJsonElement<ReviewDto>(payload)
                    val dto = ReviewService.put(userId, full)
                    return json.encodeToJsonElement(ReviewDto.serializer(), dto)
                }
                "DELETE" -> {
                    val id = UUID.fromString(op.idEntidad)
                    ReviewService.delete(userId, id)
                    return null
                }
                else -> throw ApiException(HttpStatusCode.BadRequest, "acción inválida")
            }
            "RATING" -> {
                val reviewId = UUID.fromString(op.idEntidad)
                val payload = op.payload ?: throw ApiException(HttpStatusCode.BadRequest, "payload requerido")
                val upsert = json.decodeFromJsonElement<RatingUpsert>(payload)
                val dto = RatingService.upsert(reviewId, userId, upsert)
                return json.encodeToJsonElement(RatingDto.serializer(), dto)
            }
            "FRIENDSHIP" -> when (op.accion) {
                "INSERT" -> {
                    val payload = op.payload ?: throw ApiException(HttpStatusCode.BadRequest, "payload requerido")
                    val fc = json.decodeFromJsonElement<FriendshipCreate>(payload)
                    val f = FriendshipService.create(userId, fc)
                    return json.encodeToJsonElement(FriendshipDto.serializer(), f)
                }
                "UPDATE" -> {
                    val payload = op.payload ?: throw ApiException(HttpStatusCode.BadRequest, "payload requerido")
                    val patch = json.decodeFromJsonElement<FriendshipPatch>(payload)
                    val f = FriendshipService.patch(UUID.fromString(op.idEntidad), userId, patch)
                    return json.encodeToJsonElement(FriendshipDto.serializer(), f)
                }
                else -> throw ApiException(HttpStatusCode.BadRequest, "acción inválida")
            }
            else -> throw ApiException(HttpStatusCode.BadRequest, "entidad desconocida")
        }
    }

    suspend fun pull(userId: UUID, since: String?): SyncPullResponse {
        val sinceTime = since?.let {
            runCatching { OffsetDateTime.parse(it) }.getOrElse {
                throw ApiException(HttpStatusCode.BadRequest, "since inválido")
            }
        } ?: OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val changes = mutableListOf<SyncOperationDto>()
        val reviews = ReviewService.listVisible(userId, 500).items.filter { r ->
            val m = r.modifiedAt?.let { OffsetDateTime.parse(it) } ?: return@filter false
            m.isAfter(sinceTime)
        }
        for (r in reviews) {
            changes.add(
                SyncOperationDto(
                    id = UUID.randomUUID().toString(),
                    nombreEntidad = "REVIEW",
                    idEntidad = r.id,
                    accion = "UPDATE",
                    clientTimestamp = r.modifiedAt ?: OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    payload = json.encodeToJsonElement(ReviewDto.serializer(), r),
                ),
            )
        }
        val next = OffsetDateTime.now(ZoneOffset.UTC).toString()
        return SyncPullResponse(changes = changes, nextCursor = next)
    }
}
