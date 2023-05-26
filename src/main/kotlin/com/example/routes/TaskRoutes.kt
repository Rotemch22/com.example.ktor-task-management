package com.example.routes

import com.example.exceptions.Exceptions
import com.example.models.*
import com.example.services.TasksService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
const val TASKS_ROUTE = "/tasks"
const val TASK_ID_ROUTE = "/tasks/{id}"

fun Route.taskRoutes(tasksService: TasksService) {
    get(TASKS_ROUTE) {
        val statusParam = call.request.queryParameters["status"]
        val severityParam = call.request.queryParameters["severity"]
        val orderParam = call.request.queryParameters["order"]
        val owner = call.request.queryParameters["owner"]


        // parse the query params given into the corresponding enum classes and use throw an exception if not possible
        val status = statusParam?.let { parseEnumValue<TaskStatus>(it, "status") }
        val severity = severityParam?.let { parseEnumValue<TaskSeverity>(it, "severity") }
        val order = orderParam?.let { parseEnumValue<TaskSortOrder>(it, "order") }

        call.respond(Json.encodeToString(tasksService.getTasks(TasksQueryRequest(status, severity, owner, order))))
    }



    get(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")

        tasksService.getTaskById(id)?.let { task ->
            call.respond(task)
        } ?: kotlin.run {
            logger.error { "task with id $id not found" }
            throw Exceptions.TaskNotFoundException(id)
        }
    }

    post(TASKS_ROUTE) {
        val task = call.receive<Task>()
        val id = tasksService.insertTask(task)
        call.respond(status = HttpStatusCode.Created, task.copy(taskId = id))
    }

    put(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val task = call.receive<Task>()

        tasksService.updateTask(id, task)
        call.respond(status = HttpStatusCode.OK, task)
    }

    delete(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        tasksService.deleteTask(id)

        call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
    }
}

inline fun <reified T : Enum<T>> parseEnumValue(value: String, paramName: String): T? {
    return runCatching { enumValueOf<T>(value.uppercase()) }
        .onFailure {
            throw Exceptions.InvalidTaskQueryValueException(value, paramName)
        }.getOrNull()
}
