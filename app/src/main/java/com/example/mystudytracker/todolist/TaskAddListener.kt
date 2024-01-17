package com.example.mystudytracker.todolist

interface TaskAddListener {
    fun onTaskAdded(
        taskType: String,
        taskName: String,
        startTime: String,
        endTime: String,
        isCompleted: Boolean
    )
}