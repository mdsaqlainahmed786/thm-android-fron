package com.thehotelmedia.android.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SugestedPlaceItemLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.checkIn.Results

class PlacesAdapter(
    private val context: Context,
    private val  placeList: ArrayList<Results>,
    private val onPlaceClick: (placeId: String,placeName: String,address: String, lat: Double, lng: Double) -> Unit
    ) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>()  {

    inner class ViewHolder(val binding: SugestedPlaceItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SugestedPlaceItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return placeList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        val name = placeList[position].name.toString().capitalizeFirstLetter()
        val location = placeList[position].vicinity.toString()
        val icon = placeList[position].icon


        Glide.with(context).load(icon).placeholder(R.drawable.ic_selected_home).into(binding.iconIv)

        val blueColor = getColor(context, R.color.blue)
        binding.iconIv.setColorFilter(blueColor, PorterDuff.Mode.SRC_IN)


        binding.titleTv.text = name
        binding.descriptionTv.text = location


        holder.itemView.setOnClickListener {
                val placeId = placeList[position].placeId.toString()
                val lat = placeList[position].geometry?.location?.lat ?: 0.0 // Provide a default value
                val lng = placeList[position].geometry?.location?.lng ?: 0.0 // Provide a default value

                // Trigger the lambda function
                onPlaceClick(placeId,name,location, lat, lng)

        }





    }
}