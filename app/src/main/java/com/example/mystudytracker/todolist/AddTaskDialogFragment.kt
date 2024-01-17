package com.example.mystudytracker.todolist

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mystudytracker.R
import com.example.mystudytracker.databinding.FragmentAddTaskDialogBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentAddTaskDialogBinding
    private var taskType: String = ""
    private var taskName: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
    private var isCompleted: Boolean = false

    var taskAddListener: TaskAddListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddTaskDialogBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        registerEvents()
    }

    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.task_options_array,
            R.layout.custom_spinner_item // Use your custom layout for spinner items
        )

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dropdownMenu.adapter = spinnerAdapter
    }


    private fun registerEvents() {
        binding.startTimeButton.setOnClickListener {
            showTimePicker(true)
        }

        binding.endTimeButton.setOnClickListener {
            showTimePicker(false)
        }

        binding.saveButton.setOnClickListener {
            taskType = binding.dropdownMenu.selectedItem.toString()
            taskName = binding.addTaskEt.text.toString()
            if (taskType.isNotEmpty() && taskName.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty()) {
                binding.saveButton.isEnabled = false

                // Log the values just before invoking the listener
                Log.d("AddTaskDialog", "Task Type: $taskType")
                Log.d("AddTaskDialog", "Task Name: $taskName")
                Log.d("AddTaskDialog", "Start Time: $startTime")
                Log.d("AddTaskDialog", "End Time: $endTime")
                Log.d("AddTaskDialog", "Is Completed: $isCompleted")

                taskAddListener?.onTaskAdded(taskType, taskName, startTime, endTime, isCompleted)
                dismiss()
            } else {
                Toast.makeText(context, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }

        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)
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