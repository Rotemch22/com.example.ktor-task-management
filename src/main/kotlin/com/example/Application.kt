package com.example

import com.example.exceptions.ErrorResponse
import com.example.exceptions.Exceptions
import com.example.repository.TasksRepository
import com.example.repository.UsersRepository
import com.example.routes.taskRoutes
import com.example.routes.userRoutes
import com.example.services.TasksService
import com.example.services.UsersService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource


fun main() {
    val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks"
        serverName = "localhost"
        portNumber = 5432
    }
    val db = Database.connect(dataSource)

    val usersRepository = UsersRepository(db)
    val tasksRepository = TasksRepository(db)

    // Create the database tables
    transaction (db) {
        SchemaUtils.create(UsersRepository.UsersTable, TasksRepository.TasksTable)
    }

    val usersService = UsersService(usersRepository)
    usersService.initializeAdminUser()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(TasksService(tasksRepository), usersService)
    }.start(wait = true)
}

fun Application.module(tasksService: TasksService, usersService: UsersService) {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<Exceptions.TaskNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: ""))
        }
        exception<Exceptions.MismatchedTaskIdException> { call, cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(cause.message ?: ""))
        }
        exception<Exceptions.TaskDueDatePastException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: ""))
        }
        exception<Exceptions.InvalidTaskQueryValueException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: ""))
        }
        exception<Exceptions.EndUserWithoutManager> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: ""))
        }
        exception<Exceptions.UserWithInvalidManagerId> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: ""))
        }
    }

    routing {
        taskRoutes(tasksService)
        userRoutes(usersService)
    }

}



