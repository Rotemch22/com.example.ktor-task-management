package com.example.repository

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource


class TasksRepository {
    val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks"
    }
    val db = Database.connect(dataSource)


    fun insertTask(task: Task) {
        transaction(db) {
            TasksTable.insert {
                it[taskId] = task.taskId
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[severity] = task.severity
                it[owner] = task.owner

            }
        }
    }

    fun getAllTasks() : List<Task> {
        return transaction(db) {
            TasksTable.selectAll().map {
                val title = it[TasksTable.title]
                val description = it[TasksTable.description]
                val status = it[TasksTable.status]
                val severity = it[TasksTable.severity]
                val owner = it[TasksTable.owner]
                Task(title, description, status, severity, owner)
            }
        }
    }


    object TasksTable : Table() {
        val taskId = varchar("id", 100).uniqueIndex()
        val title = varchar("title", 100)
        val description = varchar("description", 100).nullable()
        val status = enumeration("task_status", TaskStatus::class)
        val severity = enumeration("task_severity", TaskSeverity::class)
        val owner = varchar("owner", 100).nullable()
    }


}