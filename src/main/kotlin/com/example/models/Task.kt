package com.example.models

import kotlinx.serialization.Serializable
import java.util.*

enum class TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class TaskSeverity {
    LOW,
    HIGH,
    URGENT
}

@Serializable
data class Task(
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val severity: TaskSeverity,
    val owner: String?
) {
    val taskId: String = UUID.randomUUID().toString()


}