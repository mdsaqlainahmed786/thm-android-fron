package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.BusinessAmenitiesItemLayoutBinding

class BusinessAmenitiesParentAdapter(
    private val context: Context,
    private val items: List<Map<String, Any>> // List of maps to display in the RecyclerView
) : RecyclerView.Adapter<BusinessAmenitiesParentAdapter.BusinessAmenityViewHolder>() {

    // ViewBinding for the item layout
    inner class BusinessAmenityViewHolder(private val binding: BusinessAmenitiesItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(item: Map<String, Any>, position: Int) {
            // Assuming the map contains a list of categories with "category" and "names"
            val amenityName = item["category"] as? String ?: "Unknown Amenity"
            binding.textViewAmenity.text = amenityName

            // Extracting the names directly into a List<String>
            val namesList = (item["names"] as? List<String>) ?: emptyList()

            println("asdgjaksldjgklasjg   $namesList")
            val adapter = FeaturesAdapter(namesList, false)

            binding.recyclerView.adapter = adapter

            // Hide the RecyclerView in the last item
            if (position == items.size - 1) {
                binding.view.visibility = View.INVISIBLE // Hide RecyclerView for the last item
            } else {
                binding.view.visibility = View.VISIBLE // Ensure RecyclerView is visible for other items
            }



        }
    }

    // Create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessAmenityViewHolder {
        val binding = BusinessAmenitiesItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BusinessAmenityViewHolder(binding)
    }

    // Bind data to ViewHolder
    override fun onBindViewHolder(holder: BusinessAmenityViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item,position)

    }

    // Return the item count
    override fun getItemCount(): Int = items.size
}
