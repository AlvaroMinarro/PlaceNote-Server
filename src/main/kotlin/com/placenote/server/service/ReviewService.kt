package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.ReviewCreate
import com.placenote.server.api.ReviewDto
import com.placenote.server.api.ReviewPage
import com.placenote.server.db.Friendships
import com.placenote.server.db.Reviews
import com.placenote.server.db.dbQuery
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

object ReviewService {
    private val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun ResultRow.toDto() = ReviewDto(
        id = this[Reviews.id].toString(),
        nombreSitio = this[Reviews.nombreSitio],
        latitud = this[Reviews.latitud],
        longitud = this[Reviews.longitud],
        fotoUrl = this[Reviews.fotoUrl],
        ocrTextoCrudo = this[Reviews.ocrTextoCrudo],
        precioTotal = this[Reviews.precioTotal],
        fechaVisita = isoFmt.format(this[Reviews.fechaVisita]),
        creadorId = this[Reviews.creadorId].toString(),
        modifiedAt = isoFmt.format(this[Reviews.modifiedAt]),
    )

    suspend fun canViewReview(viewerId: UUID, creatorId: UUID): Boolean {
        if (viewerId == creatorId) return true
        val low = minOf(viewerId, creatorId)
        val high = maxOf(viewerId, creatorId)
        return dbQuery {
            Friendships.selectAll()
                .where {
                    (Friendships.userAId eq low) and
                        (Friendships.userBId eq high) and
                        (Friendships.state eq "ACCEPTED")
                }
                .count() > 0
        }
    }

    suspend fun get(reviewId: UUID, viewerId: UUID): ReviewDto {
        val (dto, creator) = dbQuery {
            val row = Reviews.selectAll().where { Reviews.id eq reviewId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            if (row[Reviews.deletedAt] != null) {
                throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            }
            row.toDto() to row[Reviews.creadorId]
        }
        if (!canViewReview(viewerId, creator)) {
            throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
        }
        return dto
    }

    suspend fun listVisible(userId: UUID, limit: Int): ReviewPage {
        val friendIds: Set<UUID> = dbQuery {
            Friendships.selectAll()
                .where {
                    (Friendships.state eq "ACCEPTED") and
                        ((Friendships.userAId eq userId) or (Friendships.userBId eq userId))
                }
                .map { row ->
                    val a = row[Friendships.userAId]
                    val b = row[Friendships.userBId]
                    if (a == userId) b else a
                }
                .toSet()
        }
        return dbQuery {
            val rows = Reviews.selectAll()
                .where { Reviews.deletedAt.isNull() }
                .orderBy(Reviews.modifiedAt to SortOrder.DESC)
                .map { it }
                .filter { row ->
                    val c = row[Reviews.creadorId]
                    c == userId || c in friendIds
                }
                .take(limit + 1)
            val list = rows.take(limit).map { it.toDto() }
            val next = if (rows.size > limit) list.last().id else null
            ReviewPage(items = list, nextCursor = next)
        }
    }

    suspend fun create(userId: UUID, body: ReviewCreate): ReviewDto {
        val id = runCatching { UUID.fromString(body.id) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "id inválido")
        }
        val fecha = runCatching { OffsetDateTime.parse(body.fechaVisita) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "fechaVisita inválida")
        }
        return dbQuery {
            val clash = Reviews.selectAll().where { Reviews.id eq id }.singleOrNull()
            if (clash != null) throw ApiException(HttpStatusCode.Conflict, "Ya existe una reseña con ese id")
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            Reviews.insert {
                it[Reviews.id] = id
                it[nombreSitio] = body.nombreSitio
                it[latitud] = body.latitud
                it[longitud] = body.longitud
                it[fotoUrl] = body.fotoUrl
                it[ocrTextoCrudo] = body.ocrTextoCrudo
                it[precioTotal] = body.precioTotal
                it[fechaVisita] = fecha
                it[creadorId] = userId
                it[modifiedAt] = now
                it[deletedAt] = null
            }
            Reviews.selectAll().where { Reviews.id eq id }.map { it.toDto() }.single()
        }
    }

    suspend fun put(userId: UUID, body: ReviewDto): ReviewDto {
        val id = runCatching { UUID.fromString(body.id) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "id inválido")
        }
        if (body.creadorId != userId.toString()) {
            throw ApiException(HttpStatusCode.BadRequest, "creadorId debe coincidir con el usuario autenticado")
        }
        val fecha = runCatching { OffsetDateTime.parse(body.fechaVisita) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "fechaVisita inválida")
        }
        return dbQuery {
            val row = Reviews.selectAll().where { Reviews.id eq id }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            if (row[Reviews.creadorId] != userId) {
                throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            }
            if (row[Reviews.deletedAt] != null) {
                throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            }
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            Reviews.update({ Reviews.id eq id }) {
                it[nombreSitio] = body.nombreSitio
                it[latitud] = body.latitud
                it[longitud] = body.longitud
                it[fotoUrl] = body.fotoUrl
                it[ocrTextoCrudo] = body.ocrTextoCrudo
                it[precioTotal] = body.precioTotal
                it[fechaVisita] = fecha
                it[modifiedAt] = now
            }
            Reviews.selectAll().where { Reviews.id eq id }.map { it.toDto() }.single()
        }
    }

    suspend fun delete(userId: UUID, reviewId: UUID) {
        dbQuery {
            val row = Reviews.selectAll().where { Reviews.id eq reviewId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            if (row[Reviews.creadorId] != userId) {
                throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            }
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            Reviews.update({ Reviews.id eq reviewId }) {
                it[deletedAt] = now
                it[modifiedAt] = now
            }
        }
    }
}
