package com.example.services

import com.example.exceptions.Exceptions
import com.example.models.Role
import com.example.models.Task
import com.example.models.TaskRevision
import com.example.models.TasksQueryRequest
import com.example.repository.TasksRepository
import kotlinx.datetime.toJavaLocalDateTime
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}


class TasksService(private val tasksRepository: TasksRepository, private val usersService: UsersService) {

    fun getTasks(loggedInUsername : String, query: TasksQueryRequest): List<Task> {
        return tasksRepository.getTasks(query).filter { isTaskAuthorizedForUser(it, loggedInUsername) }
    }

    fun getAuthorizedTaskById(loggedInUsername : String, id: Int): Task {
        return tasksRepository.getTaskById(id)?.also { task ->
            if (!isTaskAuthorizedForUser(task, loggedInUsername)){
                throw Exceptions.TaskNotAuthorizedForUser(task, loggedInUsername)
            }
        } ?: throw Exceptions.TaskNotFoundException(id)
    }

    fun getTaskHistory(id: Int) : List<TaskRevision>{
        return tasksRepository.getTaskHistory(id)
    }

    fun insertTask(loggedInUsername : String, task: Task): Int {
        if (!isTaskAuthorizedForUser(task, loggedInUsername)){
            throw Exceptions.TaskNotAuthorizedForUser(task, loggedInUsername)
        }
        // verify the due date is in the future before creating a task
        task.takeUnless { it.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now()) }
            ?.let { validTask ->
                val id = tasksRepository.insertTask(loggedInUsername, validTask)
                logger.info { "Task $validTask created successfully" }
                return id
            } ?: run {
            logger.error { "Task $task can't be created with due date ${task.dueDate} in the past" }
            throw Exceptions.TaskDueDatePastException(task)
        }
    }

    fun updateTask(loggedInUsername : String, id: Int, task: Task) {
        // verify the URL id matches the task id in the body, if so verify a task with such id exist and the new dueDate is in the future
        val currentTask = getAuthorizedTaskById(loggedInUsername, id)
        when {
            id != task.taskId -> {
                logger.error { "The task ID in the URL $id does not match the taskId in the request body ${task.taskId}" }
                throw Exceptions.MismatchedTaskIdException(id, task.taskId)
            }
            task.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now()) -> {
                logger.error { "Task $task can't be updated with due date ${task.dueDate} in the past" }
                throw Exceptions.TaskDueDatePastException(task)
            }
            else -> {
                tasksRepository.updateTask(loggedInUsername, task)
                logger.info { "Task with id ${task.taskId} updated successfully: $currentTask -> $task" }
            }
        }
    }

    fun deleteTask(loggedInUsername : String, id: Int) {
        val task = getAuthorizedTaskById(loggedInUsername, id)
        if (!isTaskAuthorizedForUser(task, loggedInUsername)){
            throw Exceptions.TaskNotAuthorizedForUser(task, loggedInUsername)
        }

        tasksRepository.deleteTask(loggedInUsername, id)
        logger.info { "task with id $id deleted successfully" }
    }

    // returns true if
    //   task has no owner
    //   task's owner is the logged-in user
    //   logged-in user is admin
    //   task's owner is managed by the current logged-in user who is a manger
    private fun isTaskAuthorizedForUser(task: Task, username: String) : Boolean {
        val user = username.let { usersService.getUserByUserName(username) ?: throw Exceptions.NoUserFoundForLoggedInUserException(username) }

        return when {
            task.owner == null -> true
            task.owner == user.userId -> true
            user.role == Role.ADMIN -> true
            user.role == Role.MANAGER -> {
                val managedUsers = usersService.getManagersToUsersMap().getOrDefault(user, emptyList())
                managedUsers.any { it.userId == task.owner }
            }
            else -> false
        }
    }
}