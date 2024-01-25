package com.example.mystudytracker.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mystudytracker.R

class IWouldLikeAdapter(private val data: List<IWouldLikeItem>) :
    RecyclerView.Adapter<IWouldLikeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImageView: ImageView = itemView.findViewById(R.id.itemImageView)
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val itemCountTextView: TextView = itemView.findViewById(R.id.itemCountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_i_would_like, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.itemImageView.setImageResource(item.imageResource)
        holder.itemNameTextView.text = item.itemName
        // Use resource string with a placeholder for the quantity
        val quantityText = holder.itemView.context.getString(R.string.quantity_text, item.itemCount)
        holder.itemCountTextView.text = quantityText
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
