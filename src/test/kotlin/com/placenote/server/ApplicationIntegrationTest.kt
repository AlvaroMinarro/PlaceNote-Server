package com.placenote.server

import com.placenote.server.api.AuthResponse
import com.placenote.server.api.ReviewPage
import com.placenote.server.api.UserDto
import com.placenote.server.config.AppConfig
import com.placenote.server.config.DatabaseConfig
import com.placenote.server.config.JwtConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
private data class SuccessEnvelopeAuth(
    val status: String,
    val data: AuthResponse,
)

@Serializable
private data class SuccessEnvelopeUser(
    val status: String,
    val data: UserDto,
)

@Serializable
private data class SuccessEnvelopeReviewPage(
    val status: String,
    val data: ReviewPage,
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
        val regBody = reg.bodyAsText()
        assertTrue(regBody.contains("\"status\":\"success\""))
        assertTrue(regBody.contains("accessToken"))
        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"a@b.co","password":"password12"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val loginBody = login.bodyAsText()
        assertTrue(loginBody.contains("\"status\":\"success\""))
        assertTrue(loginBody.contains("accessToken"))
    }

    @Test
    fun usersMeAndReviewsWithJwt() = testApplication {
        application {
            module(testConfig())
        }
        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"jwt@b.co","password":"password12","name":"JwtUser"}""")
        }
        assertEquals(HttpStatusCode.Created, reg.status)
        val token = json.decodeFromString<SuccessEnvelopeAuth>(reg.bodyAsText()).data.accessToken

        val me = client.get("/api/v1/users/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, me.status)
        val meBody = me.bodyAsText()
        assertTrue(meBody.contains("\"status\":\"success\""))
        val user = json.decodeFromString<SuccessEnvelopeUser>(meBody).data
        assertEquals("JwtUser", user.name)

        val reviews = client.get("/api/v1/reviews") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, reviews.status)
        val page = json.decodeFromString<SuccessEnvelopeReviewPage>(reviews.bodyAsText()).data
        assertTrue(page.items.isEmpty())
    }
}
