import com.example.RequestContext
import com.example.exceptions.Exceptions
import com.example.models.Task
import com.example.models.TaskRevision
import com.example.models.TasksQueryRequest

/**
 * Interface defining the contract for the TasksService.
 */
interface TasksService {
    /**
     * Retrieves a list of tasks based on the provided query parameters.
     * Only tasks authorized for the user in the given request context will be returned.
     *
     * @param requestContext The request context containing the user information.
     * @param query The query parameters for filtering the tasks.
     * @return The list of authorized tasks.
     * @throws Exceptions.TaskNotAuthorizedForUser if a task is not authorized for the user in the request context.
     */
    fun getTasks(requestContext: RequestContext, query: TasksQueryRequest): List<Task>

    /**
     * Retrieves the authorized task by its ID.
     *
     * @param requestContext The request context containing the user information.
     * @param id The ID of the task to retrieve.
     * @return The authorized task.
     * @throws Exceptions.TaskNotFoundException if the task with the given ID is not found.
     * @throws Exceptions.TaskNotAuthorizedForUser if the task is not authorized for the user in the request context.
     */
    fun getAuthorizedTaskById(requestContext: RequestContext, id: Int): Task

    /**
     * Retrieves the history of revisions for a task.
     *
     * @param id The ID of the task.
     * @return The list of task revisions.
     */
    fun getTaskHistory(id: Int): List<TaskRevision>

    /**
     * Inserts a new task into the system.
     *
     * @param requestContext The request context containing the user information.
     * @param task The task to insert.
     * @return The ID of the inserted task.
     * @throws Exceptions.TaskNotAuthorizedForUser if the task is not authorized for the user in the request context.
     * @throws Exceptions.TaskDueDatePastException if the task's due date is in the past.
     * @throws Exceptions.MissingTaskTitleException if the task title is empty.
     * @throws Exceptions.TaskFieldExceededMaxLength if the task title or description length exceeds the maximum limits.
     */
    fun insertTask(requestContext: RequestContext, task: Task): Int

    /**
     * Updates an existing task in the system.
     *
     * @param requestContext The request context containing the user information.
     * @param id The ID of the task to update.
     * @param task The updated task data.
     * @throws Exceptions.TaskNotFoundException if the task with the given ID is not found.
     * @throws Exceptions.TaskNotAuthorizedForUser if the task is not authorized for the user in the request context.
     * @throws Exceptions.MismatchedTaskIdException if the ID in the URL does not match the task's ID.
     * @throws Exceptions.TaskDueDatePastException if the task's due date is in the past.
     * @throws Exceptions.MissingTaskTitleException if the task title is empty.
     * @throws Exceptions.TaskFieldExceededMaxLength if the task title or description length exceeds the maximum limits.
     */
    fun updateTask(requestContext: RequestContext, id: Int, task: Task)

    /**
     * Deletes a task from the system.
     *
     * @param requestContext The request context containing the user information.
     * @param id The ID of the task to delete.
     * @throws Exceptions.TaskNotFoundException if the task with the given ID is not found.
     * @throws Exceptions.TaskNotAuthorizedForUser if the task is not authorized for the user in the request context.
     */
    fun deleteTask(requestContext: RequestContext, id: Int)
}
