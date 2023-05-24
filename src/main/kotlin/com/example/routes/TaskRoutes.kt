package com.example.routes

import com.example.exceptions.Exceptions
import com.example.models.Task
import com.example.repository.TasksRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
const val TASKS_ROUTE = "/tasks"
const val TASK_ID_ROUTE = "/tasks/{id}"

fun Route.taskRoutes(tasksRepository: TasksRepository) {
    get(TASKS_ROUTE) {
        call.respond(Json.encodeToString(tasksRepository.getAllTasks()))
    }

    get(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")

        tasksRepository.getTaskById(id)?.let { task ->
            call.respond(task)
        } ?: kotlin.run {
            logger.error { "task with id $id not found" }
            throw Exceptions.TaskNotFoundException(id)
        }
    }

    post(TASKS_ROUTE) {
        val task = call.receive<Task>()
        task.takeUnless { it.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now()) }
            ?.let { validTask ->
                val id = tasksRepository.insertTask(validTask)
                logger.info { "Task $validTask created successfully" }
                call.respond(status = HttpStatusCode.Created, validTask.copy(taskId = id))
            } ?: run {
            logger.error { "Task $task can't be created with due date ${task.dueDate} in the past" }
            throw Exceptions.TaskDueDatePastException(task)
        }
    }

    put(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")
        val task = call.receive<Task>()
        val currentTask = tasksRepository.getTaskById(id)
        when {
            id != task.taskId -> {
                logger.error { "The task ID in the URL $id does not match the taskId in the request body ${task.taskId}" }
                throw Exceptions.MismatchedTaskIdException(id, task.taskId)
            }
            currentTask == null -> {
                logger.error { "Task with ID $id does not exist and can't be updated" }
                throw Exceptions.TaskNotFoundException(id)
            }
            task.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now()) -> {
                logger.error { "Task $task can't be updated with due date ${task.dueDate} in the past" }
                throw Exceptions.TaskDueDatePastException(task)
            }
            else -> {
                tasksRepository.updateTask(task)
                logger.info { "Task with id ${task.taskId} updated successfully: $currentTask -> $task" }
                call.respond(status = HttpStatusCode.OK, task)
            }
        }
        tasksRepository.updateTask(task)
        logger.info { "task with id ${task.taskId} updated successfully: $currentTask -> $task" }
        call.respond(status = HttpStatusCode.OK, task)
    }

    delete(TASK_ID_ROUTE) {
        val id = call.parameters.getOrFail<Int>("id")

        if (tasksRepository.getTaskById(id) == null) {
            logger.error { "Task with ID $id does not exist and can't be deleted" }
            throw Exceptions.TaskNotFoundException(id)
        }

        tasksRepository.deleteTask(id)
        logger.info { "task with id $id deleted successfully" }
        call.respondText("Task with id $id deleted successfully", status = HttpStatusCode.OK)
    }
}
