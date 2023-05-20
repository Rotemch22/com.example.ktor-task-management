import com.example.Task
import com.example.TaskStatus
import com.example.TaskSeverity
import com.example.User
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TaskTest {
    @Test
    fun `test Task data class getters`() {
        // Create a sample user
        val manager = User("Manager Doe", null)
        val user = User("John Doe", manager)

        // Create a sample task
        val task = Task(
            "Sample Task",
            "This is a sample task",
            LocalDateTime.now(),
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            user
        )

        // Test getters
        assertEquals("Sample Task", task.title)
        assertEquals("This is a sample task", task.description)
        assertEquals(TaskStatus.NOT_STARTED, task.status)
        assertEquals(TaskSeverity.LOW, task.Severity)
        assertEquals(user, task.owner)
    }

    @Test
    fun `test Task data class toString`() {
        // Create a sample user
        val manager = User("Manager Doe", null)
        val user = User("John Doe", manager)

        // Create a sample task
        val task = Task(
            "Sample Task",
            "This is a sample task",
            LocalDateTime.now(),
            TaskStatus.NOT_STARTED,
            TaskSeverity.LOW,
            user
        )

        // Test toString() representation
        val expectedToString = "Task(title=Sample Task, description=This is a sample task, " +
                "dueDate=${task.dueDate}, status=NOT_STARTED, Severity=LOW, owner=$user)"
        assertEquals(expectedToString, task.toString())
    }
}
