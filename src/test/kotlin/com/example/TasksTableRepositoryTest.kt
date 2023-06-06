package com.example

import com.example.models.*
import com.example.repository.TasksRepositoryImpl
import com.example.repository.UsersRepositoryImpl
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
    private val tasksRepository = TasksRepositoryImpl(db)
    private val usersRepository = UsersRepositoryImpl(db)

    private val task1 = TaskDetails("task1","task description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null, LocalDateTime.parse("2023-08-30T18:43:00"))
    private val task2 = TaskDetails("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, 1, LocalDateTime.parse("2024-01-01T00:00:00"))

    private var adminId: Int? = null
    private var user: User? = null

    @Before
    fun resetDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepositoryImpl.TasksTable, TasksRepositoryImpl.TasksRevisionsTable, UsersRepositoryImpl.UsersTable)
            SchemaUtils.createMissingTablesAndColumns(TasksRepositoryImpl.TasksTable, TasksRepositoryImpl.TasksRevisionsTable, UsersRepositoryImpl.UsersTable)
        }

        adminId = usersRepository.initializeAdminUser()
        user = User("admin", "admin", "admin@email.com", Role.ADMIN, null, adminId!!)
    }

    @After
    fun cleanDB(){
        transaction (db) {
            SchemaUtils.drop(TasksRepositoryImpl.TasksTable, TasksRepositoryImpl.TasksRevisionsTable, UsersRepositoryImpl.UsersTable)
        }
    }

    @Test
    fun testInsertTask(){
        transaction (db) {
            tasksRepository.insertTask(RequestContext(user!!), task1)
        }

        val tasks = tasksRepository.getAllTasks()
        assertEquals(1, tasks.size)
        assertEquals(task1, tasks.first().taskDetails)

    }

    @Test
    fun testUpdateTask(){
        var taskId : Int? = null
        transaction (db) {
            taskId = tasksRepository.insertTask(RequestContext(user!!), task1)
            tasksRepository.insertTask(RequestContext(user!!), task2)
        }

        tasksRepository.updateTask(RequestContext(user!!), taskId!!, task1.copy(status = TaskStatus.COMPLETED))
        val updatedTask = tasksRepository.getTaskById(taskId!!)
        assertEquals(task1.copy(status = TaskStatus.COMPLETED), updatedTask?.taskDetails)
        assertEquals(taskId, updatedTask?.taskId)
    }

    @Test
    fun testDeleteTask(){
        var taskId1 : Int? = null
        var taskId2 : Int? = null
        transaction (db) {
            taskId1 = tasksRepository.insertTask(RequestContext(user!!), task1)
            taskId2 = tasksRepository.insertTask(RequestContext(user!!), task2)
        }

        tasksRepository.deleteTask(RequestContext(user!!), taskId1!!)
        val deletedTask = tasksRepository.getTaskById(taskId1!!)
        assertNull(deletedTask)

        val tasks = tasksRepository.getAllTasks()
        assertEquals(1, tasks.size)
        assertEquals(listOf(TaskRecord(task2, taskId2!!)), tasks)
    }

    @Test
    fun testGetAllTasks(){
        transaction (db) {
            tasksRepository.insertTask(RequestContext(user!!), task1)
            tasksRepository.insertTask(RequestContext(user!!), task2)
        }

        val tasks = tasksRepository.getAllTasks()
        assertEquals(2, tasks.size)
        assertEquals(listOf(task1, task2), tasks.map { it.taskDetails })

    }

    @Test
    fun testGetTaskById(){
        transaction (db) {
            tasksRepository.insertTask(RequestContext(user!!), task1)
            tasksRepository.insertTask(RequestContext(user!!), task2)
        }

        val task = tasksRepository.getTaskById(2)
        assertEquals(task2, task?.taskDetails)

        val noneExistingTask = tasksRepository.getTaskById(7)
        assertNull(noneExistingTask)
    }

    @Test
    fun testGetTaskHistory(){
        var taskId : Int? = null
        transaction (db) {
            taskId = tasksRepository.insertTask(RequestContext(user!!), task1)
            tasksRepository.updateTask(RequestContext(user!!), taskId!!, task1.copy(status = TaskStatus.IN_PROGRESS))
            tasksRepository.updateTask(RequestContext(user!!), taskId!!, task1.copy(status = TaskStatus.COMPLETED))
            tasksRepository.deleteTask(RequestContext(user!!), taskId!!)
        }

        val taskHistory = tasksRepository.getTaskHistory(taskId!!)
        assertEquals(4, taskHistory.size)
        assertEquals(UpdateType.CREATE, taskHistory[0].updateType)
        assertEquals(TaskStatus.NOT_STARTED, taskHistory[0].task.taskDetails.status)
        assertEquals(UpdateType.UPDATE, taskHistory[1].updateType)
        assertEquals(TaskStatus.IN_PROGRESS, taskHistory[1].task.taskDetails.status)
        assertEquals(UpdateType.UPDATE, taskHistory[2].updateType)
        assertEquals(TaskStatus.COMPLETED, taskHistory[2].task.taskDetails.status)
        assertEquals(UpdateType.DELETE, taskHistory[3].updateType)
    }
}

