package com.example.mystudytracker.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubOptionsAdapter(private val subOptions: List<Pair<String, Int>>,
                        private val subOptionCounts: MutableMap<String, Int>,
                        private val uid: String,
                        private val updateCountInFirebase: (String, Int) -> Unit // Add this as a parameter
) :
    RecyclerView.Adapter<SubOptionsAdapter.ViewHolder>() {

    private var currentDateCalendar = Calendar.getInstance()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subOptionImageView: ImageView = itemView.findViewById(R.id.subOptionImageView)
        val subOptionTextView: TextView = itemView.findViewById(R.id.subOptionTextView)
        val minusButton: ImageButton = itemView.findViewById(R.id.minusButton)
        val countTextView: TextView = itemView.findViewById(R.id.countTextView)
        val plusButton: ImageButton = itemView.findViewById(R.id.plusButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_sub_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (subOption, imageResource) = subOptions[position]

        holder.subOptionImageView.setImageResource(imageResource)
        holder.subOptionTextView.text = subOption

        // Get the count for the specific sub-option
        val count = subOptionCounts[subOption] ?: 0

        holder.countTextView.text = count.toString()

        holder.minusButton.setOnClickListener {
            if (count > 0) {
                subOptionCounts[subOption] = count - 1
                updateCountInFirebase(subOption, count - 1)
                notifyDataSetChanged()
            }
        }

        holder.plusButton.setOnClickListener {
            subOptionCounts[subOption] = count + 1
            updateCountInFirebase(subOption, count + 1)
            notifyDataSetChanged()
        }
    }


    override fun getItemCount(): Int {
        return subOptions.size
    }
}
