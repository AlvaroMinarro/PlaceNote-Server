package com.placenote.server

import com.placenote.server.config.AppConfig
import com.placenote.server.config.DatabaseConfig
import com.placenote.server.config.JwtConfig
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {
    private lateinit var postgres: PostgreSQLContainer<Nothing>

    @BeforeAll
    fun startDb() {
        try {
            postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
            postgres.start()
        } catch (e: Exception) {
            Assumptions.abort("Docker no disponible para Testcontainers: ${e.message}")
        }
    }

    @AfterAll
    fun stopDb() {
        if (::postgres.isInitialized) postgres.stop()
    }

    private fun testConfig(): AppConfig =
        AppConfig(
            database = DatabaseConfig(
                jdbcUrl = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
            ),
            jwt = JwtConfig(
                secret = "test-secret-test-secret-test-secret-32",
                issuer = "placenote-test",
                audience = "test",
                realm = "test",
                accessTtlSeconds = 3600,
            ),
        )

    @Test
    fun healthOk() = testApplication {
        application {
            module(testConfig())
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ok"))
    }

    @Test
    fun registerAndLogin() = testApplication {
        application {
            module(testConfig())
        }
        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"a@b.co","password":"password12","name":"Test"}""")
        }
        assertEquals(HttpStatusCode.Created, reg.status)
        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"a@b.co","password":"password12"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        assertTrue(login.bodyAsText().contains("accessToken"))
    }
}
