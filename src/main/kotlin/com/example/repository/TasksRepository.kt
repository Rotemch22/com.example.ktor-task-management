package com.example.repository

import com.example.models.Task

class TasksRepository {
    val tasks : MutableList<Task> = mutableListOf()

    fun add(task: Task) {
        tasks.add(task)
    }


}