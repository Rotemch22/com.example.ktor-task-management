package com.example.routes

import com.example.models.User
import com.example.services.UsersService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

const val USERS_ROUTE = "/users"


fun Route.userRoutes(usersService: UsersService) {
    post (USERS_ROUTE) {
        val user = call.receive<User>()
        val id = usersService.insertUser(user)
        call.respond(status = HttpStatusCode.Created, user.copy(userId = id))
    }
}
