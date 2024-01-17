package com.example.mystudytracker.menu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.mystudytracker.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class OnlyForHerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_only_for_her, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet: View = requireView().findViewById(R.id.bottomSheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)


        bottomSheetBehavior.apply {
            peekHeight = 200
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = false // This prevents the bottom sheet from completely hiding
        }

        val imageButton: View = requireView().findViewById(R.id.scrollButton)
        imageButton.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        val backButton: View = requireView().findViewById(R.id.back_button)

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_onlyForHerFragment_to_homeFragment)
        }
    }
}