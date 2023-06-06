package com.example.repository.interfaces

import com.example.RequestContext
import com.example.models.TaskDetails
import com.example.models.TaskRecord
import com.example.models.TaskRevision
import com.example.models.TasksQueryRequest

/**
 * Repository interface for managing tasks in the database.
 */
interface TasksRepository {
    /**
     * Inserts a new task into the database.
     * @param requestContext The request context.
     * @param taskDetails The task to be inserted.
     * @return The ID of the inserted task.
     */
    fun insertTask(requestContext: RequestContext, taskDetails: TaskDetails): Int

    /**
     * Updates an existing task in the database.
     * @param requestContext The request context.
     * @param id: the id of the task.
     * @param taskDetails The updated task.
     */
    fun updateTask(requestContext: RequestContext, id: Int, taskDetails: TaskDetails)

    /**
     * Retrieves all tasks from the database.
     * @return A list of tasks.
     */
    fun getAllTasks(): List<TaskRecord>

    /**
     * Retrieves tasks from the database based on the specified query parameters.
     * @param request The query request.
     * @return A list of tasks matching the query parameters.
     */
    fun getTasks(request: TasksQueryRequest): List<TaskRecord>

    /**
     * Retrieves the task history for a specific task.
     * @param taskId The ID of the task.
     * @return A list of task revisions.
     */
    fun getTaskHistory(taskId: Int): List<TaskRevision>

    /**
     * Retrieves a task by its ID.
     * @param id The ID of the task.
     * @return The task with the specified ID, or null if not found.
     */
    fun getTaskById(id: Int): TaskRecord?

    /**
     * Deletes a task from the database.
     * @param requestContext The request context.
     * @param id The ID of the task to be deleted.
     */
    fun deleteTask(requestContext: RequestContext, id: Int)
}
