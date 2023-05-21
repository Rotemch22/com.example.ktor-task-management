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
            task ?: call.respondText("task with id $id not found", status = HttpStatusCode.NotFound)
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

            if (id != task.taskId){
                TODO("handle the case where the id in the path does not match the id in the task body")
            }

            tasksRepository.updateTask(task)
            call.respond(status = HttpStatusCode.OK, task)
        }

        delete("/tasks/{id}"){
            val idStr = call.parameters["id"]
            val taskId = idStr?.toIntOrNull() ?: throw java.lang.IllegalArgumentException("id argument must be numerical, given $idStr")

            tasksRepository.deleteTask(taskId)
            call.respondText("Task with id $taskId deleted successfully", status = HttpStatusCode.OK)
        }
    }

    install(ContentNegotiation) {
        json()
    }
}
