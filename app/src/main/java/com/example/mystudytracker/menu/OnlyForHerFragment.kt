package com.example.mystudytracker.menu

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
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
    private lateinit var iWouldLikeRecycler: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var progressTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOnlyForHerBinding.inflate(inflater, container, false)
        iWouldLikeRecycler = binding.iWouldLikeRecycler
        iWouldLikeRecycler.layoutManager = LinearLayoutManager(requireContext())

        val adapter = IWouldLikeAdapter(emptyList()) // Initialize with an empty list

        iWouldLikeRecycler.adapter = adapter // Set the adapter immediately

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressTitle = binding.progressTitle

        val lottieAnimationView: LottieAnimationView = binding.catAnimation
        lottieAnimationView.setAnimation("my_space_cat.json")
        lottieAnimationView.repeatCount = LottieDrawable.INFINITE
        lottieAnimationView.playAnimation()

        val bottomSheet: View = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

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

        // Register callback for onBackPressed
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)


        val items = createDummyData() // Replace this with your actual data
        val adapter = ExpandableAdapter(items) { itemName, itemCount ->
            updateCountInFirebase(itemName, itemCount)
        }

        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
        fetchDailyCompletionForWeek()
    }

    private fun createDummyData(): List<ExpandableItem> {

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

        fetchFirebaseData(items)

        return items
    }

    private fun updateCountInFirebase(itemName: String, itemCount: Int) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)
        val formattedDates = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

        val uidPath = "Users/$uid/mySpace/${formattedDates}/$itemName"

        // Update the count in the Firebase Realtime Database
        databaseReference.child(uidPath).setValue(itemCount)
            .addOnSuccessListener {
                // After successfully updating the count, fetch updated data
                fetchFirebaseData(createDummyData())
            }
            .addOnFailureListener { e ->
                // Handle the failure to update the count
                Log.e("FirebaseData", "Error updating count: ${e.message}")
            }
    }

    private fun fetchFirebaseData(items: List<ExpandableItem>) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()

        val iWouldLikeItems = mutableListOf<IWouldLikeItem>()
        var fetchCount = 0

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)
        val formattedDates = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

        for (item in items) {
            for (subItemPair in item.subOptions) {
                val subItemName = subItemPair.first
                val uidPath = "Users/$uid/mySpace/${formattedDates}/$subItemName"

                databaseReference.child(uidPath)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val count = snapshot.getValue(Int::class.java) ?: 0
                            item.subOptionCounts[subItemName] = count

                            if (count > 0) {
                                val iWouldLikeItem =
                                    IWouldLikeItem(subItemName, count, subItemPair.second)
                                iWouldLikeItems.add(iWouldLikeItem)
                            }

                            fetchCount++
                            if (fetchCount == items.sumOf { it.subOptions.size }) {
                                updateIWouldLikeRecycler(iWouldLikeItems)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FirebaseData", "Error fetching data: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun updateIWouldLikeRecycler(data: List<IWouldLikeItem>) {

        val adapter = IWouldLikeAdapter(data)
        iWouldLikeRecycler.adapter = adapter
        // You may need to notify the adapter about the data change if required
        adapter.notifyDataSetChanged()
    }

    private fun fetchDailyCompletionForWeek() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())

        val calendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)
        val weekDates = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

        calendar.time = startDate

        var totalDailyCompletion = 0.0

        var count = 0

        // Iterate over each day of the week
        for (i in 1..7) {
            val formattedDate = sdf.format(calendar.time)
            val dayName = sdfDayOfWeek.format(calendar.time)

            // Path for daily completion
            val dayPath = "Users/${FirebaseAuth.getInstance().currentUser?.uid}/Weeks/dailyCompletion/Week - $weekDates/$dayName - $formattedDate/dailyCompletionPercentage"

            // Retrieve daily completion from Firebase
            val databaseReference = FirebaseDatabase.getInstance().reference
            databaseReference.child(dayPath).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyCompletion = snapshot.getValue(Double::class.java) ?: 0.0

                    Log.d("DailyCompletion", "Day: $dayPath, Completion: $dailyCompletion")

                    count ++

                    totalDailyCompletion += dailyCompletion

                    if(count == 7){
                        displayProgress(totalDailyCompletion)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseData", "Error fetching daily completion: ${error.message}")
                }
            })

            // Move to the next day
            calendar.add(Calendar.DAY_OF_WEEK, 1)
        }
    }

    private fun displayProgress(totalDailyCompletion: Double){
        val averageDailyCompletion = totalDailyCompletion/7.0

        Log.d("DailyCompletion","$averageDailyCompletion")
        // Update the ProgressBar
        val progressBar: ProgressBar = binding.progressBar
        progressBar.progress = averageDailyCompletion.toInt()

        val progressText = resources.getString(R.string.my_space_progress_title, averageDailyCompletion.toInt())
        val spannableString = SpannableString(progressText)
        val lastWordStartPosition = spannableString.lastIndexOf(' ') + 1

        // Apply text size and color to the last word
        spannableString.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.purple)), // Purple color
            lastWordStartPosition,
            progressText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        progressTitle.text = spannableString
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the callback when the view is destroyed
        backPressedCallback.remove()
    }

}