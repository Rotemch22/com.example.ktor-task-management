package com.example.routes

import com.example.exceptions.Exceptions
import com.example.getLoggedInUsername
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

const val TASKS_ROUTE = "/tasks"
const val TASK_ID_ROUTE = "$TASKS_ROUTE/{id}"
const val TASK_HISTORY_ROUTE = "$TASK_ID_ROUTE/history"

fun Route.taskRoutes(tasksService: TasksService) {
    get(TASKS_ROUTE) {
        val statusParam = call.request.queryParameters["status"]
        val severityParam = call.request.queryParameters["severity"]
        val orderParam = call.request.queryParameters["order"]
        val owner = call.request.queryParameters["owner"]
        val loggedInUsername = call.getLoggedInUsername()

        // parse the query params given into the corresponding enum classes and use throw an exception if not possible
        val status = statusParam?.let { parseEnumValue<TaskStatus>(it, "status") }
        val severity = severityParam?.let { parseEnumValue<TaskSeverity>(it, "severity") }
        val order = orderParam?.let { parseEnumValue<TaskSortOrder>(it, "order") }

        call.respond(Json.encodeToString(tasksService.getTasks(loggedInUsername, TasksQueryRequest(status, severity, owner?.toInt(), order))))
    }

    get(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val loggedInUsername = call.getLoggedInUsername()

        call.respond(tasksService.getAuthorizedTaskById(loggedInUsername, id))
    }

    get(TASK_HISTORY_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")

        call.respond(Json.encodeToString(tasksService.getTaskHistory(id)))
    }

    post(TASKS_ROUTE) {
        val task = call.receive<Task>()
        val loggedInUsername = call.getLoggedInUsername()

        val id = tasksService.insertTask(loggedInUsername, task)
        call.respond(status = HttpStatusCode.Created, task.copy(taskId = id))
    }

    put(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val task = call.receive<Task>()
        val loggedInUsername = call.getLoggedInUsername()

        tasksService.updateTask(loggedInUsername, id, task)
        call.respond(status = HttpStatusCode.OK, task)
    }

    delete(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val loggedInUsername = call.getLoggedInUsername()

        tasksService.deleteTask(loggedInUsername, id)
        call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
    }
}

inline fun <reified T : Enum<T>> parseEnumValue(value: String, paramName: String): T? {
    return runCatching { enumValueOf<T>(value.uppercase()) }
        .onFailure {
            throw Exceptions.InvalidTaskQueryValueException(value, paramName)
        }.getOrNull()
}
