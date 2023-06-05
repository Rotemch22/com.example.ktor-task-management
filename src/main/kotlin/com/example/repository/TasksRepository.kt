package com.example.repository

import com.example.RequestContext
import com.example.exceptions.Exceptions
import com.example.models.*
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class TasksRepository (private val db: Database) {

    fun insertTask(requestContext: RequestContext, task: Task): Int {
        val id = transaction(db) {
            TasksTable.insertAndGetId {
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[severity] = task.severity
                it[owner] = task.owner
                it[dueDate] = task.dueDate.toJavaLocalDateTime()
            }
        }

        logger.info { "task $task successfully created in db with id ${id.value}" }

        addTaskRevision(requestContext.user.userId, task.copy(taskId = id.value), UpdateType.CREATE)
        return id.value
    }

    fun updateTask(requestContext: RequestContext, task: Task) {
        transaction(db) {
            val rowsUpdated = TasksTable.update({ TasksTable.id eq task.taskId })
            {
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[severity] = task.severity
                it[owner] = task.owner
                it[dueDate] = task.dueDate.toJavaLocalDateTime()
            }

            // if no rows were updated then the task with given id does not exist
            if (rowsUpdated == 0) {
                logger.error { "No task with id ${task.taskId} exists in the db" }
                throw Exceptions.TaskNotFoundException(task.taskId)
            }

            logger.info { "task $task successfully updated in db" }
            addTaskRevision(requestContext.user.userId, task, UpdateType.UPDATE)
        }
    }

    fun getAllTasks(): List<Task> {
        return transaction(db) {
            TasksTable.selectAll().map {
                TasksTable.toTask(it)
            }
        }
    }

    fun getTasks(request: TasksQueryRequest): List<Task> {
        return transaction(db) {
            // create query using the request if they are not null
            val tasksQuery = TasksTable.select {
                (request.status?.let { TasksTable.status eq it } ?: Op.TRUE) and
                        (request.severity?.let { TasksTable.severity eq it } ?: Op.TRUE) and
                        (request.owner?.let { TasksTable.owner eq it } ?: Op.TRUE)
            }

            // adding order to the query if the order in request is not null
            request.order?.let {
                val taskSortOrder = when (request.order) {
                    TaskSortOrder.DESC -> SortOrder.DESC
                    TaskSortOrder.ASC -> SortOrder.ASC
                }
                tasksQuery.orderBy(TasksTable.dueDate to taskSortOrder)
            }

            tasksQuery.map { TasksTable.toTask(it) }
        }
    }


    fun getTaskHistory(taskId: Int) : List<TaskRevision>{
        return transaction(db) {
            TasksRevisionsTable.select(TasksRevisionsTable.taskId eq taskId)
                .orderBy(TasksRevisionsTable.revision).map { TasksRevisionsTable.toTaskRevision(it) }
        }
    }

    fun getTaskById(id: Int): Task? {
        return transaction(db) {
            TasksTable.select(TasksTable.id eq id).map {
                TasksTable.toTask(it)
            }.singleOrNull()
        }
    }

    fun deleteTask(requestContext: RequestContext, id: Int) {
        transaction(db) {
            val task = TasksTable.select(TasksTable.id eq id).map {
                TasksTable.toTask(it)
            }.singleOrNull()

            if (task == null) {
                logger.error { "No task with id $id exists in the db" }
                throw Exceptions.TaskNotFoundException(id)
            }

            TasksTable.deleteWhere { TasksTable.id eq id }
            logger.info { "task with id $id successfully deleted from the db" }
            addTaskRevision(requestContext.user.userId, task, UpdateType.DELETE)
        }
    }


    private fun addTaskRevision(loggedInUserId : Int, task: Task, update: UpdateType) {
        transaction(db) {
            val currentMaxRevision = TasksRevisionsTable
                .slice(TasksRevisionsTable.revision)
                .select { TasksRevisionsTable.taskId eq task.taskId }
                .maxOfOrNull { it[TasksRevisionsTable.revision] } ?: 0

            TasksRevisionsTable.insert {
                it[taskId] = task.taskId
                it[revision] = currentMaxRevision + 1
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[severity] = task.severity
                it[owner] = task.owner
                it[dueDate] = task.dueDate.toJavaLocalDateTime()
                it[modifiedBy] = loggedInUserId
                it[modifiedDate] = LocalDateTime.now()
                it[updateType] = update
            }

            logger.info { "task revision ${currentMaxRevision + 1} successfully inserted for task $task with update type $update" }
        }
    }


    object TasksTable : IntIdTable() {
        val title = varchar("title", 100)
        val description = varchar("description", 1000).nullable()
        val status = enumeration("task_status", TaskStatus::class)
        val severity = enumeration("task_severity", TaskSeverity::class)
        val owner = reference("owner", UsersRepository.UsersTable).nullable()
        val dueDate = datetime("dueDate")
    }

    object TasksRevisionsTable : Table() {
        val taskId = integer("task_id")
        val revision = integer("revision")
        val title = varchar("title", 100)
        val description = varchar("description", 1000).nullable()
        val status = enumeration("task_status", TaskStatus::class)
        val severity = enumeration("task_severity", TaskSeverity::class)
        val owner = reference("owner", UsersRepository.UsersTable).nullable()
        val dueDate = datetime("dueDate")
        val modifiedBy = reference("modified_by", UsersRepository.UsersTable)
        val modifiedDate = datetime("modified_date")
        val updateType = enumeration("update_type", UpdateType::class)
        init {
            uniqueIndex(taskId, revision)
        }
    }

    private fun TasksTable.toTask(it: ResultRow): Task {
        val title = it[title]
        val description = it[description]
        val status = it[status]
        val severity = it[severity]
        val owner = it[owner]?.value
        val dueDate = it[dueDate].toKotlinLocalDateTime()
        val taskId = it[id].value
        return Task(title, description, status, severity, owner, dueDate, taskId)
    }


    private fun TasksRevisionsTable.toTaskRevision(it: ResultRow): TaskRevision {
        val title = it[title]
        val description = it[description]
        val status = it[status]
        val severity = it[severity]
        val owner = it[owner]?.value
        val dueDate = it[dueDate].toKotlinLocalDateTime()
        val taskId = it[taskId]
        val revision = it[revision]
        val modifiedBy = it[modifiedBy].value
        val modifiedDate = it[modifiedDate].toKotlinLocalDateTime()
        val updateType = it[updateType]
        return TaskRevision(
            Task(title, description, status, severity, owner, dueDate, taskId),
            revision, modifiedBy, modifiedDate, updateType)
    }
}