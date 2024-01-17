package com.example.mystudytracker.todolist

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R
import com.example.mystudytracker.database.TaskData
import com.google.firebase.database.DatabaseReference

class ToDoListAdapter(
    private val taskList: MutableList<TaskData>,
    private val databaseRef: DatabaseReference,
    private val editTaskListener: TaskEditListener,
    private val taskUpdateListener: TaskUpdateListener,
    private val checkBoxCheckListener: CheckBoxCheckListener
) :
    RecyclerView.Adapter<ToDoListAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskName: TextView = itemView.findViewById(R.id.todoItemTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.todoCheckBox)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_to_do_list, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentItem = taskList[position]
        val formattedText =
            "${currentItem.taskType}\n${currentItem.taskName}\nStart: ${currentItem.startTime}\nEnd: ${currentItem.endTime}"

        // Create a SpannableString to format the text
        val spannable = SpannableString(formattedText)

        // Apply bold style only to the TaskType part
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0, // Start index of TaskType
            currentItem.taskType.length, // End index of TaskType (exclusive)
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.taskName.text = spannable
        holder.checkBox.isChecked = currentItem.isCompleted

        // Set a tag on the checkbox to hold its position in the list
        holder.checkBox.tag = position

        // Handle checkbox click event
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val clickedItem = taskList[adapterPosition]
                clickedItem.isCompleted = isChecked

                // Update the corresponding value in Firebase
                updateTaskCompletionStatus(clickedItem.taskId, isChecked)
            }
        }

        holder.checkBox.setOnClickListener{
            if (holder.checkBox.isChecked) {
                // Checkbox is checked, send true
                checkBoxCheckListener.onCheckBoxChecked(true)
            } else {
                // Checkbox is unchecked, send false
                checkBoxCheckListener.onCheckBoxChecked(false)
            }
        }

        holder.editButton.setOnClickListener {
            val editDialog = EditTaskDialogFragment()
            val bundle = Bundle().apply {
                putString("taskId", currentItem.taskId)
                putString("taskType", currentItem.taskType)
                putString("taskName", currentItem.taskName)
                putString("startTime", currentItem.startTime)
                putString("endTime", currentItem.endTime)
                putBoolean("isCompleted", currentItem.isCompleted)
            }
            editDialog.arguments = bundle
            editDialog.taskEditListener = editTaskListener
            editDialog.show(
                (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                "EditTaskDialog"
            )
        }

        holder.deleteButton.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val deletedTask = taskList[adapterPosition]

                // Delete the task from Firebase using the passed DatabaseReference
                deleteTaskFromFirebase(deletedTask.taskId, adapterPosition)
            }
        }
    }

    // Function to update task completion status in Firebase
    private fun updateTaskCompletionStatus(taskId: String, isChecked: Boolean) {
        // Update 'isCompleted' value in the Firebase database for the specific task ID
        val taskRef = databaseRef.child(taskId)
        taskRef.child("isCompleted").setValue(isChecked)
            .addOnSuccessListener {
                Log.d("ToDoListAdapter", "Task completion status updated in Firebase")
                taskUpdateListener.onTaskUpdated()
            }
            .addOnFailureListener { e ->
                Log.e("ToDoListAdapter", "Error updating task completion status", e)
            }
    }

    private fun deleteTaskFromFirebase(taskId: String, adapterPosition: Int) {
        val taskRef = databaseRef.child(taskId)
        taskRef.removeValue()
            .addOnSuccessListener {
                // Remove the deleted item from the taskList in the adapter
                taskList.removeAt(adapterPosition)

                // Notify the adapter about the item removal
                notifyItemRemoved(adapterPosition)
                notifyItemRangeChanged(adapterPosition, taskList.size)

                Log.d("ToDoListAdapter", "Task deleted from Firebase")

                taskUpdateListener.onTaskDeleted()
            }
            .addOnFailureListener { e ->
                Log.e("ToDoListAdapter", "Error deleting task", e)
            }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    interface TaskUpdateListener {
        fun onTaskUpdated()
        fun onTaskDeleted()
    }

    interface CheckBoxCheckListener {
        fun onCheckBoxChecked(checked: Boolean)
    }

}
