package com.example.models


data class TasksQueryRequest(
    val status: TaskStatus? = null,
    val severity: TaskSeverity? = null,
    val owner: String? = null,
    val order: TaskSortOrder? = null
)

enum class TaskSortOrder {
    ASC, DESC
}
