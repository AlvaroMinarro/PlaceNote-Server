package com.placenote.server.config

import java.util.UUID

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTtlSeconds: Long,
)

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val database = System.getenv("DATABASE_URL")?.let { parseDatabaseUrl(it) }
                ?: DatabaseConfig(
                    jdbcUrl = "jdbc:postgresql://${env("POSTGRES_HOST", "localhost")}:${env("POSTGRES_PORT", "5432")}/${env("POSTGRES_DB", "placenote")}",
                    username = env("POSTGRES_USER", "placenote"),
                    password = env("POSTGRES_PASSWORD", "placenote_dev_change_me"),
                )

            val jwtSecret = System.getenv("JWT_SECRET")
                ?: "dev-only-change-me-${UUID.randomUUID()}"
            val jwt = JwtConfig(
                secret = jwtSecret,
                issuer = env("JWT_ISSUER", "placenote"),
                audience = env("JWT_AUDIENCE", "placenote-clients"),
                realm = env("JWT_REALM", "PlaceNote API"),
                accessTtlSeconds = System.getenv("JWT_ACCESS_TTL_SECONDS")?.toLongOrNull() ?: 86400L,
            )

            return AppConfig(database = database, jwt = jwt)
        }

        /** Formato `postgresql://usuario:clave@host:puerto/base` */
        private fun parseDatabaseUrl(url: String): DatabaseConfig {
            if (url.startsWith("jdbc:")) {
                return DatabaseConfig(
                    jdbcUrl = url,
                    username = env("POSTGRES_USER", "placenote"),
                    password = env("POSTGRES_PASSWORD", ""),
                )
            }
            val raw = url.removePrefix("postgresql://").removePrefix("postgres://")
            val at = raw.indexOf('@')
            require(at > 0) { "DATABASE_URL debe incluir credenciales" }
            val userInfo = raw.substring(0, at)
            val hostAndDb = raw.substring(at + 1)
            val user = userInfo.substringBefore(':')
            val pass = userInfo.substringAfter(':', missingDelimiterValue = "")
            return DatabaseConfig(
                jdbcUrl = "jdbc:postgresql://$hostAndDb",
                username = user,
                password = pass,
            )
        }

        private fun env(key: String, default: String) = System.getenv(key) ?: default
    }
}
