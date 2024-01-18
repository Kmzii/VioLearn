package com.example.mystudytracker.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mystudytracker.R
import com.example.mystudytracker.databinding.FragmentOnlyForHerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OnlyForHerFragment : Fragment() {
    private lateinit var binding: FragmentOnlyForHerBinding
    private var currentDateCalendar = Calendar.getInstance()

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

        val items = createDummyData() // Replace this with your actual data
        val adapter = ExpandableAdapter(items)
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }
    private fun createDummyData(): List<ExpandableItem> {
        val databaseReference = FirebaseDatabase.getInstance().reference

        val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()

        val items = listOf(
            ExpandableItem(
                "Chocolates",
                listOf(
                    Pair("Dairy Milk", R.drawable.dairy_milk_image),
                    Pair("Munch", R.drawable.munch_image),
                    Pair("Kit Kat", R.drawable.kit_kat_image),
                    Pair("Fuse", R.drawable.fuse_image),
                    Pair("Crispello", R.drawable.crispello_image)
                ),
                mutableMapOf(), // Initialize an empty map for sub-option counts
                false
            ),
            ExpandableItem(
                "Chips",
                listOf(
                    Pair("Lays", R.drawable.lays_image),
                    Pair("Bingo", R.drawable.bingo_image),
                    Pair("Kurkure", R.drawable.kurkure_image)
                    ),
                mutableMapOf(), // Initialize an empty map for sub-option counts
                false
            ),
            ExpandableItem(
                "Food",
                listOf(
                    Pair("Momos", R.drawable.momos_image),
                    Pair("Burger", R.drawable.burger_image),
                    Pair("Rolls", R.drawable.rolls_image)
                ),
                mutableMapOf(), // Initialize an empty map for sub-option counts
                false
            ),
            ExpandableItem(
                "Items",
                listOf(
                    Pair("Scrunchies", R.drawable.scrunchies_image),
                    Pair("Nail Paint", R.drawable.nail_paint_image),
                ),
                mutableMapOf(), // Initialize an empty map for sub-option counts
                false
            ),
        )

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)
        val formattedDates = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

        // Fetch counts for each item from Firebase and update the subOptionCounts
        for (item in items) {
            val category = item.category

            for (subItemPair in item.subOptions) {
                val subItemName = subItemPair.first
                val uidPath = "Users/$uid/mySpace/${formattedDates}/$subItemName"

                databaseReference.child(uidPath).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val count = snapshot.getValue(Int::class.java) ?: 0
                        item.subOptionCounts[subItemName] = count

                        // Log the fetched data
                        Log.d("FirebaseData", "Item: $subItemName, Count: $count")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                        Log.e("FirebaseData", "Error fetching data: ${error.message}")
                    }
                })
            }
        }
        return items
    }

    private fun getStartOfWeek(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return cal.time
    }

    private fun getEndOfWeek(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek + 6)
        return cal.time
    }
}