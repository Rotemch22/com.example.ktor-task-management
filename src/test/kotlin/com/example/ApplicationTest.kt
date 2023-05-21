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
import io.mockk.mockk
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ApplicationTest {

    private val tasksRepository = mockk<TasksRepository>()
    private val task1 = Task("task1","task description1", TaskStatus.NOT_STARTED, TaskSeverity.HIGH, null, 1)
    private val task2 = Task("task2","task description2", TaskStatus.IN_PROGRESS, TaskSeverity.URGENT, "some owner", 2)

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
            install(ContentNegotiation) { // Error on the `install` call
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
            install(ContentNegotiation) { // Error on the `install` call
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
            install(ContentNegotiation) { // Error on the `install` call
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
}
