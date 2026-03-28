package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.FriendshipCreate
import com.placenote.server.api.FriendshipDto
import com.placenote.server.api.FriendshipPage
import com.placenote.server.api.FriendshipPatch
import com.placenote.server.db.Friendships
import com.placenote.server.db.Users
import com.placenote.server.db.dbQuery
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object FriendshipService {
    private fun ResultRow.toDto() = FriendshipDto(
        id = this[Friendships.id].toString(),
        userAId = this[Friendships.userAId].toString(),
        userBId = this[Friendships.userBId].toString(),
        state = this[Friendships.state],
    )

    suspend fun create(currentUserId: UUID, body: FriendshipCreate): FriendshipDto {
        val target = runCatching { UUID.fromString(body.targetUserId) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "targetUserId inválido")
        }
        if (target == currentUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "No puedes solicitar amistad contigo mismo")
        }
        if (UserService.findById(target) == null) {
            throw ApiException(HttpStatusCode.NotFound, "Usuario no encontrado")
        }
        val (low, high) = if (currentUserId < target) currentUserId to target else target to currentUserId
        return dbQuery {
            val existing = Friendships.selectAll()
                .where { Friendships.userAId eq low and (Friendships.userBId eq high) }
                .map { it.toDto() }
                .singleOrNull()
            if (existing != null) {
                throw ApiException(HttpStatusCode.Conflict, "Ya existe una relación con ese usuario")
            }
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            Friendships.insert {
                it[Friendships.id] = id
                it[userAId] = low
                it[userBId] = high
                it[initiatedBy] = currentUserId
                it[state] = "PENDING"
                it[createdAt] = now
                it[updatedAt] = now
            }
            Friendships.selectAll().where { Friendships.id eq id }.map { it.toDto() }.single()
        }
    }

    suspend fun listForUser(userId: UUID, @Suppress("UNUSED_PARAMETER") cursor: String?, limit: Int): FriendshipPage = dbQuery {
        val rows = Friendships.selectAll()
            .where {
                (Friendships.userAId eq userId) or (Friendships.userBId eq userId)
            }
            .orderBy(Friendships.updatedAt to SortOrder.DESC)
            .limit(limit + 1)
            .map { it.toDto() }
        val items = rows.take(limit)
        val next = if (rows.size > limit) items.last().id else null
        FriendshipPage(items = items, nextCursor = next)
    }

    suspend fun patch(friendshipId: UUID, userId: UUID, patch: FriendshipPatch): FriendshipDto = dbQuery {
        val row = Friendships.selectAll().where { Friendships.id eq friendshipId }.singleOrNull()
            ?: throw ApiException(HttpStatusCode.NotFound, "Amistad no encontrada")
        val low = row[Friendships.userAId]
        val high = row[Friendships.userBId]
        if (userId != low && userId != high) {
            throw ApiException(HttpStatusCode.NotFound, "Amistad no encontrada")
        }
        if (row[Friendships.state] != "PENDING") {
            throw ApiException(HttpStatusCode.BadRequest, "La solicitud ya fue procesada")
        }
        if (patch.state != "ACCEPTED") {
            throw ApiException(HttpStatusCode.BadRequest, "Estado no soportado")
        }
        if (row[Friendships.initiatedBy] == userId) {
            throw ApiException(HttpStatusCode.BadRequest, "No puedes aceptar tu propia solicitud")
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        Friendships.update({ Friendships.id eq friendshipId }) {
            it[Friendships.state] = "ACCEPTED"
            it[Friendships.updatedAt] = now
        }
        Friendships.selectAll().where { Friendships.id eq friendshipId }.map { it.toDto() }.single()
    }
}
