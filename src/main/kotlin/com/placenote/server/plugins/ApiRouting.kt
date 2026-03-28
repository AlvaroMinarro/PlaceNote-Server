package com.placenote.server.plugins

import com.placenote.server.api.ApiInfoResponse
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
import com.placenote.server.security.UserIdPrincipal
import com.placenote.server.service.AuthService
import com.placenote.server.service.FriendshipService
import com.placenote.server.service.RatingService
import com.placenote.server.service.ReviewService
import com.placenote.server.service.SyncService
import com.placenote.server.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.configureApiRouting(authService: AuthService) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok", service = "placeNote-server"))
        }
        route("/api/v1") {
            get {
                call.respond(
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
                call.respond(HttpStatusCode.Created, res)
            }
            post("/auth/login") {
                val body = call.receive<LoginRequest>()
                call.respond(authService.login(body))
            }
            authenticate("jwt") {
                get("/me") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val u = UserService.findById(uid) ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        com.placenote.server.api.ProblemDetails(status = 401, detail = "No autorizado"),
                    )
                    call.respond(u)
                }
                patch("/me") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val patch = call.receive<UserPatch>()
                    call.respond(UserService.updateUser(uid, patch))
                }
                route("/friendships") {
                    get {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val cursor = call.request.queryParameters["cursor"]
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                        call.respond(FriendshipService.listForUser(uid, cursor, limit))
                    }
                    post {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val body = call.receive<FriendshipCreate>()
                        call.respond(HttpStatusCode.Created, FriendshipService.create(uid, body))
                    }
                    patch("/{friendshipId}") {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val id = UUID.fromString(call.parameters["friendshipId"]!!)
                        val body = call.receive<FriendshipPatch>()
                        call.respond(FriendshipService.patch(id, uid, body))
                    }
                }
                route("/reviews") {
                    get {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                        call.respond(ReviewService.listVisible(uid, limit))
                    }
                    post {
                        val uid = call.principal<UserIdPrincipal>()!!.userId
                        val body = call.receive<ReviewCreate>()
                        call.respond(HttpStatusCode.Created, ReviewService.create(uid, body))
                    }
                    route("/{reviewId}") {
                        get {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            call.respond(ReviewService.get(id, uid))
                        }
                        put {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            val body = call.receive<ReviewDto>()
                            if (body.id != id.toString()) {
                                return@put call.respond(
                                    HttpStatusCode.BadRequest,
                                    com.placenote.server.api.ProblemDetails(status = 400, detail = "id no coincide con la ruta"),
                                )
                            }
                            call.respond(ReviewService.put(uid, body))
                        }
                        delete {
                            val uid = call.principal<UserIdPrincipal>()!!.userId
                            val id = UUID.fromString(call.parameters["reviewId"]!!)
                            ReviewService.delete(uid, id)
                            call.respond(HttpStatusCode.NoContent)
                        }
                        route("/ratings") {
                            get {
                                val uid = call.principal<UserIdPrincipal>()!!.userId
                                val reviewId = UUID.fromString(call.parameters["reviewId"]!!)
                                call.respond(RatingService.listForReview(reviewId, uid))
                            }
                            put {
                                val uid = call.principal<UserIdPrincipal>()!!.userId
                                val reviewId = UUID.fromString(call.parameters["reviewId"]!!)
                                val body = call.receive<RatingUpsert>()
                                call.respond(RatingService.upsert(reviewId, uid, body))
                            }
                        }
                    }
                }
                post("/sync/push") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val body = call.receive<SyncPushRequest>()
                    call.respond(SyncService.push(uid, body))
                }
                get("/sync/pull") {
                    val uid = call.principal<UserIdPrincipal>()!!.userId
                    val since = call.request.queryParameters["since"]
                    call.respond(SyncService.pull(uid, since))
                }
            }
        }
    }
}
