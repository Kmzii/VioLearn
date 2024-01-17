package com.example.mystudytracker.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.mystudytracker.R
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SleepFragment : Fragment() {

    private var dayName: String? = null
    private var date: String? = null
    private lateinit var dateSpinner: View
    private lateinit var dateSpinnerTextView: TextView
    private var selectedDateMillis: Long? = null
    private var currentDateMillis: Long? = null
    private var mondayDate: String? = null
    private var sundayDate: String? = null
    private var taskTypeDurations: MutableMap<String, Long> = mutableMapOf(
        "Sleep" to 0L,
    )

    private var isDatePickerClickable: Boolean = true
    private var idealTime: String = ""
    private var sleepTime: String = ""
    private lateinit var idealSleepSpinner: Spinner
    private var isInitialSetup: Boolean = true // Flag to track initial setup

    // Firebase
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        uid = auth.currentUser?.uid
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sleep, container, false)
        dateSpinner = view.findViewById(R.id.dateSpinner)
        dateSpinnerTextView = view.findViewById(R.id.dateSpinnerTextView)
        idealSleepSpinner = view.findViewById(R.id.idealSleepSpinner)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton: View = requireView().findViewById(R.id.back_button)

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_sleepFragment_to_homeFragment)
        }
        currentDateMillis = getCurrentDateInMillis()

        val currentMillis =
            currentDateMillis ?: return // Store the value of currentDateMillis in a local variable

        calculateWeekDates(currentMillis)

        val currentDate = getCurrentDate()
        val splitDate = currentDate.split(" - ")
        if (splitDate.size == 2) {
            dayName = splitDate[0]
            date = splitDate[1]
        }

        // Log the initial dayName and date
        Log.d("InitialValues", "DayName: $dayName, Date: $date")
        dateSpinnerTextView.text = currentDate

        dateSpinner.setOnClickListener {
            showDatePicker()
        }
        Log.d("WeekDates", "Monday: $mondayDate, Sunday: $sundayDate")

        fetchSleepTaskFromFirebase(currentMillis)
        initializeIdealSleepSpinner()
        fetchIdealSleepTimeFromFirebase()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        // Store dayName and date in the global variables
        dayName = getDayName(calendar.timeInMillis)
        date = formatDate(calendar.timeInMillis)
        return "$dayName - $date"
    }

    private fun getCurrentDateInMillis(): Long {
        val calendar = Calendar.getInstance()
        return calendar.timeInMillis
    }

    private fun showDatePicker() {
        taskTypeDurations.clear()

        val builder = MaterialDatePicker.Builder.datePicker()

        if (selectedDateMillis != null) {
            builder.setSelection(selectedDateMillis!!)
        }

        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selectedDate ->
            calculateWeekDates(selectedDate)

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate

            // Store dayName and date in the global variables
            dayName = getDayName(selectedDate)
            date = formatDate(selectedDate)

            dateSpinnerTextView.text =
                getString(R.string.selected_date_placeholder, dayName ?: "", date ?: "")

            selectedDateMillis = selectedDate

            // Fetch taskId from Firebase for the selected date
            fetchSleepTaskFromFirebase(selectedDate)
            isDatePickerClickable = true
            Log.d(
                "DatePickerClickable",
                "IsDatePickerClickable: $isDatePickerClickable"
            ) // Log the state

            fetchIdealSleepTimeFromFirebase()

        }
        picker.addOnCancelListener {
            // Re-enable the date picker when Cancel button is clicked
            isDatePickerClickable = true
            Log.d(
                "DatePickerClickable",
                "IsDatePickerClickable: $isDatePickerClickable"
            ) // Log the state

        }

        picker.addOnDismissListener {
            // Re-enable the date picker when it is dismissed (if canceled or date selected)
            isDatePickerClickable = true
            Log.d(
                "DatePickerClickable",
                "IsDatePickerClickable: $isDatePickerClickable"
            ) // Log the state

        }

        if (isDatePickerClickable) {
            isDatePickerClickable = false // Disable the date picker temporarily
            Log.d(
                "DatePickerClickable",
                "IsDatePickerClickable: $isDatePickerClickable"
            ) // Log the state

            picker.show(requireActivity().supportFragmentManager, picker.toString())
        }
    }

    private fun getDayName(dateInMillis: Long): String {
        val sdfDayName = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdfDayName.format(dateInMillis)
    }

    private fun formatDate(dateInMillis: Long): String {
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdfDate.format(dateInMillis)
    }

    private fun calculateWeekDates(selectedDate: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Set the selected day as the starting day of the week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // Get Monday date
        mondayDate = sdf.format(calendar.time)

        // Move to the next Sunday
        calendar.add(Calendar.DATE, 6)
        sundayDate = sdf.format(calendar.time)

        val weekDates = "$mondayDate - $sundayDate"
        Log.d("WeekDates", weekDates)
    }

    private fun fetchSleepTaskFromFirebase(selectedDate: Long) {
        uid?.let { userId ->
            val databaseReference = database.reference
            val weekReference = databaseReference
                .child("Users")
                .child(userId)
                .child("Weeks")
                .child("Tasks")
                .child("Week - $mondayDate to $sundayDate")
                .child("$dayName - ${formatDate(selectedDate)}") // Use selectedDate here

            val sleepTaskListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    taskTypeDurations["Sleep"] = 0L // Reset sleep duration for the selected date
                    dataSnapshot.children.forEach { taskSnapshot ->
                        val taskType = taskSnapshot.child("taskType").getValue(String::class.java)
                        val isCompleted =
                            taskSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false

                        if (taskType == "Sleep" && isCompleted) {
                            val startTime =
                                taskSnapshot.child("startTime").getValue(String::class.java) ?: ""
                            val endTime =
                                taskSnapshot.child("endTime").getValue(String::class.java) ?: ""
                            val duration =
                                calculateDurationForCompletedTasks(startTime, endTime, isCompleted)

                            taskTypeDurations["Sleep"] = duration // Update sleep duration
                            // Log the sleep duration for verification
                            Log.d("SleepDuration", "Sleep duration: $duration minutes")
                        }
                    }

                    // Update the TextViews and other relevant UI components for Sleep duration
                    updateSleepTimeTextView()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching sleep data: ${databaseError.message}")
                    // Handle database fetch error for sleep data
                }
            }

            weekReference.addValueEventListener(sleepTaskListener)
        }
    }

    fun calculateDurationForCompletedTasks(
        startTime: String?,
        endTime: String?,
        isCompleted: Boolean
    ): Long {
        if (isCompleted && startTime != null && endTime != null) {
            val pattern = "hh:mm a" // Define the time pattern
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())

            try {
                val startDate: Date = sdf.parse(startTime)!!
                val endDate: Date = sdf.parse(endTime)!!

                // Calculate the difference between end time and start time in milliseconds
                var differenceInMillis: Long = endDate.time - startDate.time

                // Check if the difference is negative (end time is earlier than start time)
                if (differenceInMillis < 0) {
                    // Adjust the difference to consider a full day (24 hours)
                    differenceInMillis += TimeUnit.DAYS.toMillis(1)
                }

                // Convert milliseconds to minutes
                return TimeUnit.MILLISECONDS.toMinutes(differenceInMillis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return 0 // Return 0 if there's an issue with parsing or if startTime or endTime is null
    }

    private fun updateSleepTimeTextView() {
        val sleepTimeTextView = view?.findViewById<TextView>(R.id.sleepTimeTextView)

        sleepTimeTextView?.text = convertToHoursMinutes(taskTypeDurations["Sleep"] ?: 0L)

        sleepTime = convertToHoursMinutes(taskTypeDurations["Sleep"] ?: 0L)


    }

    private fun convertToHoursMinutes(minutes: Long): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return String.format("%d hours %02d minutes", hours, remainingMinutes)
    }

    private fun initializeIdealSleepSpinner() {
        idealSleepSpinner = requireView().findViewById(R.id.idealSleepSpinner)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.hours_array,
            R.layout.custom_spinner_item
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        idealSleepSpinner.adapter = adapter

        idealSleepSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!isInitialSetup) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()

                    // Update the selected time in the database
                    updateIdealSleepTimeInDatabase(selectedItem)
                } else {
                    isInitialSetup = false // Set the flag to false after initial setup
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where nothing is selected (if needed)
            }
        }
    }

    private fun getIndexOfTime(time: String): Int {
        val taskTypesArray = resources.getStringArray(R.array.hours_array)
        return taskTypesArray.indexOf(time)
    }

    private fun initializeSpinnerTime() {
        idealSleepSpinner = requireView().findViewById(R.id.idealSleepSpinner)
        val adapter = idealSleepSpinner.adapter
        if (adapter is ArrayAdapter<*>) {
            val index = getIndexOfTime(idealTime) // Use your function to get the index
            if (index != -1) {
                idealSleepSpinner.setSelection(index)
            }
        }
        calculateSleepPercentage()
    }

    private fun fetchIdealSleepTimeFromFirebase() {
        uid?.let { userId ->
            val databaseReference = database.reference
            val weekReference = databaseReference
                .child("Users")
                .child(userId)
                .child("Weeks")
                .child("idealSleep")
                .child("Week - $mondayDate to $sundayDate")
                .child("$dayName - $date") // Use selectedDate here

            val idealSleepTimeListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val idealSleepTime =
                        dataSnapshot.child("idealSleepTime").getValue(String::class.java)

                    // Assign fetched idealSleepTime to initialTime
                    idealTime = idealSleepTime ?: ""

                    Log.d("IdealTime", "$dayName - $date : $idealTime")

                    // Call initializeSpinnerTime to update the spinner selection
                    initializeSpinnerTime()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(
                        "FirebaseError",
                        "Error fetching idealSleepTime: ${databaseError.message}"
                    )
                    // Handle database fetch error for idealSleepTime
                }
            }

            weekReference.addListenerForSingleValueEvent(idealSleepTimeListener)
        }
    }


    private fun updateIdealSleepTimeInDatabase(selectedTime: String) {
        uid?.let { userId ->
            val databaseReference = database.reference
            val weekReference = databaseReference
                .child("Users")
                .child(userId)
                .child("Weeks")
                .child("idealSleep")
                .child("Week - $mondayDate to $sundayDate")
                .child("$dayName - $date") // Use dayName and date variables here

            // Update the 'idealSleepTime' value in the database
            weekReference.child("idealSleepTime").setValue(selectedTime)
                .addOnSuccessListener {
                    // Handle success if needed
                    idealTime = selectedTime
                    calculateSleepPercentage()
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Error updating idealSleepTime: ${e.message}")
                    // Handle failure if needed
                }
        }
    }

    private fun calculateSleepPercentage() {

        val dayProgressBar: ProgressBar = requireView().findViewById(R.id.dayProgressBar)
        val progressTextView: TextView = requireView().findViewById(R.id.progressTextView)

        val sleepTimeParts = sleepTime.split(" ")
        val idealTimeParts = idealTime.split(" ")

        val sleepHours = sleepTimeParts[0].toInt()
        val sleepMinutes = sleepTimeParts[2].toInt()

        val idealHours = if (idealTimeParts[0].isNotEmpty()) {
            idealTimeParts[0].toInt()
        } else {
            0
        }

        val sleepInMinutes = sleepHours * 60 + sleepMinutes
        val idealInMinutes = idealHours * 60

        // Calculate the percentage (using your previous code)
        val percentage = (sleepInMinutes.toFloat() / idealInMinutes.toFloat()) * 100

        // Convert the percentage to an integer
        val roundedPercentage = percentage.toInt()

        // Access the string resource for percentage format
        val percentageFormat = getString(R.string.percentage_format)

        // Format the integer using the percentage format string
        val formattedPercentage = String.format(percentageFormat, roundedPercentage)

        if (sleepInMinutes <= idealInMinutes) {
            Log.d("logSleep", "Sleep Percentage: $percentage%")
            dayProgressBar.progress = percentage.toInt()
            progressTextView.text = formattedPercentage
        } else {
            val oversleptHours = (sleepInMinutes - idealInMinutes) / 60
            val oversleptMinutes = (sleepInMinutes - idealInMinutes) % 60

            // Set progress bar to maximum value (100) and show the overslept percentage separately
            dayProgressBar.progress = 100
            progressTextView.text = formattedPercentage

            Log.d(
                "logSleep",
                "Sleep Percentage: $percentage%, Overslept: $oversleptHours hours $oversleptMinutes minutes"
            )
        }
    }
}