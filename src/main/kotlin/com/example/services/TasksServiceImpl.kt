package com.example.services

import TasksService
import UsersService
import com.example.RequestContext
import com.example.exceptions.Exceptions
import com.example.models.*
import com.example.repository.TasksRepository
import kotlinx.datetime.toJavaLocalDateTime
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}


class TasksServiceImpl(private val tasksRepository: TasksRepository, private val usersService: UsersService) : TasksService{

    override fun getTasks(requestContext: RequestContext, query: TasksQueryRequest): List<Task> {
        return tasksRepository.getTasks(query).filter { isTaskAuthorizedForUser(it, requestContext.user) }
    }

    override fun getAuthorizedTaskById(requestContext: RequestContext, id: Int): Task {
        return tasksRepository.getTaskById(id)?.also { task ->
            if (!isTaskAuthorizedForUser(task, requestContext.user)){
                throw Exceptions.TaskNotAuthorizedForUser(task, requestContext.user.username)
            }
        } ?: throw Exceptions.TaskNotFoundException(id)
    }

    override fun getTaskHistory(id: Int) : List<TaskRevision>{
        return tasksRepository.getTaskHistory(id)
    }

    override fun insertTask(requestContext: RequestContext, task: Task): Int {
        task
            .isAuthorizedForUser(requestContext.user)
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(1000)
            .also {
                val id = tasksRepository.insertTask(requestContext, task)
                logger.info { "Task $task created successfully" }
                return id
            }
    }

    override fun updateTask(requestContext: RequestContext, id: Int, task: Task) {
        val currentTask = getAuthorizedTaskById(requestContext, id)
        task
            .isIdMatchUrl(id)
            .isAuthorizedForUser(requestContext.user) // Check if the user is authorized for the updated task
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(1000)
            .also { taskToUpdate ->
                // Check if the user is authorized for the previous owner of the task
                if (currentTask.owner != taskToUpdate.owner) {
                    currentTask.isAuthorizedForUser(requestContext.user)
                }
                tasksRepository.updateTask(requestContext, taskToUpdate)
                logger.info { "Task with id ${task.taskId} updated successfully: $currentTask -> $task" }
            }
    }

    override fun deleteTask(requestContext: RequestContext, id: Int) {
        val task = getAuthorizedTaskById(requestContext, id)
        task.isAuthorizedForUser(requestContext.user)

        tasksRepository.deleteTask(requestContext, id)
        logger.info { "task with id $id deleted successfully" }
    }

    // returns true if
    //   task has no owner
    //   task's owner is the logged-in user
    //   logged-in user is admin
    //   task's owner is managed by the current logged-in user who is a manger
    private fun isTaskAuthorizedForUser(task: Task, user: User) : Boolean {
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

    private fun Task.isAuthorizedForUser(user: User): Task {
        if (!isTaskAuthorizedForUser(this, user)) {
            throw Exceptions.TaskNotAuthorizedForUser(this, user.username)
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