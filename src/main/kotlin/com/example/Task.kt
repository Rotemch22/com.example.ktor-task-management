package com.example

import java.time.LocalDateTime

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

data class Task(
    val title: String,
    val description: String?,
    val dueDate: LocalDateTime,
    val status: TaskStatus,
    val Severity: TaskSeverity,
    val owner: User?
) {
}