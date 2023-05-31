package com.example

import com.example.exceptions.Exceptions
import com.example.models.*
import com.example.repository.TasksRepository
import com.example.services.TasksService
import com.example.services.UsersService
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.toKotlinLocalDateTime
import org.junit.Test
import kotlin.test.assertFailsWith

class TasksServiceTest {
    private val tasksRepository = mockk<TasksRepository>()
    private val usersService = mockk<UsersService>()
    private val taskService = TasksService(tasksRepository, usersService)


    @Test
    fun `test insert task with past due date`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        assertFailsWith(Exceptions.TaskDueDatePastException::class) {
            taskService.insertTask(
                "admin",
                Task(
                    "title",
                    "description",
                    TaskStatus.NOT_STARTED,
                    TaskSeverity.LOW,
                    null,
                    java.time.LocalDateTime.now().minusDays(1).toKotlinLocalDateTime()
                )
            )
        }
    }

    @Test
    fun `test update task with past due date`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        val task = Task(
            "title",
            "description",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            null,
            java.time.LocalDateTime.now().plusDays(1).toKotlinLocalDateTime(),
            1
        )
        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.TaskDueDatePastException::class) {
            taskService.updateTask(
                "admin",
                1,
                task.copy(dueDate = java.time.LocalDateTime.now().minusDays(1).toKotlinLocalDateTime())
            )
        }
    }

    @Test
    fun `test update task with mis matching ids`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        val task = Task(
            "title",
            "description",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            null,
            java.time.LocalDateTime.now().plusDays(1).toKotlinLocalDateTime(),
            1
        )
        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.MismatchedTaskIdException::class) {
            taskService.updateTask(
                "admin",
                1,
                task.copy(title = "newTitle", taskId =  2)
            )
        }
    }

    @Test
    fun `test update none existing task`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        val task = Task(
            "title",
            "description",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            null,
            java.time.LocalDateTime.now().plusDays(1).toKotlinLocalDateTime(),
            1
        )
        every {tasksRepository.getTaskById(1)} returns null
        assertFailsWith(Exceptions.TaskNotFoundException::class) {
            taskService.updateTask(
                "admin",
                1,
                task.copy(title = "newTitle")
            )
        }
    }

    @Test
    fun `test delete none existing task`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        every {tasksRepository.getTaskById(1)} returns null

        assertFailsWith(Exceptions.TaskNotFoundException::class) {
            taskService.deleteTask("admin", 1)
        }
    }
}