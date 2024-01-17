package com.example.mystudytracker.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mystudytracker.R
import com.example.mystudytracker.database.FirebaseUtils
import com.example.mystudytracker.database.UserData
import com.example.mystudytracker.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()

        // Callback to handle the back button press in the SignInFragment
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Delay closing the app by 2 seconds (2000 milliseconds)
                Handler(Looper.getMainLooper()).postDelayed({
                    requireActivity().finish()
                }, 500)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun init(view: View) {
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
    }

    private fun registerEvents() {
        binding.signUp.setOnClickListener {
            navControl.navigate(R.id.action_signInFragment_to_signUpFragment)
        }
        binding.reset.setOnClickListener {
            navControl.navigate(R.id.action_signInFragment_to_forgotPassFragment)
        }
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            // Retrieve user data
                            val userEmail = it.email
                            val userName = it.displayName // Assuming display name is available

                            // Create a UserData object with retrieved user data
                            val userData = UserData(userName ?: "", userEmail ?: "")

                            // Store UserData in Firebase Realtime Database
                            FirebaseUtils.addOrUpdateUserData(userData)

                            // Move to homeFragment after successful sign-in
                            Toast.makeText(context, "Signed in Successfully", Toast.LENGTH_SHORT)
                                .show()
                            navControl.navigate(R.id.action_signInFragment_to_homeFragment)
                        }
                    } else {
                        Toast.makeText(context, signInTask.exception?.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(context, "Enter all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
