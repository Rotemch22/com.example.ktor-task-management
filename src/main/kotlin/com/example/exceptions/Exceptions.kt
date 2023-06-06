package com.example.exceptions

import com.example.models.Task
import com.example.routes.UserInput
import com.example.routes.UserResponse
import io.ktor.http.*
import kotlin.reflect.KClass

class Exceptions {

    class TaskNotFoundException(id: Int) : Exception("Task with id $id not found")
    open class InvalidTaskException(message: String) : Exception(message)
    class TaskDueDatePastException(task: Task) :
        InvalidTaskException("Task $task can't be created/updated with due date ${task.dueDate} in the past")

    class MismatchedTaskIdException(urlId: Int, bodyId: Int) :
        InvalidTaskException("The task ID in the URL $urlId does not match the taskId in the request body $bodyId")

    class MissingTaskTitleException(task: Task) : InvalidTaskException("Invalid task $task with an empty title")
    class TaskFieldExceededMaxLength(task: Task, field: String, length: Int) :
        InvalidTaskException("Invalid task $task, field $field exceeded max length $length")

    class TaskOwnerDoesntExist(task: Task) :
        InvalidTaskException("Task $task with none existing owner id ${task.owner}")

    class InvalidTaskQueryValueException(value: String, field: String) :
        Exception("Invalid value $value for field $field")

    class EndUserWithoutManager(user: UserResponse) : Exception("user $user does not have an assigned manager")
    class UserWithInvalidManagerId(userInput: UserInput) :
        Exception("user $userInput with invalid manager id, no matching manager found for manager id: ${userInput.managerId}")

    class NoLoggedInUserException : Exception("No logged in user found")
    class NoUserFoundForLoggedInUserException(username: String) :
        Exception("No user found for logged in username $username")

    class TaskNotAuthorizedForUser(task: Task, username: String) :
        Exception("Task $task is not authorized for user $username")

    companion object ExceptionStatusMap {
        val statusMap: Map<KClass<out Exception>, HttpStatusCode> = mapOf(
            TaskNotFoundException::class to HttpStatusCode.NotFound,
            MismatchedTaskIdException::class to HttpStatusCode.UnprocessableEntity,
            TaskDueDatePastException::class to HttpStatusCode.BadRequest,
            InvalidTaskQueryValueException::class to HttpStatusCode.BadRequest,
            EndUserWithoutManager::class to HttpStatusCode.BadRequest,
            UserWithInvalidManagerId::class to HttpStatusCode.BadRequest,
            NoLoggedInUserException::class to HttpStatusCode.Unauthorized,
            NoUserFoundForLoggedInUserException::class to HttpStatusCode.Unauthorized,
            TaskNotAuthorizedForUser::class to HttpStatusCode.Unauthorized,
            MissingTaskTitleException::class to HttpStatusCode.BadRequest,
            TaskFieldExceededMaxLength::class to HttpStatusCode.BadRequest,
            TaskOwnerDoesntExist::class to HttpStatusCode.BadRequest
        )
    }
}

@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)