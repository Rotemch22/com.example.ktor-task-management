package com.example

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.repository.TasksRepository
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull



class TasksTableRepositoryTest {
    private val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks_test"
        serverName = "localhost"
        portNumber = 5433
    }
    private val db = Database.connect(dataSource)
    private val tasksRepository = TasksRepository(db)

    private val task1 = Task("task1","task description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null, LocalDateTime.parse("2023-08-30T18:43:00"),1)
    private val task2 = Task("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, "some owner", LocalDateTime.parse("2024-01-01T00:00:00"), 2)

    @Before
    fun resetDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable)
            SchemaUtils.createMissingTablesAndColumns(TasksRepository.TasksTable)
        }
    }

    @After
    fun cleanDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable)
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
    fun testUpdateTask(){
        transaction (db) {
            tasksRepository.insertTask(task1)
            tasksRepository.insertTask(task2)
        }

        tasksRepository.updateTask(task1.copy(status = TaskStatus.COMPLETED))
        val updatedTask = tasksRepository.getTaskById(task1.taskId)
        assertEquals(task1.copy(status = TaskStatus.COMPLETED), updatedTask)
    }

    @Test
    fun testDeleteTask(){
        transaction (db) {
            tasksRepository.insertTask(task1)
            tasksRepository.insertTask(task2)
        }

        tasksRepository.deleteTask(task1.taskId)
        val deletedTask = tasksRepository.getTaskById(task1.taskId)
        assertNull(deletedTask)

        val readItems = tasksRepository.getAllTasks()
        assertEquals(1, readItems.size)
        assertEquals(listOf(task2), readItems)
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

