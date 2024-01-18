package com.example.mystudytracker.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class ExpandableAdapter(private val items: List<ExpandableItem>) :
    RecyclerView.Adapter<ExpandableAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryHeading: View = itemView.findViewById(R.id.categoryHeading)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        val expandImageView: ImageView = itemView.findViewById(R.id.expandImageView)
        val subOptionsRecyclerView: RecyclerView = itemView.findViewById(R.id.subOptionsRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_expandable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.categoryTextView.text = item.category

        // Handle expand/collapse
        holder.categoryHeading.setOnClickListener {
            item.isExpanded = !item.isExpanded
            // Toggle visibility of sub-options RecyclerView
            val visibility = if (holder.subOptionsRecyclerView.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
            holder.subOptionsRecyclerView.visibility = visibility

            notifyDataSetChanged() // Notify adapter to refresh views

        }

        // Set the correct image resource based on the expansion state
        val expandImageResource = if (item.isExpanded) {
            R.drawable.caret_up_outline
        } else {
            R.drawable.caret_down_outline
        }

        holder.expandImageView.setImageResource(expandImageResource)

        val userUid = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // Setup and bind sub-options RecyclerView
        val subOptionsAdapter = SubOptionsAdapter(item.subOptions, item.subOptionCounts, userUid)
        holder.subOptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subOptionsAdapter
            visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }
}
