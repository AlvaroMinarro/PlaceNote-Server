package com.placenote.server.service

import com.placenote.server.api.ApiException
import com.placenote.server.api.AuthResponse
import com.placenote.server.api.LoginRequest
import com.placenote.server.api.RegisterRequest
import com.placenote.server.config.JwtConfig
import com.placenote.server.security.JwtService
import io.ktor.http.HttpStatusCode
import org.mindrot.jbcrypt.BCrypt

class AuthService(
    private val jwtConfig: JwtConfig,
    private val jwtService: JwtService,
) {
    suspend fun register(req: RegisterRequest): AuthResponse {
        validateEmail(req.email)
        if (req.password.length < 8) {
            throw ApiException(HttpStatusCode.BadRequest, "La contraseña debe tener al menos 8 caracteres")
        }
        if (req.name.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "El nombre es obligatorio")
        }
        if (UserService.findByEmail(req.email) != null) {
            throw ApiException(HttpStatusCode.Conflict, "El email ya está registrado")
        }
        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt(12))
        val id = UserService.createUser(req.email, hash, req.name.trim())
        val token = jwtService.createAccessToken(id)
        return AuthResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = jwtConfig.accessTtlSeconds,
        )
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val row = UserService.findByEmail(req.email)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Credenciales inválidas")
        val ok = BCrypt.checkpw(req.password, row.passwordHash)
        if (!ok) throw ApiException(HttpStatusCode.Unauthorized, "Credenciales inválidas")
        val token = jwtService.createAccessToken(row.id)
        return AuthResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = jwtConfig.accessTtlSeconds,
        )
    }

    private fun validateEmail(email: String) {
        if (!email.contains('@') || email.length > 320) {
            throw ApiException(HttpStatusCode.BadRequest, "Email inválido")
        }
    }
}
