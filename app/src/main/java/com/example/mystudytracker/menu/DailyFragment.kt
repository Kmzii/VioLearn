package com.example.mystudytracker.menu

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mystudytracker.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
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


class DailyFragment : Fragment() {
    // Declare global variables for dayName and date
    private var dayName: String? = null
    private var date: String? = null
    private lateinit var dateSpinner: View
    private lateinit var dateSpinnerTextView: TextView
    private var selectedDateMillis: Long? = null
    private var currentDateMillis: Long? = null
    private var mondayDate: String? = null
    private var sundayDate: String? = null
    private var taskTypeDurations: MutableMap<String, Long> = mutableMapOf(
        "Physics" to 0L,
        "Chemistry" to 0L,
        "Zoology" to 0L,
        "Botany" to 0L,
        "Sleep" to 0L,
        "Other" to 0L
    )
    private var isDatePickerClickable: Boolean = true

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
        val view = inflater.inflate(R.layout.fragment_daily, container, false)
        dateSpinner = view.findViewById(R.id.dateSpinner)
        dateSpinnerTextView = view.findViewById(R.id.dateSpinnerTextView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton: View = requireView().findViewById(R.id.back_button)

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_dailyFragment_to_homeFragment)
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

        fetchTaskIdFromFirebase(currentMillis)
        dataListing()

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
            fetchTaskIdFromFirebase(selectedDate)
            isDatePickerClickable = true
            Log.d(
                "DatePickerClickable",
                "IsDatePickerClickable: $isDatePickerClickable"
            ) // Log the state

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

    private fun fetchTaskIdFromFirebase(selectedDate: Long) {
        uid?.let { userId ->
            val databaseReference = database.reference
            val weekReference = databaseReference
                .child("Users")
                .child(userId)
                .child("Weeks")
                .child("Tasks")
                .child("Week - $mondayDate to $sundayDate")
                .child("$dayName - ${formatDate(selectedDate)}") // Use selectedDate here

            val taskListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.children.forEach { taskSnapshot ->
                        val taskId = taskSnapshot.key ?: ""
                        val startTime =
                            taskSnapshot.child("startTime").getValue(String::class.java) ?: ""
                        val endTime =
                            taskSnapshot.child("endTime").getValue(String::class.java) ?: ""
                        val isCompleted =
                            taskSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
                        val duration =
                            calculateDurationForCompletedTasks(startTime, endTime, isCompleted)

                        // Update the cumulative duration for the taskType
                        if (isCompleted) {
                            val taskType =
                                taskSnapshot.child("taskType").getValue(String::class.java) ?: ""
                            val currentDuration = taskTypeDurations[taskType] ?: 0L
                            taskTypeDurations[taskType] = currentDuration + duration

                            // Use taskId and taskData as needed
                            Log.d(
                                "Tasks",
                                "taskId: $taskId, taskType: $taskType, startTime: $startTime, endTime: $endTime, isCompleted: $isCompleted"
                            )
                        }

                    }

                    // Display total time for each taskType
                    taskTypeDurations.forEach { (taskType, totalDuration) ->
                        Log.d(
                            "TaskTypeDuration",
                            "Task Type: $taskType, Total Duration: $totalDuration minutes"
                        )
                    }
                    updateTextViews()
                    updateTotalStudyTime()
                    dataListing()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching data: ${databaseError.message}")
                    // Handle database fetch error
                }
            }

            weekReference.addValueEventListener(taskListener)
        }
    }

    // Function to calculate the duration in minutes between start and end times
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

    // Function to update TextViews with task type durations
    private fun updateTextViews() {
        val physicsTimeTextView = view?.findViewById<TextView>(R.id.physicsTimeTextView)
        val chemistryTimeTextView = view?.findViewById<TextView>(R.id.chemistryTimeTextView)
        val zoologyTimeTextView = view?.findViewById<TextView>(R.id.zoologyTimeTextView)
        val botanyTimeTextView = view?.findViewById<TextView>(R.id.botanyTimeTextView)
//        val sleepTimeTextView = view?.findViewById<TextView>(R.id.sleepTimeTextView)
        val otherTimeTextView = view?.findViewById<TextView>(R.id.otherTimeTextView)

        // Convert and display durations for each task type in hours and minutes format
        physicsTimeTextView?.text = convertToHoursMinutes(taskTypeDurations["Physics"] ?: 0L)
        chemistryTimeTextView?.text =
            convertToHoursMinutes(taskTypeDurations["Chemistry"] ?: 0L)

        zoologyTimeTextView?.text =
            convertToHoursMinutes(taskTypeDurations["Zoology"] ?: 0L)

        botanyTimeTextView?.text =
            convertToHoursMinutes(taskTypeDurations["Botany"] ?: 0L)
//        sleepTimeTextView?.text = getString(
//            R.string.task_type_time_format,
//            "Sleep",
//            convertToHoursMinutes(taskTypeDurations["Sleep"] ?: 0L)
//        )
        otherTimeTextView?.text = convertToHoursMinutes(taskTypeDurations["Other"] ?: 0L)
    }

    private fun convertToHoursMinutes(minutes: Long): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return String.format("%d hours %02d minutes", hours, remainingMinutes)
    }

    private fun updateTotalStudyTime() {
        val totalStudyTimeTextView = view?.findViewById<TextView>(R.id.totalStudyTimeTextView)

        // Calculate the total time studied for physics, chemistry, botany, and zoology in minutes
        val physicsDuration = taskTypeDurations["Physics"] ?: 0L
        val chemistryDuration = taskTypeDurations["Chemistry"] ?: 0L
        val botanyDuration = taskTypeDurations["Botany"] ?: 0L
        val zoologyDuration = taskTypeDurations["Zoology"] ?: 0L

        val totalStudyTimeInMinutes =
            physicsDuration + chemistryDuration + botanyDuration + zoologyDuration

        // Convert total study time from minutes to hours and minutes
        val hours = totalStudyTimeInMinutes / 60
        val minutes = totalStudyTimeInMinutes % 60

        // Set the total study time in hours and minutes format in the totalStudyTextView
        totalStudyTimeTextView?.text = getString(R.string.total_study_time_format, hours, minutes)
    }

    private fun dataListing() {
        // Prepare data to display on the chart (converted to hours)
        val physicsHours = (taskTypeDurations["Physics"] ?: 0L) / 60.0
        val chemistryHours = (taskTypeDurations["Chemistry"] ?: 0L) / 60.0
        val zoologyHours = (taskTypeDurations["Zoology"] ?: 0L) / 60.0
        val botanyHours = (taskTypeDurations["Botany"] ?: 0L) / 60.0
        val othersHours = (taskTypeDurations["Other"] ?: 0L) / 60.0

        // Store the calculated hours in a list
        val hoursList = listOf(
            physicsHours.toFloat(),
            chemistryHours.toFloat(),
            zoologyHours.toFloat(),
            botanyHours.toFloat(),
            othersHours.toFloat()
        )

        // Pass the hours list to the setChart function
        setChart(hoursList)
    }

    private fun setChart(hoursList: List<Float>) {
        val barChart: BarChart = view?.findViewById(R.id.barChart) ?: return
        val barChartRender =
            CustomBarChartRender(barChart, barChart.animator, barChart.viewPortHandler)
        barChartRender.setRadius(20)
        barChart.renderer = barChartRender

        val colors = ArrayList<Int>()
        val maxHours = hoursList.maxOrNull() ?: 0f
        Log.d("MaxHours", "Max hours: $maxHours") // Logging the maximum hours

        val darkestColor = Color.parseColor("#CF0CC6") // Darkest shade
        val lightestColor = Color.parseColor("#FFCCFF") // Lightest shade

        for (hours in hoursList) {
            val fraction = if (maxHours != 0f) (maxHours - hours) / maxHours else 1f
            val adjustedColor = adjustColorShade(darkestColor, lightestColor, fraction)
            colors.add(adjustedColor)
            Log.d("ColorForHours", "Hours: $hours, Color: $adjustedColor") // Logging the colors

        }

        // Prepare entries for the BarChart
        val entries: ArrayList<BarEntry> = ArrayList()
        for ((index, value) in hoursList.withIndex()) {
            entries.add(BarEntry(index.toFloat(), value))
        }

        val barDataSet = BarDataSet(entries, "") // No label for the dataset
        barDataSet.colors = colors // Set custom colors for bars
        barDataSet.valueTextSize = 12f // Set the font size for the values on top of bars
        barDataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_color) // Set text color for bar value texts

        val dataSets: ArrayList<IBarDataSet> = ArrayList()
        dataSets.add(barDataSet)

        // Create BarData and customize the chart
        val data = BarData(dataSets)
        barChart.data = data

        // Customize XAxis (Bottom axis)
        val xAxis = barChart.xAxis
        xAxis.textSize = 12f // Set the text size for X-axis labels
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_color) // Set text color for X-axis labels
        xAxis.setDrawGridLines(false) // Disable vertical grid lines
        xAxis.setDrawAxisLine(true) // Enable XAxis line
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Set X-axis position to bottom

        // Set custom labels for the X-axis
        val labels = listOf("Phy", "Chem", "Zoo", "Bot", "Other")
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        // Customize YAxis (Left axis)
        val leftAxis = barChart.axisLeft
        leftAxis.textSize = 12f // Set the text size for Y-axis labels
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_color) // Set text color for Y-axis labels
        leftAxis.setDrawGridLines(false) // Disable horizontal grid lines
        leftAxis.setDrawAxisLine(true) // Enable YAxis line
        leftAxis.axisMinimum = 0f // Set minimum value to zero

        // Hide the right YAxis (if not needed)
        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false // Disable right YAxis

        barChart.setExtraBottomOffset(10f) // Adjust the bottom offset as needed (e.g., 10f for extra space)

        // Customize chart borders
        barChart.setDrawBorders(true)
        barChart.setBorderColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        barChart.setBorderWidth(1f)
        barChart.animateY(1500, Easing.EaseInOutQuad)

        barChart.setFitBars(true)
        barChart.description.isEnabled = false // Disable bottom label

        // Remove the legend (color blocks beside the label)
        val legend = barChart.legend
        legend.isEnabled = false

        barChart.setScaleEnabled(false)
        barChart.setDoubleTapToZoomEnabled(false)

        barChart.invalidate() // Refresh the chart
    }

    // Function to adjust color shade based on fraction
    private fun adjustColorShade(darkestColor: Int, lightestColor: Int, fraction: Float): Int {
        val alpha = Color.alpha(darkestColor)
        val red = ((Color.red(lightestColor) - Color.red(darkestColor)) * fraction + Color.red(
            darkestColor
        )).toInt()
        val green =
            ((Color.green(lightestColor) - Color.green(darkestColor)) * fraction + Color.green(
                darkestColor
            )).toInt()
        val blue = ((Color.blue(lightestColor) - Color.blue(darkestColor)) * fraction + Color.blue(
            darkestColor
        )).toInt()
        return Color.argb(alpha, red, green, blue)
    }

}
