package com.example.routes

import com.example.exceptions.Exceptions
import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.models.TasksQueryRequest.*
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


        // verify the query params are valid before invoking the tasks service
        val status = statusParam?.let {
            runCatching { TaskStatus.valueOf(it.uppercase()) }
                .onFailure {
                    throw Exceptions.InvalidTaskQueryValueException(statusParam, "status")
                }.getOrNull()
        }

        val severity = severityParam?.let {
            runCatching { TaskSeverity.valueOf(it.uppercase()) }
                .onFailure {
                    throw Exceptions.InvalidTaskQueryValueException(severityParam, "severity")
                }.getOrNull()
        }

        val order = orderParam?.let {
            kotlin.runCatching { TaskSortOrder.valueOf(it.uppercase()) }
                .onFailure {
                    throw Exceptions.InvalidTaskQueryValueException(orderParam, "order")
                }.getOrNull()
        }


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
