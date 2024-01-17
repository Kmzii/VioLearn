package com.example.mystudytracker.menu

import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R

class AchievementsAdapter : RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {
    private val achievementsList = mutableListOf<String>()

    fun addAchievement(achievement: String) {
        achievementsList.add(achievement)
    }

    fun clearAchievements() {
        achievementsList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val achievement = achievementsList[position]
        holder.bind(achievement)
    }

    override fun getItemCount(): Int {
        return achievementsList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(achievement: String) {
            val textView: TextView = itemView.findViewById(R.id.achievementTextView)

            // Extract the first three words from the achievement string
            val words = achievement.split(" ")
            val firstThreeWords = words.take(3).joinToString(" ")

            // Find the starting index of the first three words
            val startIndex = achievement.indexOf(firstThreeWords)

            if (startIndex != -1) {
                // Create a SpannableStringBuilder
                val spannable = SpannableStringBuilder(achievement)

                // Apply bold style to the first three words
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    startIndex + firstThreeWords.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Set the formatted text to the TextView
                textView.text = spannable
            } else {
                // If the first three words are not found, set the original achievement text
                textView.text = achievement
            }
        }
    }
}