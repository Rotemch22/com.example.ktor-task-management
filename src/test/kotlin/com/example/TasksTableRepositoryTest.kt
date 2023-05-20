package com.example

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.repository.TasksRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertEquals

val dataSource = PGSimpleDataSource().apply {
    user = "test"
    password = "test"
    databaseName = "tasks"
}
val db = Database.connect(dataSource)

class TasksTableRepositoryTest {

    private val task = Task("task","task description", TaskStatus.NOT_STARTED, TaskSeverity.URGENT, null)
    private val tasksRepository = TasksRepository()

    @Before
    fun resetDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable)
            SchemaUtils.createMissingTablesAndColumns(TasksRepository.TasksTable)
        }
    }

    @Test
    fun testInsertTask(){
        transaction {
            tasksRepository.insertTask(task)
        }

        transaction {
            val readItems = tasksRepository.getAllTasks()

            assertEquals(task, readItems.first())
        }

    }




}

