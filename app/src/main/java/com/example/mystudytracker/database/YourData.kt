package com.example.mystudytracker.database

data class YourData(
    val dayName: String = "",
    val date: String = "",
    val weekDates: String = "",
    var dailyCompletionPercentage: Int // Include the completion percentage field

)
