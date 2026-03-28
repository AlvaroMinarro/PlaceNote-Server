package com.placenote.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 320)
    val passwordHash = text("password_hash")
    val name = varchar("name", 255)
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Friendships : Table("friendships") {
    val id = uuid("id")
    val userAId = uuid("user_a_id")
    val userBId = uuid("user_b_id")
    val initiatedBy = uuid("initiated_by")
    val state = varchar("state", 20)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Reviews : Table("reviews") {
    val id = uuid("id")
    val nombreSitio = text("nombre_sitio")
    val latitud = double("latitud")
    val longitud = double("longitud")
    val fotoUrl = text("foto_url").nullable()
    val ocrTextoCrudo = text("ocr_texto_crudo").nullable()
    val precioTotal = double("precio_total").nullable()
    val fechaVisita = timestampWithTimeZone("fecha_visita")
    val creadorId = uuid("creador_id")
    val modifiedAt = timestampWithTimeZone("modified_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Ratings : Table("ratings") {
    val reviewId = uuid("review_id")
    val userId = uuid("user_id")
    val nota = double("nota")
    val comentarioPrivado = text("comentario_privado").nullable()
    val modifiedAt = timestampWithTimeZone("modified_at")
    override val primaryKey = PrimaryKey(reviewId, userId)
}
