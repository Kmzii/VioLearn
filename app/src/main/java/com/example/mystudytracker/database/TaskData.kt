package com.example.mystudytracker.database

data class TaskData(
    var taskId: String = "",
    var taskType: String = "",
    var taskName: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var isCompleted: Boolean = false
)


