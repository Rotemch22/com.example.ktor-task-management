package com.example.routes

import UsersService
import com.example.exceptions.Exceptions
import com.example.models.Role
import com.example.models.User
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
const val USERS_ROUTE = "/users"


fun Route.userRoutes(usersService: UsersService) {
    get(USERS_ROUTE) {
        call.respond(
            Json.encodeToString(usersService.getAllUsers().map { it.toUserResponse() }))
    }

    post (USERS_ROUTE) {
        val userInput = call.receive<UserInput>()
        val manager = usersService.getUserById(userInput.managerId)
        // verify that if given a manager id then it exists in the database
        if (userInput.managerId != null && manager == null){
            logger.error { "user $userInput with invalid managerId ${userInput.managerId}, no such manager found" }
            throw Exceptions.UserWithInvalidManagerId(userInput)
        }

        val userData = userInput.toUserData(manager)
        val id = usersService.insertUser(userData)
        logger.info { "user $userData with user id $id created successfully" }
        call.respond(status = HttpStatusCode.Created, userData.copy(userId = id).toUserResponse())
    }
}

fun User.toUserResponse() : UserResponse{
    return UserResponse(username, email, role, manager?.toUserResponse(), userId)
}

private fun UserInput.toUserData(manager: User?): User {
    return User(username, password, email, role, manager)
}

@Serializable
data class UserInput
    (val username: String,
     val password: String,
     val email: String,
     val role: Role,
     val managerId: Int? = null)


@Serializable
data class UserResponse(
    val username: String,
    val email: String,
    val role: Role,
    val manager: UserResponse?,
    val userId : Int?
)