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
import io.ktor.server.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

fun main() {
    val tasksRepository = TasksRepository()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(tasksRepository)
    }.start(wait = true)
}

fun Application.module(tasksRepository: TasksRepository) {
    routing {
        get("/tasks") {
            call.respond(Json.encodeToString(tasksRepository.getAllTasks()))
        }

        get("/tasks/{id}"){
            val id = call.parameters.getOrFail<Int>("id")

            val task = tasksRepository.getTaskById(id)
            task ?: call.respond(status = HttpStatusCode.NotFound, ErrorResponse("task with id $id not found"))
            call.respond(task!!)
        }

        post ("/tasks") {
            val task = call.receive<Task>()
            val id = tasksRepository.insertTask(task)

            call.respond(status = HttpStatusCode.Created, task.copy(taskId = id))
        }

        put ("/tasks/{id}"){
            val id = call.parameters.getOrFail<Int>("id")
            val task = call.receive<Task>()

            // verify that the id in the path matches the id in the json body
            if (id != task.taskId){
                call.respond(status = HttpStatusCode.UnprocessableEntity, "The task ID in the URL $id does not match the taskId in the request body ${task.taskId}")
                return@put
            }

            if (tasksRepository.getTaskById(id) == null){
                call.respond(status = HttpStatusCode.NotFound, ErrorResponse("Task with ID $id does not exist"))
                return@put
            }

            tasksRepository.updateTask(task)
            call.respond(status = HttpStatusCode.OK, task)
        }

        delete("/tasks/{id}"){
            val id = call.parameters.getOrFail<Int>("id")

            if (tasksRepository.getTaskById(id) == null){
                call.respond(status = HttpStatusCode.NotFound, ErrorResponse("Task with ID $id does not exist"))
                return@delete
            }

            tasksRepository.deleteTask(id)
            call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
        }
    }

    install(ContentNegotiation) {
        json()
    }
}

@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)