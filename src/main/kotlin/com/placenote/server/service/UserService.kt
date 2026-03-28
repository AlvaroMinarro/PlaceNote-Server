package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.UserDto
import com.placenote.server.api.UserPatch
import com.placenote.server.db.Users
import com.placenote.server.db.dbQuery
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class UserWithPassword(
    val id: UUID,
    val passwordHash: String,
    val name: String,
)

object UserService {
    fun ResultRow.toDto() = UserDto(
        id = this[Users.id].toString(),
        name = this[Users.name],
        avatarUrl = this[Users.avatarUrl],
    )

    suspend fun findById(id: UUID): UserDto? = dbQuery {
        Users.selectAll().where { Users.id eq id }.map { it.toDto() }.singleOrNull()
    }

    suspend fun findByEmail(email: String): UserWithPassword? = dbQuery {
        Users.selectAll().where { Users.email eq email.lowercase() }
            .map {
                UserWithPassword(
                    id = it[Users.id],
                    passwordHash = it[Users.passwordHash],
                    name = it[Users.name],
                )
            }.singleOrNull()
    }

    suspend fun createUser(email: String, passwordHash: String, name: String): UUID = dbQuery {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        Users.insert {
            it[Users.id] = id
            it[Users.email] = email.lowercase()
            it[Users.passwordHash] = passwordHash
            it[Users.name] = name
            it[Users.avatarUrl] = null
            it[Users.createdAt] = now
            it[Users.updatedAt] = now
        }
        id
    }

    suspend fun updateUser(id: UUID, patch: UserPatch): UserDto = dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updated = Users.update({ Users.id eq id }) { st ->
            patch.name?.let { st[Users.name] = it }
            patch.avatarUrl?.let { st[Users.avatarUrl] = it }
            st[Users.updatedAt] = now
        }
        if (updated == 0) throw ApiException(HttpStatusCode.NotFound, "Usuario no encontrado")
        Users.selectAll().where { Users.id eq id }.map { it.toDto() }.single()
    }
}
