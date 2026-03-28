package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.RatingDto
import com.placenote.server.api.RatingUpsert
import com.placenote.server.db.Ratings
import com.placenote.server.db.Reviews
import com.placenote.server.db.dbQuery
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

object RatingService {
    private val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun ResultRow.toDto() = RatingDto(
        reviewId = this[Ratings.reviewId].toString(),
        userId = this[Ratings.userId].toString(),
        nota = this[Ratings.nota],
        comentarioPrivado = this[Ratings.comentarioPrivado],
        modifiedAt = isoFmt.format(this[Ratings.modifiedAt]),
    )

    suspend fun listForReview(reviewId: UUID, viewerId: UUID): List<RatingDto> {
        val creator = dbQuery {
            val r = Reviews.selectAll().where { Reviews.id eq reviewId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            r[Reviews.creadorId]
        }
        if (!ReviewService.canViewReview(viewerId, creator)) {
            throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
        }
        return dbQuery {
            Ratings.selectAll().where { Ratings.reviewId eq reviewId }.map { row -> row.toDto() }
        }
    }

    suspend fun upsert(reviewId: UUID, userId: UUID, body: RatingUpsert): RatingDto {
        val creator = dbQuery {
            val r = Reviews.selectAll().where { Reviews.id eq reviewId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            if (r[Reviews.deletedAt] != null) {
                throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
            }
            r[Reviews.creadorId]
        }
        if (!ReviewService.canViewReview(userId, creator)) {
            throw ApiException(HttpStatusCode.NotFound, "Reseña no encontrada")
        }
        if (body.nota < 0 || body.nota > 10) {
            throw ApiException(HttpStatusCode.BadRequest, "La nota debe estar entre 0 y 10")
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return dbQuery {
            val existing = Ratings.selectAll()
                .where { (Ratings.reviewId eq reviewId) and (Ratings.userId eq userId) }
                .singleOrNull()
            if (existing == null) {
                Ratings.insert {
                    it[Ratings.reviewId] = reviewId
                    it[Ratings.userId] = userId
                    it[Ratings.nota] = body.nota
                    it[Ratings.comentarioPrivado] = body.comentarioPrivado
                    it[Ratings.modifiedAt] = now
                }
            } else {
                Ratings.update({ (Ratings.reviewId eq reviewId) and (Ratings.userId eq userId) }) {
                    it[Ratings.nota] = body.nota
                    it[Ratings.comentarioPrivado] = body.comentarioPrivado
                    it[Ratings.modifiedAt] = now
                }
            }
            Ratings.selectAll()
                .where { (Ratings.reviewId eq reviewId) and (Ratings.userId eq userId) }
                .map { row -> row.toDto() }
                .single()
        }
    }
}
