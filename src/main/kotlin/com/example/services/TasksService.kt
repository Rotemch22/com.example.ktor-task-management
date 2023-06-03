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
        task
            .isAuthorizedForUser(loggedInUsername)
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(500)
            .also {
                val id = tasksRepository.insertTask(loggedInUsername, task)
                logger.info { "Task $task created successfully" }
                return id
            }
    }

    fun updateTask(loggedInUsername : String, id: Int, task: Task) {
        val currentTask = getAuthorizedTaskById(loggedInUsername, id)
        task
            .isIdMatchUrl(id)
            .isAuthorizedForUser(loggedInUsername) // Check if the user is authorized for the updated task
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(1000)
            .also { taskToUpdate ->
                // Check if the user is authorized for the previous owner of the task
                if (currentTask.owner != taskToUpdate.owner) {
                    currentTask.isAuthorizedForUser(loggedInUsername)
                }
                tasksRepository.updateTask(loggedInUsername, taskToUpdate)
                logger.info { "Task with id ${task.taskId} updated successfully: $currentTask -> $task" }
            }
    }

    fun deleteTask(loggedInUsername : String, id: Int) {
        val task = getAuthorizedTaskById(loggedInUsername, id)
        task.isAuthorizedForUser(loggedInUsername)

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

    private fun Task.isAuthorizedForUser(username: String): Task {
        if (!isTaskAuthorizedForUser(this, username)) {
            throw Exceptions.TaskNotAuthorizedForUser(this, username)
        }
        return this
    }

    private fun Task.isDueDateInFuture(): Task {
        if (this.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now())) {
            throw Exceptions.TaskDueDatePastException(this)
        }
        return this
    }

    private fun Task.isIdMatchUrl(id: Int): Task {
        if (id != this.taskId) {
            throw Exceptions.MismatchedTaskIdException(id, this.taskId)
        }
        return this
    }

    private fun Task.validateTitleLength(maxLength: Int): Task {
        require(title.isNotBlank()) { "Task title must not be empty" }
        require(title.length <= maxLength) { "Task title exceeds the maximum length of $maxLength characters" }
        return this
    }

    private fun Task.validateDescriptionLength(maxLength: Int): Task {
        if (description != null) {
            require(description.length <= maxLength) { "Task description exceeds the maximum length of $maxLength characters" }
        }
        return this
    }
}