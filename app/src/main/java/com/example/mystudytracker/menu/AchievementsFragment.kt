package com.example.mystudytracker.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.mystudytracker.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resumeWithException

class AchievementsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementsAdapter
    private lateinit var syncButton: ImageButton
    private lateinit var lottieAnimationView: LottieAnimationView
    private var loadedItemsCount = 10
    private var fetchJob: Job? = null // Declare a Job variable to keep track of the coroutine job


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_achievements, container, false)

        // Initialize RecyclerView and its adapter
        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = AchievementsAdapter()

        // Set the RecyclerView adapter
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton: View = requireView().findViewById(R.id.back_button)
        syncButton = requireView().findViewById(R.id.syncButton)
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_achievementsFragment_to_homeFragment)
        }

        syncButton.setOnClickListener {
            syncButton.isEnabled = false

            // Load additional items when the sync button is clicked
            adapter.clearAchievements()
            loadedItemsCount += 10
            loadItems()
        }

        loadItems()
    }

    private fun loadItems() {
        syncButton.isEnabled = false

        // Get the current date
        var currentDate = LocalDate.now()

        // Counter to keep track of the items loaded
        var itemsLoadedCounter = 0

        var dailyCompletions = mutableListOf<Pair<String, String>>() // Pair of date and achievement message

        fetchJob = viewLifecycleOwner.lifecycleScope.launch {
            lottieAnimationView.visibility = View.VISIBLE // Show Lottie animation

            val jobList = mutableListOf<Job>()

            while (itemsLoadedCounter < loadedItemsCount) {
                // Get the day name
                val dayName =
                    currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

                // Format the date
                val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

                val weekStartDate = currentDate.with(DayOfWeek.MONDAY)
                val weekEndDate = weekStartDate.plusDays(6)

                // Format the week start and end dates
                val formattedWeekDates =
                    "${weekStartDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))} to " +
                            weekEndDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

                val dayPath =
                    "Users/${FirebaseAuth.getInstance().currentUser?.uid}/Weeks/dailyCompletion/Week - $formattedWeekDates/$dayName - $formattedDate/dailyCompletionPercentage"

                // Add coroutine jobs to the list
                jobList.add(fetchDailyCompletionAsync(dayPath, dailyCompletions))

                itemsLoadedCounter++
                currentDate = currentDate.minusDays(1)

            }

            // Wait for all coroutine jobs to complete
            jobList.joinAll()

            //Sort
            dailyCompletions.sortWith(compareByDescending { LocalDate.parse(it.first, DateTimeFormatter.ofPattern("dd MMM yyyy")) })


            // Process results from dailyCompletions list
            for ((date, achievementMessage) in dailyCompletions) {
                // Display achievement only if completion is 100%
                if (achievementMessage.isNotEmpty()) {
                    // Add the achievement to the adapter
                    adapter.addAchievement("$date: $achievementMessage")
                }
            }

            adapter.notifyDataSetChanged()
            syncButton.isEnabled = true
            lottieAnimationView.visibility = View.GONE // Hide Lottie animation
        }
    }

    // Function to fetch completion percentage asynchronously using coroutines
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchDailyCompletionAsync(dayPath: String, resultContainer: MutableList<Pair<String, String>>): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val databaseRef = FirebaseDatabase.getInstance().getReference(dayPath)
                val completionPercentage = suspendCancellableCoroutine<Int> { continuation ->
                    databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val percentage = snapshot.getValue(Int::class.java) ?: 0
                            continuation.resume(percentage) {
                                // Handle cancellation if needed
                                Log.e("fetchDailyCompletionAsync", "Fetch operation cancelled.")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            continuation.resumeWithException(error.toException())
                        }
                    })
                }

                // If completion is 100%, add the date and achievement message to the result list
                if (completionPercentage == 100) {
                    val date = dayPath.substringAfterLast(" - ").substringBefore("/")
                    val achievementMessage = "Yay! You have completed all tasks for this day."
                    resultContainer.add(Pair(date, achievementMessage))
                }
            } catch (e: Exception) {
                Log.e("fetchDailyCompletionAsync", "Error: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Cancel the coroutine job when the view is destroyed
        fetchJob?.cancel()
    }
}