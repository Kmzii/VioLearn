package com.example.mystudytracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class LogoutDialogFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_logout_dialog, container, false)

        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)

        // Set click listener for the logout button
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
            dismiss() // Close the dialog after logout
        }

        cancelButton.setOnClickListener{
            dismiss()
        }
        return view
    }

}