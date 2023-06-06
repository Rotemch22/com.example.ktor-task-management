package com.example.routes

import com.example.RequestContext
import com.example.exceptions.Exceptions
import com.example.getLoggedInUsername
import com.example.models.*
import com.example.services.interfaces.TasksService
import com.example.services.interfaces.UsersService
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
const val TASK_ID_ROUTE = "$TASKS_ROUTE/{id}"
const val TASK_HISTORY_ROUTE = "$TASK_ID_ROUTE/history"

fun Route.taskRoutes(tasksService: TasksService, usersService: UsersService) {

    fun getRequestContext(username : String) :RequestContext {
        return usersService.getUserByUserName(username)?.let { RequestContext(it) } ?: throw Exceptions.NoUserFoundForLoggedInUserException(username)
    }

    get(TASKS_ROUTE) {
        val statusParam = call.request.queryParameters["status"]
        val severityParam = call.request.queryParameters["severity"]
        val orderParam = call.request.queryParameters["order"]
        val owner = call.request.queryParameters["owner"]
        val requestContext = getRequestContext(call.getLoggedInUsername())

        // parse the query params given into the corresponding enum classes and throw an exception if not possible
        val status = statusParam?.let { parseEnumValue<TaskStatus>(it, "status") }
        val severity = severityParam?.let { parseEnumValue<TaskSeverity>(it, "severity") }
        val order = orderParam?.let { parseEnumValue<TaskSortOrder>(it, "order") }

        call.respond(Json.encodeToString(tasksService.getTasks(requestContext, TasksQueryRequest(status, severity, owner?.toInt(), order))))
    }

    get(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val requestContext = getRequestContext(call.getLoggedInUsername())

        call.respond(tasksService.getAuthorizedTaskById(requestContext, id))
    }

    get(TASK_HISTORY_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")

        call.respond(Json.encodeToString(tasksService.getTaskHistory(id)))
    }

    post(TASKS_ROUTE) {
        val taskDetails = call.receive<TaskDetails>()
        val requestContext = getRequestContext(call.getLoggedInUsername())

        val id = tasksService.insertTask(requestContext, taskDetails)
        logger.info { "Task $taskDetails with id $id created successfully" }
        call.respond(status = HttpStatusCode.Created, TaskRecord(taskDetails, id))
    }

    put(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val task = call.receive<TaskDetails>()
        val requestContext = getRequestContext(call.getLoggedInUsername())

        tasksService.updateTask(requestContext, id, task)
        logger.info { "Task $task updated successfully" }
        call.respond(status = HttpStatusCode.OK, TaskRecord(task, id))
    }

    delete(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val requestContext = getRequestContext(call.getLoggedInUsername())

        tasksService.deleteTask(requestContext, id)
        logger.info { "Task with id$id deleted successfully" }
        call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
    }
}

inline fun <reified T : Enum<T>> parseEnumValue(value: String, paramName: String): T? {
    return runCatching { enumValueOf<T>(value.uppercase()) }
        .onFailure {
            throw Exceptions.InvalidTaskQueryValueException(value, paramName)
        }.getOrNull()
}
