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
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

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
            if (task == null){
                val errorMsg = "task with id $id not found"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.NotFound, ErrorResponse(errorMsg))
                return@get
            }

            call.respond(task)
        }

        post ("/tasks") {
            val task = call.receive<Task>()

            if (task.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now())){
                val errorMsg = "Task $task can't be created with due date ${task.dueDate} in the past"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.BadRequest, errorMsg)
                return@post
            }

            val id = tasksRepository.insertTask(task)

            logger.info { "task $task created successfully" }
            call.respond(status = HttpStatusCode.Created, task.copy(taskId = id))
        }

        put ("/tasks/{id}"){
            val id = call.parameters.getOrFail<Int>("id")
            val task = call.receive<Task>()

            // verify that the id in the path matches the id in the json body
            if (id != task.taskId){
                val errorMsg = "The task ID in the URL $id does not match the taskId in the request body ${task.taskId}"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.UnprocessableEntity, errorMsg)
                return@put
            }

            val currentTask = tasksRepository.getTaskById(id)
            if (currentTask == null){
                val errorMsg = "Task with ID $id does not exist"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.NotFound, ErrorResponse(errorMsg))
                return@put
            }

            if (task.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now())){
                val errorMsg = "Task $task can't be updated with due date ${task.dueDate} in the past"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.BadRequest, errorMsg)
                return@put
            }

            tasksRepository.updateTask(task)
            logger.info { "task with id ${task.taskId} updated successfully: $currentTask -> $task"  }
            call.respond(status = HttpStatusCode.OK, task)
        }

        delete("/tasks/{id}"){
            val id = call.parameters.getOrFail<Int>("id")

            if (tasksRepository.getTaskById(id) == null){
                val errorMsg = "Task with ID $id does not exist"
                logger.error { errorMsg }
                call.respond(status = HttpStatusCode.NotFound, ErrorResponse(errorMsg))
                return@delete
            }

            tasksRepository.deleteTask(id)
            logger.info { "task with id $id deleted successfully" }
            call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
        }
    }

    install(ContentNegotiation) {
        json()
    }
}

@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)