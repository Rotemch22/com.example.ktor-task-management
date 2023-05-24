package com.example.repository

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.models.TasksQueryRequest.TasksQueryRequest
import com.example.models.TasksQueryRequest.TaskSortOrder
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource

private val logger = KotlinLogging.logger {}

class TasksRepository {
    private val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks"
    }
    private val db = Database.connect(dataSource)


    fun insertTask(task: Task): Int {
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
        return id.value
    }

    fun updateTask(task: Task) {
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
                val errorMsg = "No task with id ${task.taskId} exists in the db"
                logger.error { errorMsg }
                throw IllegalArgumentException(errorMsg)
            }

            logger.info { "task $task successfully updated in db" }
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


    fun getTaskById(id: Int): Task? {
        return transaction(db) {
            TasksTable.select(TasksTable.id eq id).map {
                TasksTable.toTask(it)
            }.firstOrNull()
        }
    }

    fun deleteTask(id: Int) {
        transaction(db) {
            val rowsUpdated = TasksTable.deleteWhere { TasksTable.id eq id }

            // if no rows were updated then the task with given id does not exist
            if (rowsUpdated == 0) {
                val errorMsg = "No task with id $id exists in the db"
                logger.error { errorMsg }
                throw IllegalArgumentException(errorMsg)
            }

            logger.info { "task with id $id successfully deleted from the db" }
        }
    }

    object TasksTable : IntIdTable() {
        val title = varchar("title", 100)
        val description = varchar("description", 1000).nullable()
        val status = enumeration("task_status", TaskStatus::class)
        val severity = enumeration("task_severity", TaskSeverity::class)
        val owner = varchar("owner", 100).nullable()
        val dueDate = datetime("dueDate")
    }

    private fun TasksTable.toTask(it: ResultRow): Task {
        val title = it[title]
        val description = it[description]
        val status = it[status]
        val severity = it[severity]
        val owner = it[owner]
        val dueDate = it[dueDate].toKotlinLocalDateTime()
        val taskId = it[id].value
        return Task(title, description, status, severity, owner, dueDate, taskId)
    }
}