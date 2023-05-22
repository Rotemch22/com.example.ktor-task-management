package com.example

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.repository.TasksRepository
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ApplicationTest {

    private val tasksRepository = mockk<TasksRepository>()
    private val task1 = Task("task1","task description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null, LocalDateTime.parse("2023-08-30T18:43:00"), 1)
    private val task2 = Task("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, "some owner", LocalDateTime.parse("2024-01-01T00:00:00"), 2)

    @Test
    fun `test get empty list of tasks`() = testApplication {
        application{
            module(tasksRepository)
        }

        every { tasksRepository.getAllTasks() } returns emptyList()

        val response = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `test get list of tasks`() = testApplication {
        application{
            module(tasksRepository)
        }

        every { tasksRepository.getAllTasks() } returns listOf(task1, task2)

        val response = client.get("/tasks")
        val responseTasks: List<Task> = Json.decodeFromString(ListSerializer(Task.serializer()), response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf(task1, task2), responseTasks)
    }



    @Test
    fun `test post task`() = testApplication {
        application{
            module(tasksRepository)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.insertTask(task1) } returns 1
        val response = client.post("/tasks"){
            contentType(ContentType.Application.Json)
            setBody(task1)
        }

        val responseTask: Task = Json.decodeFromString(Task.serializer(), response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(task1, responseTask)
    }

    @Test
    fun `test post task missing body`() = testApplication {
        application{
            module(tasksRepository)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }


        val response = client.post("/tasks"){
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `test post task invalid body`() = testApplication {
        application{
            module(tasksRepository)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.insertTask(task1) } returns 1

        // send a request with an invalid task json (missing the severity field) and assert we get the correct error code in response
        val response = client.post("/tasks"){
            contentType(ContentType.Application.Json)
            setBody("{\n" +
                    "\"title\": \"sample task\",\n" +
                    "\"description\": null,\n" +
                    "\"status\": \"NOT_STARTED\",\n" +
                    "\"owner\": null\n" +
                    "}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test update task`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val updatedTask = task1.copy(status = TaskStatus.COMPLETED)

        every { tasksRepository.getTaskById(task1.taskId) } returns task1
        every { tasksRepository.updateTask(updatedTask) } just runs

        val response = client.put("/tasks/${task1.taskId}"){
            contentType(ContentType.Application.Json)
            setBody(updatedTask)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseTask: Task = Json.decodeFromString(Task.serializer(), response.bodyAsText())
        assertEquals(updatedTask, responseTask)
    }

    @Test
    fun `test update task with mismatch ids`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }


        val response = client.put("/tasks/7"){
            contentType(ContentType.Application.Json)
            setBody(task1.copy(status = TaskStatus.COMPLETED))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `test update none existing task`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.getTaskById(7) } returns null

        val response = client.put("/tasks/7"){
            contentType(ContentType.Application.Json)
            setBody(task1.copy(status = TaskStatus.COMPLETED, taskId = 7))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test delete task`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.getTaskById(1) } returns task1
        every { tasksRepository.deleteTask(1) } just runs

        val response = client.delete("/tasks/${task1.taskId}")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test delete none existing task`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.getTaskById(7) } returns null

        val response = client.delete("/tasks/7")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun `test delete none numerical task id`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }


        val response = client.delete("/tasks/not a number")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test create task with past due date`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }


        val response = client.post("/tasks"){
            contentType(ContentType.Application.Json)
            setBody(task1.copy(dueDate = LocalDateTime.parse("2023-01-01T00:00:00")))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test update task with past due date`() = testApplication {
        application{
            module(tasksRepository)
        }


        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        every { tasksRepository.getTaskById(task1.taskId) } returns task1

        val response = client.put("/tasks/${task1.taskId}"){
            contentType(ContentType.Application.Json)
            setBody(task1.copy(dueDate = LocalDateTime.parse("2023-01-01T00:00:00")))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
