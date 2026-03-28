package com.placenote.server.security

import io.ktor.server.auth.Principal
import java.util.UUID

data class UserIdPrincipal(val userId: UUID) : Principal
