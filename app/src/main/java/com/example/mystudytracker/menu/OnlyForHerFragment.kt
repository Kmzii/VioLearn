package com.example.mystudytracker.menu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R
import com.example.mystudytracker.databinding.FragmentOnlyForHerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class OnlyForHerFragment : Fragment() {

    private lateinit var parentRecyclerView: RecyclerView
    private lateinit var binding: FragmentOnlyForHerBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOnlyForHerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet: View = binding.bottomSheet
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        parentRecyclerView = binding.parentRecyclerView
        parentRecyclerView.setHasFixedSize(true)
        parentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        bottomSheetBehavior.apply {
            peekHeight = 200
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = false // This prevents the bottom sheet from completely hiding
        }

        val imageButton: View = binding.scrollButton
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