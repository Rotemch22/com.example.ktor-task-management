package com.example.services

import com.example.exceptions.Exceptions
import com.example.models.Task
import com.example.models.TasksQueryRequest
import com.example.repository.TasksRepository
import kotlinx.datetime.toJavaLocalDateTime
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}


class TasksService(private val tasksRepository: TasksRepository) {
    fun getTasks(query: TasksQueryRequest.TasksQueryRequest): List<Task> {
        return tasksRepository.getTasks(query)
    }

    fun getTaskById(id: Int): Task? {
        return tasksRepository.getTaskById(id)
    }

    fun insertTask(task: Task): Int {
        // verify the due date is in the future before creating a task
        task.takeUnless { it.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now()) }
            ?.let { validTask ->
                val id = tasksRepository.insertTask(validTask)
                logger.info { "Task $validTask created successfully" }
                return id
            } ?: run {
            logger.error { "Task $task can't be created with due date ${task.dueDate} in the past" }
            throw Exceptions.TaskDueDatePastException(task)
        }
    }

    fun updateTask(id: Int, task: Task) {
        // verify the URL id matches the task id in the body, if so verify a task with such id exist and the new dueDate is in the future
        val currentTask = getTaskById(id)
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
            }
        }
    }

    fun deleteTask(id: Int) {
        if (getTaskById(id) == null) {
            logger.error { "Task with ID $id does not exist and can't be deleted" }
            throw Exceptions.TaskNotFoundException(id)
        }

        tasksRepository.deleteTask(id)
        logger.info { "task with id $id deleted successfully" }
    }
}