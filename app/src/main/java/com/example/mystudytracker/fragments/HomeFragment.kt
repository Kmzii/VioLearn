package com.example.mystudytracker.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.mystudytracker.LogoutDialogFragment
import com.example.mystudytracker.MainAdapter
import com.example.mystudytracker.R
import com.example.mystudytracker.UserProfileDialogFragment
import com.example.mystudytracker.UserProfileUpdateListener
import com.example.mystudytracker.database.YourData
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resumeWithException

class HomeFragment : Fragment(), UserProfileUpdateListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var burgerMenu: ImageView
    private lateinit var navigationView: NavigationView
    private lateinit var weekDatesTextView: TextView
    private lateinit var weekTitleTextView: TextView
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var adapter: MainAdapter // Add adapter variable
    private var currentDateCalendar = Calendar.getInstance()
    private var backPressedOnce = false
    private var weekDates: String = "" // Declare weekDates variable
    private lateinit var streakTextView: TextView
    private lateinit var userProfileImageView: ImageView
    private var asyncOperationCounter = 0
    private lateinit var lottieAnimationView: LottieAnimationView
    private var isLoadingImage = false
    private lateinit var checkInternet: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fadeInAnimation: AlphaAnimation
    private var isFadeInAnimationInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Fetch user's display name from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)

        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)

        setClickListeners()

        initializeBackPressedCallback()

        navigationCheck()

        currentUser?.let { user ->
            val displayName = user.displayName
            displayName?.let {
                val firstName = getFirstName(displayName)
                nameTextView.text = firstName
            }
        }
        // Initialize RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter and set it to the RecyclerView
        adapter = MainAdapter(mutableListOf()) // Initialize adapter with an empty list
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { dayName: String, date: String, weekDates: String ->
            openToDoListFragment(dayName, date, weekDates)
        }

        val startTime = System.currentTimeMillis()

        updateRecyclerViewDates(true) // Update RecyclerView dates

        val endTime = System.currentTimeMillis()
        val elapsedTimeInSeconds = (endTime - startTime) / 1000.0

        Log.d("DataRetrieval", "updateRecyclerViewDates took $elapsedTimeInSeconds seconds")

        // Update RecyclerView with initial dates when the fragment is created
        updateWeekDatesText(weekDatesTextView, weekTitleTextView)

        dailyStreak()

        loadUserProfileImage()
    }

    private fun initializeViews(view: View) {
        drawerLayout = view.findViewById(R.id.drawerLayout)
        burgerMenu = view.findViewById(R.id.burgerMenu)
        navigationView = view.findViewById(R.id.navigationView)
        weekDatesTextView = view.findViewById(R.id.weekDatesTextView)
        weekTitleTextView = view.findViewById(R.id.weekTitleTextView)
        previousButton = view.findViewById(R.id.previousButton)
        nextButton = view.findViewById(R.id.nextButton)
        streakTextView = view.findViewById(R.id.streakTextView)
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)
        checkInternet = view.findViewById(R.id.checkInternet)

        navigationView.itemIconTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple))
        updateWeekDatesText(weekDatesTextView, weekTitleTextView)
    }

    private fun setClickListeners() {
        previousButton.setOnClickListener {
            currentDateCalendar.add(Calendar.DAY_OF_YEAR, -7)
            updateWeekDatesText(weekDatesTextView, weekTitleTextView)
            updateRecyclerViewDates(false) // Update RecyclerView dates
        }

        nextButton.setOnClickListener {
            currentDateCalendar.add(Calendar.DAY_OF_YEAR, 7)
            updateWeekDatesText(weekDatesTextView, weekTitleTextView)
            updateRecyclerViewDates(false) // Update RecyclerView dates
        }

        burgerMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem)
        }

        userProfileImageView.isClickable = false

        // Set an OnClickListener to open the dialog
        userProfileImageView.setOnClickListener {
            if (!isLoadingImage) { // Add a check for image loading
                showUserProfileDialogFragment()
            }
        }
    }

    private fun initializeBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun navigationCheck() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val desiredUsername = "Neha Roy"
        val desiredEmail = "nehasignin5@gmail.com"

        currentUser?.uid?.let { userId ->
            val cacheKey = "menu_$userId"
            val sharedPreferences =
                requireContext().getSharedPreferences("MenuVisibilityCache", Context.MODE_PRIVATE)

            // Check if the cache is present or not
            if (sharedPreferences.contains(cacheKey)) {
                // Cache is present, use the cached value without querying the database
                val isMenuVisible = sharedPreferences.getBoolean(cacheKey, false)
                Log.d("MenuVisibility", "Cached visibility for user $userId: $isMenuVisible")
                navigationView.menu.findItem(R.id.menu_only_for_her).isVisible = isMenuVisible
            } else {
                // Cache is not present, fetch from the database
                val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$userId")
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val storedUsername = snapshot.child("userName").getValue(String::class.java)
                        val storedEmail = snapshot.child("userEmail").getValue(String::class.java)

                        // Check if the stored username and email match the criteria
                        if (storedUsername == desiredUsername && storedEmail == desiredEmail) {
                            // User is authorized, set the menu visibility to true and update the cache
                            sharedPreferences.edit().putBoolean(cacheKey, true).apply()
                            Log.d(
                                "MenuVisibility",
                                "User $userId is authorized. Menu is now visible."
                            )
                        } else {
                            // User is not authorized, hide the menu item and update the cache
                            navigationView.menu.findItem(R.id.menu_only_for_her).isVisible = false
                            sharedPreferences.edit().putBoolean(cacheKey, false).apply()
                            Log.d(
                                "MenuVisibility",
                                "User $userId is not authorized. Menu is hidden."
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("DataRetrieval", "Error retrieving data: ${error.message}")
                    }
                })
            }
        }
    }

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_home -> {
                handleHomeMenuClick()
                return false
            }

            R.id.menu_daily -> {
                handleDailyMenuClick()
                return false
            }

            R.id.menu_weekly -> {
                handleWeeklyMenuClick()
                return false
            }

            R.id.menu_sleep -> {
                handleSleepMenuClick()
                return false
            }

            R.id.menu_achievements -> {
                handleAchievementsMenuClick()
                return false
            }

            R.id.menu_only_for_her -> {
                handleOnlyForHerMenuClick()
                return false
            }

            R.id.menu_logout -> {
                return handleLogoutMenuClick()
            }
        }
        return false
    }

    private fun handleHomeMenuClick() {
        Log.d("TAG", "Home Clicked")
        closeDrawer()
    }

    private fun handleDailyMenuClick() {
        findNavController().navigate(R.id.action_homeFragment_to_dailyFragment)
        closeDrawer()
    }

    private fun handleWeeklyMenuClick() {
        findNavController().navigate(R.id.action_homeFragment_to_weeklyFragment)
        closeDrawer()
    }

    private fun handleSleepMenuClick() {
        findNavController().navigate(R.id.action_homeFragment_to_sleepFragment)
        closeDrawer()
    }

    private fun handleAchievementsMenuClick() {
        findNavController().navigate(R.id.action_homeFragment_to_achievementsFragment)
        closeDrawer()
    }

    private fun handleOnlyForHerMenuClick() {
        findNavController().navigate(R.id.action_homeFragment_to_onlyForHerFragment)
        closeDrawer()
    }

    private fun handleLogoutMenuClick(): Boolean {
        // Show the LogoutDialogFragment
        val logoutDialogFragment = LogoutDialogFragment()
        logoutDialogFragment.show(requireActivity().supportFragmentManager, "LogoutDialogFragment")

        return false
    }

    private fun handleBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            handleBackButtonPress()
        }
    }

    private fun handleBackButtonPress() {
        if (backPressedOnce) {
            requireActivity().finish()
        } else {
            backPressedOnce = true
            Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                backPressedOnce = false
            }, 2000)
        }
    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun showUserProfileDialogFragment() {
        val userProfileDialogFragment = UserProfileDialogFragment().apply {
            setUserProfileUpdateListener(this@HomeFragment)
        }
        userProfileDialogFragment.show(
            requireActivity().supportFragmentManager,
            "UserProfileDialogFragment"
        )
    }

    override fun onUserProfileUpdated() {
        // Reload the user profile image in the home fragment
        loadUserProfileImage()
    }

    private fun loadUserProfileImage() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            isLoadingImage = true // Set the flag to indicate that image loading is in progress
            fetchImageFromFirebaseStorage(userId)
        } else {
            Log.e("UserProfileDialog", "User ID is null. Unable to load user profile image.")
        }
    }

    private fun fetchImageFromFirebaseStorage(userId: String) {
        val storageRef = FirebaseStorage.getInstance().getReference("profile_images/$userId")

        storageRef.downloadUrl.addOnSuccessListener { uri ->
            // Load the image using Glide
            Log.d("UserProfileDialog", "Loading image from Firebase Storage.")

            Glide.with(this)
                .load(uri)
                .error(R.drawable.user_person_profile_block_account_circle)
                .into(userProfileImageView)

            // Save the fetched image to app's private storage
            saveFetchedImageToStorage(uri)

            isLoadingImage = false

        }.addOnFailureListener { e ->
            // Handle failure, load default image or handle it based on your app's logic
            Log.e("UserProfileDialog", "Error fetching image from Firebase Storage: ${e.message}")
            Glide.with(this)
                .load(R.drawable.user_person_profile_block_account_circle)
                .into(userProfileImageView)

            isLoadingImage = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveFetchedImageToStorage(imageUri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Glide.get(requireContext()).clearDiskCache()

                val userId = FirebaseAuth.getInstance().currentUser?.uid

                deleteProfileImageFiles()

                // Log the URL
                Log.d("UserProfileDialog", "Image URL: $imageUri")

                // Download the image from Firebase Storage
                val inputStream = URL(imageUri.toString()).openStream()

                // Save the image to app's private storage with timestamp
                val timestamp = System.currentTimeMillis()
                val privateImagePath =
                    File(requireContext().filesDir, "profile_image_${userId}_$timestamp.jpg")
                inputStream.use { input ->
                    privateImagePath.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // Update the image path in SharedPreferences
                saveImagePathToLocal(privateImagePath)

            } catch (e: Exception) {
                // Handle exceptions
                Log.e("UserProfileDialog", "Error saving fetched image to storage: ${e.message}")
            }
        }
    }

    private fun deleteProfileImageFiles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val filesDir = requireContext().filesDir
            val fileList = filesDir.listFiles { _, name ->
                name.startsWith("profile_image_$userId")
            }

            fileList?.forEach { file ->
                val deleted = file.delete()
                if (deleted) {
                    Log.d("UserProfileDialog", "File deleted: ${file.name}")
                } else {
                    Log.e("UserProfileDialog", "Failed to delete file: ${file.name}")
                }
            }
        }
    }


    private fun saveImagePathToLocal(imagePath: File) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Use SharedPreferences to store the imagePath
            val sharedPreferences =
                requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("userProfileImagePath_$userId", imagePath.absolutePath)
            editor.apply()
            showFilesInDirectories()
        }
    }

    private fun updateWeekDatesText(textView: TextView, titleTextView: TextView) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)
        val todayCalendar = Calendar.getInstance()
        val isCurrentWeek = isCurrentWeek(todayCalendar, currentDateCalendar)

        val titleText = when {
            isCurrentWeek -> "Current Week"
            currentDateCalendar.before(todayCalendar) -> "Previous Week"
            else -> "Upcoming Week"
        }

        val formattedDates = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        textView.text = formattedDates
        titleTextView.text = titleText
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

    private fun isCurrentWeek(todayCalendar: Calendar, currentCalendar: Calendar): Boolean {
        val todayWeek = todayCalendar[Calendar.WEEK_OF_YEAR]
        val currentWeek = currentCalendar[Calendar.WEEK_OF_YEAR]
        val todayYear = todayCalendar[Calendar.YEAR]
        val currentYear = currentCalendar[Calendar.YEAR]
        return todayWeek == currentWeek && todayYear == currentYear
    }

    private fun getFirstName(fullName: String): String {
        return fullName.split(" ").firstOrNull() ?: ""
    }

    private fun updateRecyclerViewDates(animation: Boolean) {
        if (animation) {
            lottieAnimationView.visibility = View.VISIBLE // Show Lottie animation
            handler.postDelayed({
                if (lottieAnimationView.isAnimating) {
                    fadeInCheckInternet()
                }
            }, 7000)
        }
        asyncOperationCounter = 0

        val startTime = System.currentTimeMillis()

        Log.d("DataRetrieval", "Function Called")

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())

        val startDate = getStartOfWeek(currentDateCalendar)
        val endDate = getEndOfWeek(currentDateCalendar)

        weekDates = "${sdf.format(startDate)} to ${sdf.format(endDate)}"

        val updatedDataList = mutableListOf<YourData>()

        val calendar = Calendar.getInstance()
        calendar.time = startDate // Start from the current week's start date

        for (i in 0 until 7) {
            val formattedDate = sdf.format(calendar.time)
            val dayName = sdfDayOfWeek.format(calendar.time)
            val dayPath =
                "Users/${FirebaseAuth.getInstance().currentUser?.uid}/Weeks/dailyCompletion/Week - $weekDates/$dayName - $formattedDate/dailyCompletionPercentage"

            fetchDailyCompletionPercentage(dayPath) { completionPercentage ->
                val yourData = YourData(dayName, formattedDate, weekDates, completionPercentage)
                updatedDataList.add(yourData)

                if (updatedDataList.size == 7) {
                    // Define the order of days
                    val orderedDays = listOf(
                        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
                    )

                    // Custom comparator to sort the list based on the ordered days
                    val dayComparator = Comparator<YourData> { data1, data2 ->
                        val index1 = orderedDays.indexOf(data1.dayName)
                        val index2 = orderedDays.indexOf(data2.dayName)
                        index1.compareTo(index2)
                    }

                    // Sort the updatedDataList using the custom comparator
                    updatedDataList.sortWith(dayComparator)

                    adapter.updateData(updatedDataList)

                }

                asyncOperationCounter++

                // Check if all asynchronous operations are complete
                if (asyncOperationCounter == 7 && !isFadeInAnimationInProgress) {
                    // All operations are complete, log the final timestamp
                    val endTime = System.currentTimeMillis()
                    val elapsedTimeInSeconds = (endTime - startTime) / 1000.0
                    Log.d(
                        "DataRetrieval",
                        "All data retrieval completed in $elapsedTimeInSeconds seconds"
                    )
                    checkInternet.visibility = View.GONE
                    lottieAnimationView.visibility = View.GONE // Hide Lottie animation
                }

            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun fadeInCheckInternet() {
        fadeInAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 300 // Set the duration of the fade-in effect (in milliseconds)
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    checkInternet.visibility = View.VISIBLE
                    isFadeInAnimationInProgress = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    isFadeInAnimationInProgress = false
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Optional: Handle animation repeat
                }
            })
        }

        // Start the fade-in animation on the checkInternet view
        checkInternet.startAnimation(fadeInAnimation)
    }

    private fun fetchDailyCompletionPercentage(dayPath: String, completionCallback: (Int) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference(dayPath)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val completionPercentage = snapshot.getValue(Int::class.java) ?: 0
                Log.d(
                    "DataRetrieval",
                    "Path: $dayPath, Completion Percentage: $completionPercentage"
                )
                completionCallback.invoke(completionPercentage)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRetrieval", "Error retrieving data: ${error.message}")
                completionCallback.invoke(0) // Return default value in case of error
            }
        })
    }

    private fun openToDoListFragment(dayName: String, date: String, weekDates: String) {
        val bundle = Bundle()
        bundle.putString("dayName", dayName)
        bundle.putString("date", date)
        bundle.putString("weekDates", weekDates) // Add weekDates to the bundle

        // Pass the bundle with arguments to the ToDoListFragment via NavController
        findNavController().navigate(R.id.action_homeFragment_to_toDoListFragment, bundle)
    }

    private fun dailyStreak() {
        // Get the current date
        var currentDate = LocalDate.now().minusDays(1)

        // Counter to keep track of the streak
        var streakCounter = 0

        var shouldBreakStreak = false

        var dailyCompletion: Int

        CoroutineScope(Dispatchers.Main).launch {

            do {
                // Get the day name
                val dayName =
                    currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

                // Format the date
                val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

                // Get the corresponding week's start and end dates
                val weekStartDate = currentDate.with(DayOfWeek.MONDAY)
                val weekEndDate = weekStartDate.plusDays(6)

                // Format the week start and end dates
                val formattedWeekDates =
                    "${weekStartDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))} to " +
                            weekEndDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

                val dayPath =
                    "Users/${FirebaseAuth.getInstance().currentUser?.uid}/Weeks/dailyCompletion/Week - $formattedWeekDates/$dayName - $formattedDate/dailyCompletionPercentage"

                // Fetch the completion percentage for the day using a coroutine
                val completionPercentage = fetchStreakDailyCompletionAsync(dayPath)

                dailyCompletion = completionPercentage

                Log.d("StreakPath", "$dayPath | $dailyCompletion")

                println(completionPercentage)

                // Check if the completion is 100%
                if (completionPercentage == 100) {
                    streakCounter++ // Increment the streak counter
                } else {
                    // If completion is not 100% or streak should be broken, set the flag
                    shouldBreakStreak = true
                }

                Log.d("StreakLoop", "Iteration: $formattedDate | dailyCompletion: $dailyCompletion")

                currentDate = currentDate.minusDays(1)

            } while (dailyCompletion > 0 && !shouldBreakStreak)

            // Run the second part of the logic in a different dispatcher
            withContext(Dispatchers.IO) {
                // Check the completion percentage for today
                val todayDate = LocalDate.now()

                val todayName =
                    todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

                val todayPath =
                    "Users/${FirebaseAuth.getInstance().currentUser?.uid}/Weeks/dailyCompletion/Week - $weekDates/$todayName - ${
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    }/dailyCompletionPercentage"

                // Fetch the completion percentage for today
                val todayCompletionPercentage = fetchStreakDailyCompletionAsync(todayPath)

                Log.d("Streak", "It Ran $todayPath $todayCompletionPercentage")

                // Check if today's completion is 100%
                if (todayCompletionPercentage == 100) {
                    streakCounter++ // Increment the streak counter
                }
                Log.d("StreakCompletion", "Streak Count: $streakCounter")
            }
            updateStreakTextView(streakCounter)
        }
    }

    // Modified function to fetch completion percentage asynchronously using coroutines
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchStreakDailyCompletionAsync(dayPath: String): Int {
        return suspendCancellableCoroutine { continuation ->
            val databaseRef = FirebaseDatabase.getInstance().getReference(dayPath)

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completionPercentage = snapshot.getValue(Int::class.java) ?: 0
                    continuation.resume(completionPercentage) {
                        // Handle cancellation if needed
                        Log.e("DataRetrieval", "Fetch operation cancelled.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DataRetrieval", "Error retrieving data: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    private fun updateStreakTextView(streakCounter: Int) {
        // Update streak count in TextView based on conditions
        val streakText = when (streakCounter) {
            0 -> "0 day streak :("
            1 -> "$streakCounter day streak 🔥"
            else -> "$streakCounter days streak 🔥"
        }

        streakTextView.text = streakText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the callback to avoid memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    private fun showFilesInDirectories() {
        // Show files in cache directory
        val cacheDir = requireContext().cacheDir
        val cacheFileList = cacheDir.listFiles()

        if (cacheFileList.isNullOrEmpty()) {
            Log.d("UserProfileDialog", "Cache directory is empty.")
        } else {
            Log.d("UserProfileDialog", "Files in cache directory:")
            var totalCacheSize = 0L

            for (file in cacheFileList) {
                val fileSize = file.length()
                totalCacheSize += fileSize
                Log.d("UserProfileDialog", "${file.name} - Size: $fileSize bytes")
            }

            Log.d("UserProfileDialog", "Total Cache Size: $totalCacheSize bytes")
        }

        // Show files in files directory
        val filesDir = requireContext().filesDir
        val filesFileList = filesDir.listFiles()

        if (filesFileList.isNullOrEmpty()) {
            Log.d("UserProfileDialog", "Files directory is empty.")
        } else {
            Log.d("UserProfileDialog", "Files in files directory:")
            for (file in filesFileList) {
                Log.d("UserProfileDialog", "${file.name} - Size: ${file.length()} bytes")
            }
        }
    }


}
