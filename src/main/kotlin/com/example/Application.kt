package com.example

import com.example.exceptions.ErrorResponse
import com.example.exceptions.Exceptions
import com.example.repository.TasksRepository
import com.example.routes.taskRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(TasksRepository())
    }.start(wait = true)
}

fun Application.module(tasksRepository: TasksRepository) {
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
    }

    routing {
        taskRoutes(tasksRepository)
    }

}



