package com.example.models

class TasksQueryRequest {

    data class TasksQueryRequest(
        val status: TaskStatus? = null,
        val severity: TaskSeverity? = null,
        val owner: String? = null,
        val order: TaskSortOrder? = null
    )

    enum class TaskSortOrder {
        ASC, DESC
    }
}