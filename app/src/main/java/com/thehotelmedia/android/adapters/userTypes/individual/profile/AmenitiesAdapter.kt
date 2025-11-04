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

            if (position == 0) {
                switchJob?.cancel()
                switchJob = CoroutineScope(Dispatchers.Main).launch {
                    var showTemp = true
                    while (isActive) {
                        if (showTemp) {
                            binding.titleTv.text = item.minMaxTemp ?: N_A
                            binding.imageView.setImageResource(R.drawable.temprature) // your custom temp icon
                        } else {
                            binding.titleTv.text = "AQI: ${item.aqi ?: N_A}"
                            binding.imageView.setImageResource(R.drawable.ic_aqi) // your custom AQI icon
                        }
                        showTemp = !showTemp
                        delay(5000)
                    }
                }
            } else {
                // For other positions, show just name
                binding.titleTv.text = item.name
                // Set static data
                Glide.with(context)
                    .load(item.icon)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.imageView)
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
