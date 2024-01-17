package com.example.mystudytracker.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseUtils {

    // ... other Firebase initialization code

    fun addOrUpdateUserData(userData: UserData) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid // Retrieve the UID of the current user
        val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(uid ?: "")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usersRef.updateChildren(userData.toMap())
                        .addOnSuccessListener {
                            Log.d("FirebaseUtils", "User data updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseUtils", "Error updating user data: $e")
                        }
                } else {
                    usersRef.setValue(userData.toMap())
                        .addOnSuccessListener {
                            Log.d("FirebaseUtils", "New user data added successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseUtils", "Error adding new user data: $e")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseUtils", "Database read error: $error")
            }
        })
    }
}
