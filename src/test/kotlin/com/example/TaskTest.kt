import com.example.models.Task
import com.example.models.TaskStatus
import com.example.models.TaskSeverity
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskTest {
    @Test
    fun `test Task data class getters`() {
        // Create a sample task
        val task = Task(
            "Sample Task",
            "This is a sample task",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            1,
            LocalDateTime.parse("2023-08-30T18:43:00")
        )

        // Test getters
        assertEquals("Sample Task", task.title)
        assertEquals("This is a sample task", task.description)
        assertEquals(TaskStatus.NOT_STARTED, task.status)
        assertEquals(TaskSeverity.LOW, task.severity)
        assertEquals(1, task.owner)
        assertEquals(LocalDateTime.parse("2023-08-30T18:43:00"), task.dueDate)
    }

    @Test
    fun `test Task data class toString`() {
        // Create a sample task
        val task = Task(
            "Sample Task",
            "This is a sample task",
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            1,
            LocalDateTime.parse("2023-08-30T18:43:00")
        )

        // Test toString() representation
        val expectedToString = "Task(title=Sample Task, description=This is a sample task, status=NOT_STARTED, severity=LOW, owner=1, dueDate=2023-08-30T18:43, taskId=0)"
        assertEquals(expectedToString, task.toString())
    }
}
