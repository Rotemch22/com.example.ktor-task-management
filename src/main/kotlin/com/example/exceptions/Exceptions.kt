package com.example.exceptions

import com.example.models.Task
import com.example.routes.UserInput
import com.example.routes.UserResponse

class Exceptions {

    class TaskNotFoundException(id: Int) : Exception("Task with id $id not found")
    open class InvalidTaskException(message: String) : Exception(message)
    class TaskDueDatePastException(task: Task) : InvalidTaskException("Task $task can't be created/updated with due date ${task.dueDate} in the past")
    class MismatchedTaskIdException(urlId: Int, bodyId: Int) : Exception("The task ID in the URL $urlId does not match the taskId in the request body $bodyId")
    class InvalidTaskQueryValueException(value: String, field: String): Exception("Invalid value $value for field $field")
    class EndUserWithoutManager(user : UserResponse): Exception("user $user does not have an assigned manager")
    class UserWithInvalidManagerId(userInput : UserInput): Exception("user $userInput with invalid manager id, no matching manager found for manager id: ${userInput.managerId}")

}@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)