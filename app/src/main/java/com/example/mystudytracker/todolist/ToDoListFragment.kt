package com.example.mystudytracker.todolist

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.mystudytracker.R
import com.example.mystudytracker.database.TaskData
import com.example.mystudytracker.databinding.FragmentToDoListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ToDoListFragment : Fragment(),
    TaskAddListener,
    TaskEditListener,
    ToDoListAdapter.TaskUpdateListener,
    ToDoListAdapter.CheckBoxCheckListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var navController: NavController
    private lateinit var binding: FragmentToDoListBinding
    private var dayName: String = ""
    private var date: String = ""
    private var weekDates: String = ""
    private lateinit var dayOfWeekTextView: TextView
    private lateinit var toDoListAdapter: ToDoListAdapter
    private lateinit var taskList: MutableList<TaskData>
    private lateinit var achievedPercentageText: TextView
    private lateinit var progressBar: ProgressBar
    private var isCheckBoxChecked: Boolean = false // Track checkbox state
    private var currentAnimation: LottieAnimationView? = null
    private var animationHandler: Handler? = null

    // Interface method implementations
    override fun onTaskUpdated() {
        updateDailyCompletionPercentage()
    }

    override fun onTaskDeleted() {
        updateDailyCompletionPercentage()
    }

    // Implement the interface method to display the animation
    override fun onCheckBoxChecked(checked: Boolean) {
        val lottieAnimationView = view?.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val lottieConfettiAnimation = view?.findViewById<LottieAnimationView>(R.id.lottieConfettiAnimation)

        if (checked) {
            lottieAnimationView?.let { newAnimationView ->
                if (currentAnimation != null && currentAnimation != newAnimationView) {
                    currentAnimation?.cancelAnimation()
                    currentAnimation?.visibility = View.GONE
                    animationHandler?.removeCallbacksAndMessages(null)
                }

                currentAnimation = newAnimationView
                currentAnimation?.visibility = View.VISIBLE
                currentAnimation?.playAnimation()

                animationHandler?.removeCallbacksAndMessages(null)
                animationHandler = Handler(Looper.getMainLooper())
                animationHandler?.postDelayed({
                    currentAnimation?.visibility = View.GONE
                }, 2000)
            }

            // Check if completion percentage is 100 and display confetti animation
            val completionPercentage = calculateDailyCompletionPercentage(taskList)
            if (completionPercentage == 100f) {
                lottieConfettiAnimation?.visibility = View.VISIBLE
                lottieConfettiAnimation?.playAnimation()
            }
        } else {
            currentAnimation?.cancelAnimation()
            currentAnimation?.visibility = View.GONE
            lottieConfettiAnimation?.visibility = View.GONE  // Hide confetti animation if checkbox is unchecked
            animationHandler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentToDoListBinding.inflate(inflater, container, false)

        // Retrieve arguments
        dayName = arguments?.getString("dayName") ?: ""
        date = arguments?.getString("date") ?: ""
        weekDates = arguments?.getString("weekDates") ?: ""

        dayOfWeekTextView = binding.dayOfWeekTextView
        val formattedText = getString(R.string.day_and_date, dayName, date)
        dayOfWeekTextView.text = formattedText

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        registerEvents()
        fetchTasksFromFirebase()

        /// Initialize the adapter with the edit task listener
        toDoListAdapter = ToDoListAdapter(taskList, databaseRef, this, this,this)
        binding.todoRecyclerView.adapter = toDoListAdapter
        binding.todoRecyclerView.layoutManager = LinearLayoutManager(requireContext())

    }

    private fun init(view: View) {
        achievedPercentageText = view.findViewById(R.id.achievedPercentageText)
        progressBar = view.findViewById(R.id.progressBar)

        navController = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance()
            .reference
            .child("Users")
            .child(auth.currentUser?.uid.toString())
            .child("Weeks")
            .child("Tasks")
            .child("Week - $weekDates")
            .child("$dayName - $date")
    }

    private fun registerEvents() {
        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val fragmentManager = requireActivity().supportFragmentManager
        val addTaskDialog = AddTaskDialogFragment()

        // Set this fragment as the listener in the dialog fragment
        addTaskDialog.taskAddListener = this // Ensure this line is present

        addTaskDialog.show(fragmentManager, "AddTaskDialog")
    }

    // Implement the interface method to receive data from AddTaskDialogFragment
// Inside ToDoListFragment
    override fun onTaskAdded(
        taskType: String,
        taskName: String,
        startTime: String,
        endTime: String,
        isCompleted: Boolean
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val taskData = mapOf(
                "taskType" to taskType,
                "taskName" to taskName,
                "startTime" to startTime,
                "endTime" to endTime,
                "isCompleted" to isCompleted
            )

            // Push task data to Firebase
            databaseRef.push().setValue(taskData)
                .addOnSuccessListener {
                    Log.d("ToDoListFragment", "Task added successfully")
                    updateDailyCompletionPercentage()
                }
                .addOnFailureListener { e ->
                    Log.e("ToDoListFragment", "Error adding task", e)
                }
        } else {
            Log.e("ToDoListFragment", "User is not authenticated")
        }
    }

    private fun fetchTasksFromFirebase() {
        // Initialize the list to hold tasks
        taskList = mutableListOf()

        // Add a ChildEventListener to fetch tasks from Firebase
        databaseRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val taskId = dataSnapshot.key ?: ""
                val taskType = dataSnapshot.child("taskType").getValue(String::class.java) ?: ""
                val taskName = dataSnapshot.child("taskName").getValue(String::class.java) ?: ""
                val startTime = dataSnapshot.child("startTime").getValue(String::class.java) ?: ""
                val endTime = dataSnapshot.child("endTime").getValue(String::class.java) ?: ""
                val isCompleted = dataSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false

                val taskData = TaskData(taskId, taskType, taskName, startTime, endTime, isCompleted)
                taskList.add(taskData)

                // Notify the adapter that a new item has been added
                toDoListAdapter.notifyItemInserted(taskList.size - 1)

                updateDailyCompletionPercentage()
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes to existing children if needed
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // Handle removal of children if needed
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // Handle movement of children if needed
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled event if needed
            }
        })
    }

    override fun onTaskEdited(
        taskId: String,
        taskType: String,
        taskName: String,
        startTime: String,
        endTime: String,
        isCompleted: Boolean
    ) {
        // Log the edited task details received from EditTaskDialogFragment
        Log.d("ToDoListFragment", "Task Edited - ID: $taskId, Type: $taskType, Name: $taskName, Start: $startTime, End: $endTime, Completed: $isCompleted")

        // Reference to the specific task node in Firebase using taskId
        val taskRef = databaseRef.child(taskId)

        // Create a map with the updated task details
        val updatedTaskData = mapOf(
            "taskType" to taskType,
            "taskName" to taskName,
            "startTime" to startTime,
            "endTime" to endTime,
            "isCompleted" to isCompleted
        )

        // Update the task details in Firebase
        taskRef.setValue(updatedTaskData)
            .addOnSuccessListener {
                Log.d("ToDoListFragment", "Task updated successfully in Firebase")

                // Notify the adapter about the change in the dataset
                // Find the updated task in the taskList and update it
                val updatedTaskIndex = taskList.indexOfFirst { it.taskId == taskId }
                if (updatedTaskIndex != -1) {
                    val updatedTask = TaskData(taskId, taskType, taskName, startTime, endTime, isCompleted)
                    taskList[updatedTaskIndex] = updatedTask
                    // Notify the adapter that the item at updatedTaskIndex has changed
                    toDoListAdapter.notifyItemChanged(updatedTaskIndex)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ToDoListFragment", "Error updating task in Firebase", e)
            }
    }
    // Function to calculate the daily completion percentage
    private fun calculateDailyCompletionPercentage(taskList: List<TaskData>): Float {
        val totalTasks = taskList.size
        val completedTasks = taskList.count { it.isCompleted }
        return if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks.toFloat()) * 100
        } else {
            0f // If there are no tasks, return 0 as the completion percentage
        }
    }

    // Inside ToDoListFragment where you add or delete tasks

    // Call this function after adding/deleting tasks to recalculate the completion percentage
    private fun updateDailyCompletionPercentage() {
        val completionPercentage = calculateDailyCompletionPercentage(taskList)
        Log.d("ToDoListFragment", "Daily Completion Percentage: $completionPercentage")

        updateAchievedPercentageUI(completionPercentage)

        updateDailyCompletionInFirebase(completionPercentage)



    }

    // Function to update the achieved percentage UI
    private fun updateAchievedPercentageUI(percentage: Float) {
        if (!isAdded || activity == null || isDetached) {
            // Fragment is not attached to the activity or context
            return
        }

        val roundedPercentage = percentage.toInt() // Convert Float to Int
        achievedPercentageText.text = getString(R.string.achieved_percentage, roundedPercentage)
        progressBar.progress = roundedPercentage
    }

    private fun updateDailyCompletionInFirebase(dailyCompletionPercentage: Float) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val dailyCompletionRef = FirebaseDatabase.getInstance()
                .reference
                .child("Users")
                .child(currentUser.uid)
                .child("Weeks")
                .child("dailyCompletion")
                .child("Week - $weekDates")
                .child("$dayName - $date")
                .child("dailyCompletionPercentage")

            // Update daily completion percentage in Firebase
            dailyCompletionRef.setValue(dailyCompletionPercentage)
                .addOnSuccessListener {
                    Log.d("ToDoListFragment", "Daily completion percentage updated in Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e("ToDoListFragment", "Error updating daily completion percentage", e)
                }
        } else {
            Log.e("ToDoListFragment", "User is not authenticated")
        }
    }

}
