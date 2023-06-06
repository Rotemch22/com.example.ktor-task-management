package com.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

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
data class TaskRecord(
    val taskDetails: TaskDetails,
    val taskId: Int
)

@Serializable
data class TaskDetails(
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val severity: TaskSeverity,
    val owner: Int? = null,
    val dueDate: LocalDateTime
)

@Serializable
data class TaskRevision(
    val task : TaskRecord,
    val revision : Int,
    val modifiedBy : Int,
    val modifiedDate: LocalDateTime,
    val updateType: UpdateType
)


enum class UpdateType {
    CREATE,
    UPDATE,
    DELETE
}