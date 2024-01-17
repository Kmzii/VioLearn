package com.example.mystudytracker

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.database.YourData

class MainAdapter(private val dataList: MutableList<YourData>) :
    RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(dayName: String, date: String, weekDates: String)
    }

    fun setOnItemClickListener(listener: (String, String, String) -> Unit) {
        this.listener = object : OnItemClickListener {
            override fun onItemClick(dayName: String, date: String, weekDates: String) {
                listener(dayName, date, weekDates)
            }
        }
    }

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardTextView: TextView = itemView.findViewById(R.id.cardTextView)
        val progressTextView: TextView = itemView.findViewById(R.id.progressTextView)
        val dayProgressBar: ProgressBar = itemView.findViewById(R.id.dayProgressBar)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val dayName = dataList[position].dayName
                    val date = dataList[position].date
                    val weekDates = dataList[position].weekDates // Get weekDates value
                    Log.d("WeekDatesBeforeClick", "Week Dates: $weekDates") // Log weekDates value
                    listener?.onItemClick(dayName, date, weekDates)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val currentItem = dataList[position]

        // Format day name and date using String.format()
        val formattedText = String.format(
            "%s : %s",
            currentItem.dayName,
            currentItem.date
        )
        holder.cardTextView.text = formattedText

        // Retrieve dailyCompletionPercentage from YourData model class
        val completionPercentage = currentItem.dailyCompletionPercentage

        // Set progress bar's completion based on the fetched percentage
        holder.dayProgressBar.progress = completionPercentage

        // Set progress text
        holder.progressTextView.text = "$completionPercentage%" // Display the percentage
    }


    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateData(newData: MutableList<YourData>) {
        val oldSize = dataList.size
        dataList.clear()
        dataList.addAll(newData)
        val newSize = dataList.size
        notifyItemRangeChanged(0, minOf(oldSize, newSize))
        if (oldSize < newSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
        } else if (oldSize > newSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize)
        }
    }
}