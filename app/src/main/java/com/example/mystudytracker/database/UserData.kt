package com.example.mystudytracker.database

data class UserData(
    val userName: String,
    val userEmail: String
) {
    // Function to convert UserData to a Map
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userName" to userName,
            "userEmail" to userEmail
            // Add other UserData properties here if needed
        )
    }
}
