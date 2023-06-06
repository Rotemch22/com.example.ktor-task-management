package com.example.services

import com.example.RequestContext
import com.example.exceptions.Exceptions
import com.example.models.*
import com.example.repository.interfaces.TasksRepository
import com.example.services.interfaces.TasksService
import com.example.services.interfaces.UsersService
import kotlinx.datetime.toJavaLocalDateTime
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}


class TasksServiceImpl(private val tasksRepository: TasksRepository, private val usersService: UsersService) :
    TasksService {

    override fun getTasks(requestContext: RequestContext, query: TasksQueryRequest): List<TaskRecord> {
        return tasksRepository.getTasks(query).filter { isTaskAuthorizedForUser(it.taskDetails, requestContext.user) }
    }

    override fun getAuthorizedTaskById(requestContext: RequestContext, id: Int): TaskRecord {
        return tasksRepository.getTaskById(id)?.also { task ->
            if (!isTaskAuthorizedForUser(task.taskDetails, requestContext.user)){
                throw Exceptions.TaskNotAuthorizedForUser(task.taskDetails, requestContext.user.username)
            }
        } ?: throw Exceptions.TaskNotFoundException(id)
    }

    override fun getTaskHistory(id: Int) : List<TaskRevision>{
        return tasksRepository.getTaskHistory(id)
    }

    override fun insertTask(requestContext: RequestContext, task: TaskDetails): Int {
        task
            .validateOwnerExists()
            .isAuthorizedForUser(requestContext.user)
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(1000)
            .also {
                val id = tasksRepository.insertTask(requestContext, task)
                logger.info { "Task $task with id $id created successfully" }
                return id
            }
    }

    override fun updateTask(requestContext: RequestContext, id: Int, task: TaskDetails) {
        val currentTask = getAuthorizedTaskById(requestContext, id)
        task
            .validateOwnerExists()
            .isAuthorizedForUser(requestContext.user) // Check if the user is authorized for the updated task
            .isDueDateInFuture()
            .validateTitleLength(100)
            .validateDescriptionLength(1000)
            .also { taskToUpdate ->
                // Check if the user is authorized for the previous owner of the task
                if (currentTask.taskDetails.owner != taskToUpdate.owner) {
                    currentTask.taskDetails.isAuthorizedForUser(requestContext.user)
                }
                tasksRepository.updateTask(requestContext, id, taskToUpdate)
                logger.info { "Task with id $id updated successfully: $currentTask -> $task" }
            }
    }

    override fun deleteTask(requestContext: RequestContext, id: Int) {
        val task = getAuthorizedTaskById(requestContext, id)
        task.taskDetails.isAuthorizedForUser(requestContext.user)

        tasksRepository.deleteTask(requestContext, id)
        logger.info { "task with id $id deleted successfully" }
    }

    // returns true if
    //   task has no owner
    //   task's owner is the logged-in user
    //   logged-in user is admin
    //   task's owner is managed by the current logged-in user who is a manger
    private fun isTaskAuthorizedForUser(task: TaskDetails, user: User) : Boolean {
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

    private fun TaskDetails.isAuthorizedForUser(user: User): TaskDetails {
        if (!isTaskAuthorizedForUser(this, user)) {
            throw Exceptions.TaskNotAuthorizedForUser(this, user.username)
        }
        return this
    }

    private fun TaskDetails.isDueDateInFuture(): TaskDetails {
        if (this.dueDate.toJavaLocalDateTime().isBefore(LocalDateTime.now())) {
            throw Exceptions.TaskDueDatePastException(this)
        }
        return this
    }


    private fun TaskDetails.validateTitleLength(maxLength: Int): TaskDetails {
        when{
            title.isBlank() -> throw Exceptions.MissingTaskTitleException(this)
            title.length > maxLength -> throw Exceptions.TaskFieldExceededMaxLength(this, "title", maxLength)
            else -> return this
        }
    }

    private fun TaskDetails.validateDescriptionLength(maxLength: Int): TaskDetails {
        if (description != null && description.length > maxLength){
            throw Exceptions.TaskFieldExceededMaxLength(this, "description", maxLength)
        }
        return this
    }

    private fun TaskDetails.validateOwnerExists(): TaskDetails {
        owner?.let {
            usersService.getUserById(owner) ?: throw Exceptions.TaskOwnerDoesntExist(this)
        }
        return this
    }
}