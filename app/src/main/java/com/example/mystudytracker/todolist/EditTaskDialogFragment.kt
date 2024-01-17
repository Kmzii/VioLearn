package com.example.mystudytracker.todolist

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mystudytracker.R
import com.example.mystudytracker.databinding.FragmentEditTaskDialogBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditTaskDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentEditTaskDialogBinding

    private var taskId: String = ""
    private var taskType: String = ""
    private var taskName: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
    private var isCompleted: Boolean = false

    var taskEditListener: TaskEditListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditTaskDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        registerEvents()
        initializeFields()
    }

    private fun setupSpinner() {
        val taskTypesArray = resources.getStringArray(R.array.task_options_array)
        val adapter =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, taskTypesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dropdownMenu.adapter = adapter

        binding.dropdownMenu.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Handle selection of task type from the spinner
                taskType = taskTypesArray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun registerEvents() {
        binding.startTimeButton.setOnClickListener {
            showTimePicker(true, startTime)
        }

        binding.endTimeButton.setOnClickListener {
            showTimePicker(false, endTime)
        }

        binding.saveButton.setOnClickListener {
            // Retrieve and validate updated task details
            val updatedTaskType = taskType
            val updatedTaskName = binding.addTaskEt.text.toString().trim()
            val updatedStartTime = binding.selectedStartTime.text.toString()
            val updatedEndTime = binding.selectedEndTime.text.toString()

            // Validate if all necessary fields are filled
            if (updatedTaskName.isNotEmpty() && updatedStartTime.isNotEmpty() && updatedEndTime.isNotEmpty()) {
                // Send the updated task details back to ToDoListFragment
                taskEditListener?.onTaskEdited(
                    taskId,
                    updatedTaskType,
                    updatedTaskName,
                    updatedStartTime,
                    updatedEndTime,
                    isCompleted
                )
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun initializeFields() {
        // Retrieve arguments containing task details passed from ToDoListAdapter
        taskId = arguments?.getString("taskId") ?: ""
        taskType = arguments?.getString("taskType") ?: ""
        taskName = arguments?.getString("taskName") ?: ""
        startTime = arguments?.getString("startTime") ?: ""
        endTime = arguments?.getString("endTime") ?: ""
        isCompleted = arguments?.getBoolean("isCompleted") ?: false

        Log.d(
            "ToDoListFragment",
            "EditTaskDialog - ID: $taskId, Type: $taskType, Name: $taskName, Start: $startTime, End: $endTime, Completed: $isCompleted"
        )

        // Initialize UI components with retrieved task details for editing
        binding.dropdownMenu.setSelection(getIndexOfTaskType(taskType))
        binding.addTaskEt.setText(taskName)
        binding.selectedStartTime.text = startTime
        binding.selectedEndTime.text = endTime
    }

    private fun getIndexOfTaskType(taskType: String): Int {
        val taskTypesArray = resources.getStringArray(R.array.task_options_array)
        return taskTypesArray.indexOf(taskType)
    }


    private fun showTimePicker(isStartTime: Boolean, initialTime: String) {
        val calendar = Calendar.getInstance()
        val initialHour: Int
        val initialMinute: Int

        if (initialTime.isNotEmpty()) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val date = timeFormat.parse(initialTime)
            calendar.time = date
            initialHour = calendar.get(Calendar.HOUR_OF_DAY)
            initialMinute = calendar.get(Calendar.MINUTE)
        } else {
            initialHour = calendar.get(Calendar.HOUR_OF_DAY)
            initialMinute = calendar.get(Calendar.MINUTE)
        }

        val timePickerDialog = TimePickerDialog(
            requireContext(), { _, hourOfDay, minute ->
                val selectedTime = formatTime(hourOfDay, minute)
                if (isStartTime) {
                    binding.selectedStartTime.text = selectedTime
                    startTime = selectedTime
                } else {
                    binding.selectedEndTime.text = selectedTime
                    endTime = selectedTime
                }

            },
            initialHour,
            initialMinute,
            DateFormat.is24HourFormat(requireContext())
        )
        timePickerDialog.show()
    }

    private fun formatTime(hourOfDay: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        val dateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH) // Use English locale
        val formattedTime = dateFormat.format(calendar.time)

        // Capitalize AM/PM
        return formattedTime.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }

}