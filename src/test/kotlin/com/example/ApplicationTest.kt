package com.example

import com.example.models.Task
import com.example.models.TaskSeverity
import com.example.models.TaskStatus
import com.example.repository.TasksRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
}
