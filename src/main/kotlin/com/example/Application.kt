package com.example

import com.example.models.Task
import com.example.repository.TasksRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val tasksRepository = TasksRepository()

    routing {
        get("/tasks") {
            call.respond(Json.encodeToString(tasksRepository.getAllTasks()))
        }

        get("/tasks/{id}"){
            val idStr = call.parameters["id"]
            val taskId = idStr?.toIntOrNull() ?: throw java.lang.IllegalArgumentException("id argument must be numerical, given $idStr")

            val task = tasksRepository.getTaskById(taskId)
            task ?: call.respondText("task with id $taskId not found", status = HttpStatusCode.NotFound)
            call.respond(task!!)
        }

        post ("/tasks") {
            val task = call.receive<Task>()
            val id = tasksRepository.insertTask(task)

            call.respond(status = HttpStatusCode.Created, task.copy(taskId = id))
        }


    }

    install(ContentNegotiation) {
        json()
    }
}
