package com.example

import com.example.exceptions.Exceptions
import com.example.models.*
import com.example.repository.interfaces.TasksRepository
import com.example.services.TasksServiceImpl
import com.example.services.interfaces.UsersService
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.toKotlinLocalDateTime
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class TasksServiceTest {

    private val user = User("admin", "admin", "admin@email.com", Role.ADMIN, null)

    private val tasksRepository = mockk<TasksRepository>()
    private val usersService = mockk<UsersService>()
    private val taskService = TasksServiceImpl(tasksRepository, usersService)

    private val task = TaskRecord(
        TaskDetails(
            "title",
            "description",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            null,
            java.time.LocalDateTime.now().plusDays(1).toKotlinLocalDateTime(),
            ),
        1
    )

    @Test
    fun `test insert task with past due date`() {
        every { usersService.getUserByUserName("admin") } returns user
        assertFailsWith(Exceptions.TaskDueDatePastException::class) {
            taskService.insertTask(
                RequestContext(user),
                task.taskDetails.copy(dueDate = java.time.LocalDateTime.now().minusDays(1).toKotlinLocalDateTime())
            )
        }
    }

    @Test
    fun `test update task with past due date`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)

        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.TaskDueDatePastException::class) {
            taskService.updateTask(
                RequestContext(user),
                1,
                task.taskDetails.copy(dueDate = java.time.LocalDateTime.now().minusDays(1).toKotlinLocalDateTime())
            )
        }
    }

    @Test
    fun `test delete none existing task`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        every {tasksRepository.getTaskById(1)} returns null

        assertFailsWith(Exceptions.TaskNotFoundException::class) {
            taskService.deleteTask(RequestContext(user), 1)
        }
    }

    @Test
    fun `test create or update task with missing title`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)

        assertFailsWith(Exceptions.MissingTaskTitleException::class) {
            taskService.insertTask(
                RequestContext(user),
                task.taskDetails.copy(title = "")
            )
        }

        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.MissingTaskTitleException::class) {
            taskService.updateTask(
                RequestContext(user),
                1,
                task.taskDetails.copy(title = "")
            )
        }
    }

    @Test
    fun `test create or update task with invalid title`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)

        assertFailsWith(Exceptions.TaskFieldExceededMaxLength::class) {
            taskService.insertTask(
                RequestContext(user),
                task.taskDetails.copy(title = generateRandomString(101))
            )
        }

        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.TaskFieldExceededMaxLength::class) {
            taskService.updateTask(
                RequestContext(user),
                1,
                task.taskDetails.copy(title = generateRandomString(101))
            )
        }
    }

    @Test
    fun `test create or update task with invalid description`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)

        assertFailsWith(Exceptions.TaskFieldExceededMaxLength::class) {
            taskService.insertTask(
                RequestContext(user),
                task.taskDetails.copy(description = generateRandomString(1001))
            )
        }

        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.TaskFieldExceededMaxLength::class) {
            taskService.updateTask(
                RequestContext(user),
                1,
                task.taskDetails.copy(description = generateRandomString(1001))
            )
        }
    }

    @Test
    fun `test create or update task with invalid owner`() {
        every { usersService.getUserByUserName("admin") } returns User("admin", "admin", "admin@email.com", Role.ADMIN, null)
        every { usersService.getUserById(7) } returns null

        assertFailsWith(Exceptions.TaskOwnerDoesntExist::class) {
            taskService.insertTask(
                RequestContext(user),
                task.taskDetails.copy(owner = 7)
            )
        }

        every {tasksRepository.getTaskById(1)} returns task
        assertFailsWith(Exceptions.TaskOwnerDoesntExist::class) {
            taskService.updateTask(
                RequestContext(user),
                1,
                task.taskDetails.copy(owner = 7)
            )
        }
    }

    private fun generateRandomString(length: Int): String {
        val random = Random.Default
        val charArray = CharArray(length) {
            random.nextInt(32, 127).toChar()
        }
        return String(charArray)
    }
}