package com.example.mystudytracker.todolist

interface TaskEditListener {
    fun onTaskEdited(
        taskId: String, // Identifier for the task
        taskType: String,
        taskName: String,
        startTime: String,
        endTime: String,
        isCompleted: Boolean
    )
}
