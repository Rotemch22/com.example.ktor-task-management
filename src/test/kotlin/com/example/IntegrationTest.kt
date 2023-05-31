package com.example

import com.example.models.Role
import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.repository.TasksRepository
import com.example.repository.UsersRepository
import com.example.routes.UserInput
import com.example.routes.UserResponse
import com.example.services.TasksService
import com.example.services.UsersService
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.postgresql.ds.PGSimpleDataSource
import java.time.LocalDateTime
import kotlin.test.*

class IntegrationTest {
    private val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks_test"
        serverName = "localhost"
        portNumber = 5433
    }
    private val db = Database.connect(dataSource)


    private val appModule = module {
        single { TasksRepository(db) }
        single { UsersRepository(db) }
        single { UsersService(get()) }
        single { TasksService(get(), get()) }
    }

    private var client: TestApplicationEngine? = null
    private var sessionCookie: Cookie? = null
    private val dueDate = LocalDateTime.now().withNano(0).plusHours(24).toKotlinLocalDateTime()

    private var manager1Id: Int? = null
    private var manager2Id: Int? = null
    private var user1Id: Int? = null
    private var user2Id: Int? = null
    private var user3Id: Int? = null

    private var task1: Task? = null
    private var task2: Task? = null
    private var task3: Task? = null
    private var task4: Task? = null

    @BeforeTest
    fun setup() {
        startKoin {
            modules(appModule)
        }

        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable, UsersRepository.UsersTable)
            SchemaUtils.createMissingTablesAndColumns(TasksRepository.TasksTable, UsersRepository.UsersTable)
            val usersRepository: UsersRepository = getKoin().get()
            usersRepository.initializeAdminUser()
        }

        client = TestApplicationEngine(createTestEnvironment())

        client!!.start()
        client!!.application.module(getKoin().get(), getKoin().get())

        with(client) {
            // Login with the username and password
            sessionCookie = this?.login("admin", "admin")!!

            manager1Id = createAndVerifyUser(sessionCookie, UserInput("manager1", "manager1", "manager1@email.com", Role.MANAGER))
            manager2Id = createAndVerifyUser(sessionCookie, UserInput("manager2", "manager2", "manager2@email.com", Role.MANAGER))
            user1Id = createAndVerifyUser(sessionCookie, UserInput("user1", "user1", "user1@email.com", Role.END_USER, manager1Id))
            user2Id = createAndVerifyUser(sessionCookie, UserInput("user2", "user2", "user2@email.com", Role.END_USER, manager1Id))
            user3Id = createAndVerifyUser(sessionCookie, UserInput("user3", "user3", "user3@email.com", Role.END_USER, manager2Id))

            task1 = createAndVerifyTask(
                sessionCookie, Task(
                    "title1", "description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, user1Id.toString(),
                    dueDate
                )
            )
            task2 = createAndVerifyTask(
                sessionCookie, Task(
                    "title2", "description2", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, user2Id.toString(),
                    dueDate
                )
            )
            task3 = createAndVerifyTask(
                sessionCookie, Task(
                    "title3", "description3", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, user3Id.toString(),
                    dueDate
                )
            )
            task4 = createAndVerifyTask(
                sessionCookie, Task(
                    "title4", "description4", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null,
                    dueDate
                )
            )
        }

    }

    @AfterTest
    fun teardown() {
        transaction (db) {
            SchemaUtils.drop(TasksRepository.TasksTable, UsersRepository.UsersTable)
        }

        client!!.stop(0L, 0L)
        stopKoin() // Stop Koin after each test to clean up
    }

    @Test
    fun `test GET tasks with admin logged-in user`() {
        with(client) {
            this?.handleRequest(HttpMethod.Get, "/tasks") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse?.response?.status())

                val tasks = Json.decodeFromString<List<Task>>(tasksResponse?.response?.content!!)
                assertEquals(listOf(task1, task2, task3, task4), tasks)
            }
        }
    }

    @Test
    fun `test GET tasks with manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager1", "manager1")!!
            handleRequest(HttpMethod.Get, "/tasks") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse.response.status())

                val tasks = Json.decodeFromString<List<Task>>(tasksResponse.response.content!!)
                assertEquals(listOf(task1, task2, task4), tasks)
            }

            sessionCookie = login("manager2", "manager2")!!
            handleRequest(HttpMethod.Get, "/tasks") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse.response.status())

                val tasks = Json.decodeFromString<List<Task>>(tasksResponse.response.content!!)
                assertEquals(listOf(task3, task4), tasks)
            }
        }
    }


    @Test
    fun `test GET tasks with end user logged-in user` (){
        with(client) {
            sessionCookie = this?.login("user1", "user1")!!
            handleRequest(HttpMethod.Get, "/tasks") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse.response.status())

                val tasks = Json.decodeFromString<List<Task>>(tasksResponse.response.content!!)
                assertEquals(listOf(task1, task4), tasks)
            }
        }
    }

    @Test
    fun `test GET task by id with end user logged-in user` (){
        with(client) {
            sessionCookie = this?.login("user1", "user1")!!
            handleRequest(HttpMethod.Get, "/tasks/${task1?.taskId}") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse.response.status())

                val task = Json.decodeFromString<Task>(tasksResponse.response.content!!)
                assertEquals(task1, task)
            }
        }
    }

    @Test
    fun `test GET task by id with unauthorized end user logged-in user` (){
        with(client) {
            sessionCookie = this?.login("user1", "user1")!!
            handleRequest(HttpMethod.Get, "/tasks/${task2?.taskId}") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.Unauthorized, tasksResponse.response.status())
            }
        }
    }

    @Test
    fun `test GET task by id with unauthorized manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager1", "manager1")!!
            handleRequest(HttpMethod.Get, "/tasks/${task3?.taskId}") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.Unauthorized, tasksResponse.response.status())
            }
        }
    }

    @Test
    fun `test update task with unauthorized manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager1", "manager1")!!
            handleRequest(HttpMethod.Put, "/tasks/${task3?.taskId}") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
                setBody(Json.encodeToString(task3?.copy(owner = manager1Id.toString())))
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.Unauthorized, tasksResponse.response.status())
            }
        }
    }

    @Test
    fun `test update task with authorized manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager2", "manager2")!!
            val updatedTask = task3?.copy(owner = manager2Id.toString())
            handleRequest(HttpMethod.Put, "/tasks/${task3?.taskId}") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
                setBody(Json.encodeToString(updatedTask))
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.OK, tasksResponse.response.status())
                val taskResponse = Json.decodeFromString<Task>(tasksResponse.response.content!!)
                assertEquals(updatedTask, taskResponse)
            }
        }
    }

    @Test
    fun `test create task with unauthorized manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager1", "manager1")!!
            val newTask = Task("title", "description", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, user3Id.toString(), dueDate)
            handleRequest(HttpMethod.Put, "/tasks/${task3?.taskId}") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
                setBody(Json.encodeToString(newTask))
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.Unauthorized, tasksResponse.response.status())
            }
        }
    }

    @Test
    fun `test delete task with unauthorized manager logged-in user` (){
        with(client) {
            sessionCookie = this?.login("manager1", "manager1")!!
            handleRequest(HttpMethod.Delete, "/tasks/${task3?.taskId}") {
                addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
            }.let { tasksResponse ->
                assertEquals(HttpStatusCode.Unauthorized, tasksResponse.response.status())
            }
        }
    }
}

private fun TestApplicationEngine.login(username: String, password: String): Cookie? {
    var sessionCookie: Cookie?

    handleRequest(HttpMethod.Post, "/com.example.login") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        setBody(listOf("username" to username, "password" to password).formUrlEncode())
    }.apply {
        // Retrieve the session cookie
        sessionCookie = response.cookies["SESSION"]
        assertNotNull(sessionCookie)
    }

    return sessionCookie
}

private fun TestApplicationEngine.createAndVerifyUser(sessionCookie: Cookie?, userInput: UserInput): Int {
    val response = handleRequest(HttpMethod.Post, "/users") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
        setBody(Json.encodeToString(userInput))
    }

    assertEquals(HttpStatusCode.Created, response.response.status())
    val userResponse = Json.decodeFromString<UserResponse>(response.response.content!!)
    assertEquals(userInput.username, userResponse.username)
    assertEquals(userInput.email, userResponse.email)
    assertEquals(userInput.role, userResponse.role)
    assertEquals(userInput.managerId, userResponse.manager?.userId)
    return userResponse.userId!!
}

private fun TestApplicationEngine.createAndVerifyTask(sessionCookie: Cookie?, task: Task): Task {
    val response = handleRequest(HttpMethod.Post, "/tasks") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        addHeader(HttpHeaders.Cookie, "SESSION=${sessionCookie?.value}")
        setBody(Json.encodeToString(task))
    }

    assertEquals(HttpStatusCode.Created, response.response.status())
    val taskResponse = Json.decodeFromString<Task>(response.response.content!!)
    assertEquals(task.title, taskResponse.title)
    assertEquals(task.description, taskResponse.description)
    assertEquals(task.dueDate, taskResponse.dueDate)
    assertEquals(task.status, taskResponse.status)
    assertEquals(task.severity, taskResponse.severity)
    assertEquals(task.owner, taskResponse.owner)
    return taskResponse
}