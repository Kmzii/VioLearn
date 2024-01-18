package com.example.mystudytracker.menu

data class ExpandableItem(
    val category: String,
    val subOptions: List<Pair<String, Int>>, // Pair with sub-option text and image resource ID
    val subOptionCounts: MutableMap<String, Int>, // Map to store counts for each sub-option
    var isExpanded: Boolean = false
)
