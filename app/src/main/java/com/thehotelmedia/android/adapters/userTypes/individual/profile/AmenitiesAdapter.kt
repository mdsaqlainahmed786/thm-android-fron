package com.thehotelmedia.android.adapters.userTypes.individual.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants.N_A
import com.thehotelmedia.android.databinding.ProfileAmenitiesItemsBinding
import com.thehotelmedia.android.modals.profileData.profile.AmenitiesRef

import kotlinx.coroutines.*

class AmenitiesAdapter(
    private val context: Context,
    private val amenitiesRef: ArrayList<AmenitiesRef>
) : RecyclerView.Adapter<AmenitiesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ProfileAmenitiesItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var switchJob: Job? = null

        fun bind(item: AmenitiesRef, position: Int) {
//            // Set static data
//            Glide.with(context)
//                .load(item.icon)
//                .placeholder(R.drawable.ic_profile_placeholder)
//                .into(binding.imageView)

            // Stop any previous coroutine (older version toggled temp/aqi)
            switchJob?.cancel()

            when (item.Id) {
                "static_temp" -> {
                    val tempText = item.minMaxTemp?.takeIf { it.isNotBlank() && it != N_A }
                    binding.titleTv.text = tempText ?: N_A
                    binding.imageView.setImageResource(R.drawable.temprature)
                }
                "static_aqi" -> {
                    val aqiText = item.aqi?.takeIf { it > 0 }?.toString()
                    binding.titleTv.text = if (aqiText != null) "AQI $aqiText" else N_A
                    binding.imageView.setImageResource(R.drawable.ic_aqi)
                }
                else -> {
                    // For other positions, show just name/icon from backend
                    binding.titleTv.text = item.name
                    Glide.with(context)
                        .load(item.icon)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(binding.imageView)
                }
            }
        }

        fun stopSwitching() {
            switchJob?.cancel()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileAmenitiesItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(amenitiesRef[position], position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stopSwitching()
    }

    override fun getItemCount(): Int = amenitiesRef.size
}
