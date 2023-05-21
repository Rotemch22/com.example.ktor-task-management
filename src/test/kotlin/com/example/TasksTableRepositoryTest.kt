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
import kotlin.test.assertNull

val dataSource = PGSimpleDataSource().apply {
    user = "test"
    password = "test"
    databaseName = "tasks"
}
val db = Database.connect(dataSource)

class TasksTableRepositoryTest {

    private val task1 = Task("task1","task description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null, 1)
    private val task2 = Task("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, "some owner", 2)
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
        transaction (db) {
            tasksRepository.insertTask(task1)
        }

        val readItems = tasksRepository.getAllTasks()
        assertEquals(task1, readItems.first())

    }


    @Test
    fun testGetAllTasks(){
        transaction (db) {
            tasksRepository.insertTask(task1)
            tasksRepository.insertTask(task2)
        }

        val readItems = tasksRepository.getAllTasks()
        assertEquals(2, readItems.size)
        assertEquals(listOf(task1, task2), readItems)

    }

    @Test
    fun testGetTaskById(){
        transaction (db) {
            tasksRepository.insertTask(task1)
            tasksRepository.insertTask(task2)
        }

        val task = tasksRepository.getTaskById(2)
        assertEquals(task2, task)

        val noneExistingTask = tasksRepository.getTaskById(7)
        assertNull(noneExistingTask)
    }

}

