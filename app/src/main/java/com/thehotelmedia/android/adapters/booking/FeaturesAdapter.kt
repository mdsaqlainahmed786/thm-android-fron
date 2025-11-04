package com.thehotelmedia.android.adapters.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FeaturesItemsLayoutBinding

class FeaturesAdapter(private val items: List<String>, private val showFiveItems : Boolean) : RecyclerView.Adapter<FeaturesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = FeaturesItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val binding = holder.binding

            if (showFiveItems){
                if (position < 5) {
                    binding.tvItem.text = items[position]
                    binding.imageView.setImageResource(R.drawable.ic_round_selected_checkbox)
                } else {
                    binding.tvItem.text = "${items.size - 5} more"  // Extra count item
                    binding.imageView.setImageResource(R.drawable.ic_add_grey)
                }
            }else{
                binding.tvItem.text = items[position]
                binding.imageView.setImageResource(R.drawable.ic_round_selected_checkbox)
            }


        }

        override fun getItemCount(): Int {
            return if (items.size > 5) 6 else items.size  // Max 6 items hi dikhne chahiye
        }

        class ViewHolder(val binding: FeaturesItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)
}


