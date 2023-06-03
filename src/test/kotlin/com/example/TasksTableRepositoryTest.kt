package com.example

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.models.UpdateType
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
    private val task2 = Task("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, 1, LocalDateTime.parse("2024-01-01T00:00:00"), 2)

    @Before
    fun resetDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable, TasksRepository.TasksRevisionsTable)
            SchemaUtils.createMissingTablesAndColumns(TasksRepository.TasksTable, TasksRepository.TasksRevisionsTable)
        }
    }

    @After
    fun cleanDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable, TasksRepository.TasksRevisionsTable)
        }
    }

    @Test
    fun testInsertTask(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
        }

        val readItems = tasksRepository.getAllTasks()
        assertEquals(task1, readItems.first())

    }

    @Test
    fun testUpdateTask(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
            tasksRepository.insertTask("admin", task2)
        }

        tasksRepository.updateTask("admin", task1.copy(status = TaskStatus.COMPLETED))
        val updatedTask = tasksRepository.getTaskById(task1.taskId)
        assertEquals(task1.copy(status = TaskStatus.COMPLETED), updatedTask)
    }

    @Test
    fun testDeleteTask(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
            tasksRepository.insertTask("admin", task2)
        }

        tasksRepository.deleteTask("admin", task1.taskId)
        val deletedTask = tasksRepository.getTaskById(task1.taskId)
        assertNull(deletedTask)

        val readItems = tasksRepository.getAllTasks()
        assertEquals(1, readItems.size)
        assertEquals(listOf(task2), readItems)
    }

    @Test
    fun testGetAllTasks(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
            tasksRepository.insertTask("admin", task2)
        }

        val readItems = tasksRepository.getAllTasks()
        assertEquals(2, readItems.size)
        assertEquals(listOf(task1, task2), readItems)

    }

    @Test
    fun testGetTaskById(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
            tasksRepository.insertTask("admin", task2)
        }

        val task = tasksRepository.getTaskById(2)
        assertEquals(task2, task)

        val noneExistingTask = tasksRepository.getTaskById(7)
        assertNull(noneExistingTask)
    }

    @Test
    fun testGetTaskHistory(){
        transaction (db) {
            tasksRepository.insertTask("admin", task1)
            tasksRepository.updateTask("admin", task1.copy(status = TaskStatus.IN_PROGRESS))
            tasksRepository.updateTask("admin", task1.copy(status = TaskStatus.COMPLETED))
            tasksRepository.deleteTask("admin", task1.taskId)
        }

        val taskHistory = tasksRepository.getTaskHistory(task1.taskId)
        assertEquals(4, taskHistory.size)
        assertEquals(UpdateType.CREATE, taskHistory[0].updateType)
        assertEquals(TaskStatus.NOT_STARTED, taskHistory[0].task.status)
        assertEquals(UpdateType.UPDATE, taskHistory[1].updateType)
        assertEquals(TaskStatus.IN_PROGRESS, taskHistory[1].task.status)
        assertEquals(UpdateType.UPDATE, taskHistory[2].updateType)
        assertEquals(TaskStatus.COMPLETED, taskHistory[2].task.status)
        assertEquals(UpdateType.DELETE, taskHistory[3].updateType)
    }
}

