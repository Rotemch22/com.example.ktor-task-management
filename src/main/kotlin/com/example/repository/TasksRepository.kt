package com.example.repository

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource


class TasksRepository {
    val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks"
    }
    val db = Database.connect(dataSource)


    fun insertTask(task: Task): Int {
        val id = transaction(db) {
            TasksTable.insertAndGetId {
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[severity] = task.severity
                it[owner] = task.owner
            }

        }

        return id.value
    }

    fun getAllTasks(): List<Task> {
        return transaction(db) {
            TasksTable.selectAll().map {
                TasksTable.toTask(it)
            }
        }
    }

    fun getTaskById(id: Int) : Task? {
        return transaction(db) {
             TasksTable.select(TasksTable.id eq id).map {
                TasksTable.toTask(it)
            }.firstOrNull()
        }
    }

    object TasksTable : IntIdTable() {
        val title = varchar("title", 100)
        val description = varchar("description", 100).nullable()
        val status = enumeration("task_status", TaskStatus::class)
        val severity = enumeration("task_severity", TaskSeverity::class)
        val owner = varchar("owner", 100).nullable()
    }

    private fun TasksTable.toTask(it: ResultRow): Task {
        val title = it[title]
        val description = it[description]
        val status = it[status]
        val severity = it[severity]
        val owner = it[owner]
        val taskId = it[id].value
        return Task(title, description, status, severity, owner, taskId)
    }
}