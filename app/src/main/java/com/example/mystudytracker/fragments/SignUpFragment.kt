package com.example.mystudytracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mystudytracker.R
import com.example.mystudytracker.database.FirebaseUtils
import com.example.mystudytracker.database.UserData
import com.example.mystudytracker.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class SignUpFragment : Fragment() {

    private lateinit var auth:FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()
    }

    private fun init(view: View){
        navControl = Navigation.findNavController(view)
        auth=FirebaseAuth.getInstance()
    }

    private fun registerEvents() {
        binding.button.setOnClickListener {
            val name = binding.nameEt.text.toString().trim()
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()
            val verifyPass = binding.rePassEt.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && verifyPass.isNotEmpty()) {
                if (pass == verifyPass) {
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()

                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        initializeUserData(user.uid, name, email)
                                    } else {
                                        showToast("Failed to update profile.")
                                        user.delete()
                                    }
                                }
                                // Debugging logs
                                Log.d("SignUpDebug", "Creating user account...")

                                // Check authentication status
                                if (auth.currentUser != null) {
                                    Log.d("SignUpDebug", "User authenticated successfully: ${auth.currentUser?.uid}")
                                } else {
                                    Log.d("SignUpDebug", "User authentication failed or user object is null.")
                                }

                                // Check the UID to be used for writing data
                                val userUID = auth.currentUser?.uid
                                Log.d("SignUpDebug", "User UID: $userUID")
                            } else {
                                showToast("Registration failed: ${task.exception?.message}")
                            }
                        }
                } else {
                    showToast("Passwords do not match")
                }
            } else {
                showToast("Enter all the fields")
            }
        }
    }

    private fun initializeUserData(userId: String, name: String, email: String) {
        val userReference = FirebaseDatabase.getInstance().getReference("Users")
        val userData = UserData(name, email)

        userReference.child(userId).setValue(userData)
            .addOnCompleteListener { databaseTask ->
                if (databaseTask.isSuccessful) {
                    // Store UserData in Firebase Realtime Database using FirebaseUtils
                    FirebaseUtils.addOrUpdateUserData(userData)
                    showToast("Registered Successfully")
                    navControl.navigate(R.id.action_signUpFragment_to_homeFragment)
                } else {
                    // Log the database write failure reason
                    Log.e("InitializeUserData", "Failed to create user data: ${databaseTask.exception?.message}")

                    // Log the user ID attempting to write data
                    Log.e("InitializeUserData", "User ID: $userId")

                    // Log the authenticated user's UID
                    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
                    Log.e("InitializeUserData", "Current User ID: $currentUserID")

                    // Handle the failure with showToast or further debugging/logging as needed
                    showToast("Failed to create user data: ${databaseTask.exception?.message}")
                    auth.currentUser?.delete() // This line deletes the user if data creation fails (Ensure this is the desired behavior)
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
