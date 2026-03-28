package com.placenote.server.plugins

import com.placenote.server.api.ApiInfoResponse
import com.placenote.server.api.DeleteSuccess
import com.placenote.server.api.FriendRequestBody
import com.placenote.server.api.FriendshipCreate
import com.placenote.server.api.FriendshipPatch
import com.placenote.server.api.HealthResponse
import com.placenote.server.api.RatingUpsert
import com.placenote.server.api.ReviewCreate
import com.placenote.server.api.ReviewDto
import com.placenote.server.api.SyncPushRequest
import com.placenote.server.api.LoginRequest
import com.placenote.server.api.RegisterRequest
import com.placenote.server.api.UserPatch
import com.placenote.server.api.apiJson
import com.placenote.server.api.respondApiError
import com.placenote.server.api.respondApiSuccess
import com.placenote.server.security.UserIdPrincipal
import kotlinx.serialization.encodeToString
import com.placenote.server.service.AuthService
import com.placenote.server.service.FriendshipService
import com.placenote.server.service.RatingService
import com.placenote.server.service.ReviewService
import com.placenote.server.service.SyncService
import com.placenote.server.service.UserService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.configureApiRouting(authService: AuthService) {
    routing {
        get("/health") {
            val body = apiJson.encodeToString(HealthResponse(status = "ok", service = "placeNote-server"))
            call.respondText(body, ContentType.Application.Json)
        }
        route("/api/v1") {
            get {
                call.respondApiSuccess(
                    ApiInfoResponse(
                        name = "PlaceNote API",
                        version = "0.1.0-SNAPSHOT",
                        apiVersion = "v1",
                        documentation = "See docs/api/openapi.yaml in the PlaceNote-Server repository",
                    ),
                )
            }
            post("/auth/register") {
                val body = call.receive<RegisterRequest>()
                val res = authService.register(body)
                call.respondApiSuccess(res, HttpStatusCode.Created)
            }
            post("/auth/login") {
                val body = call.receive<LoginRequest>()
                call.respondApiSuccess(authService.login(body))
            }
            authenticate("jwt") {
                // --- Usuario (preferido) ---
                route("/users") {
                    get("/me") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val u = UserService.findById(uid)
                        if (u == null) {
                            call.respondApiError(HttpStatusCode.Unauthorized, "No autorizado", 401)
                        } else {
                            call.respondApiSuccess(u)
                        }
                    }
                    patch("/me") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val patch = call.receive<UserPatch>()
                        call.respondApiSuccess(UserService.updateUser(uid, patch))
                    }
                }
                // --- Deprecado: /me (mantener compatibilidad) ---
                get("/me") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val u = UserService.findById(uid)
                    if (u == null) {
                        call.respondApiError(HttpStatusCode.Unauthorized, "No autorizado", 401)
                    } else {
                        call.respondApiSuccess(u)
                    }
                }
                patch("/me") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val patch = call.receive<UserPatch>()
                    call.respondApiSuccess(UserService.updateUser(uid, patch))
                }

                // --- Amigos (preferido) ---
                route("/friends") {
                    get {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val cursor = call.request.queryParameters["cursor"]
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                        call.respondApiSuccess(FriendshipService.listForUser(uid, cursor, limit))
                    }
                    post("/request") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val body = call.receive<FriendRequestBody>()
                        call.respondApiSuccess(
                            FriendshipService.create(uid, FriendshipCreate(targetUserId = body.userId)),
                            HttpStatusCode.Created,
                        )
                    }
                    put("/accept/{friendshipId}") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val id = UUID.fromString(call.parameters["friendshipId"]!!)
                        call.respondApiSuccess(
                            FriendshipService.patch(id, uid, FriendshipPatch(state = "ACCEPTED")),
                        )
                    }
                }
                // --- Deprecado: friendships ---
                route("/friendships") {
                    get {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val cursor = call.request.queryParameters["cursor"]
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                        call.respondApiSuccess(FriendshipService.listForUser(uid, cursor, limit))
                    }
                    post {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val body = call.receive<FriendshipCreate>()
                        call.respondApiSuccess(FriendshipService.create(uid, body), HttpStatusCode.Created)
                    }
                    patch("/{friendshipId}") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val id = UUID.fromString(call.parameters["friendshipId"]!!)
                        val body = call.receive<FriendshipPatch>()
                        call.respondApiSuccess(FriendshipService.patch(id, uid, body))
                    }
                }

                route("/reviews") {
                    get {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                        call.respondApiSuccess(ReviewService.listVisible(uid, limit))
                    }
                    post {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val body = call.receive<ReviewCreate>()
                        call.respondApiSuccess(ReviewService.create(uid, body), HttpStatusCode.Created)
                    }
                    route("/{reviewId}") {
                        get {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            call.respondApiSuccess(ReviewService.get(id, uid))
                        }
                        put {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            val body = call.receive<ReviewDto>()
                            if (body.id != id.toString()) {
                                call.respondApiError(HttpStatusCode.BadRequest, "id no coincide con la ruta", 400)
                            } else {
                                call.respondApiSuccess(ReviewService.put(uid, body))
                            }
                        }
                        delete {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            ReviewService.delete(uid, id)
                            call.respondApiSuccess(DeleteSuccess())
                        }
                        route("/ratings") {
                            get {
                                val uid = call.principal<UserIdPrincipal>()!!.userId
                                val reviewId = UUID.fromString(call.parameters["reviewId"]!!)
                                call.respondApiSuccess(RatingService.listForReview(reviewId, uid))
                            }
                            post {
                                val uid = call.principal<UserIdPrincipal>()!!.userId
                                val reviewId = UUID.fromString(call.parameters["reviewId"]!!)
                                val body = call.receive<RatingUpsert>()
                                call.respondApiSuccess(RatingService.upsert(reviewId, uid, body))
                            }
                            put {
                                val uid = call.principal<UserIdPrincipal>()!!.userId
                                val reviewId = UUID.fromString(call.parameters["reviewId"]!!)
                                val body = call.receive<RatingUpsert>()
                                call.respondApiSuccess(RatingService.upsert(reviewId, uid, body))
                            }
                        }
                    }
                }
                post("/sync/push") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val body = call.receive<SyncPushRequest>()
                    call.respondApiSuccess(SyncService.push(uid, body))
                }
                get("/sync/pull") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val since = call.request.queryParameters["last_sync"]
                        ?: call.request.queryParameters["since"]
                    call.respondApiSuccess(SyncService.pull(uid, since))
                }
            }
        }
    }
}
