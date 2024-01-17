package com.example.mystudytracker.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mystudytracker.databinding.FragmentForgotPassBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPassFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentForgotPassBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForgotPassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        resetPass()
    }
    private fun init(view: View){
        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
    }

    private fun resetPass(){
        binding.resetButton.setOnClickListener{
            val email = binding.emailEt.text.toString().trim()
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(context,"Please check your Email",Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{
                    Toast.makeText(context,it.toString(), Toast.LENGTH_SHORT).show()
                }
        }
    }
}