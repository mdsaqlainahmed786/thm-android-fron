package com.thehotelmedia.android.adapters.authentication.business

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.IncludeItemsBinding

class SubscriptionIncludeLinesAdapter(private val context: Context,private val  features: ArrayList<String>) : RecyclerView.Adapter<SubscriptionIncludeLinesAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: IncludeItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = IncludeItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return features.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.text.text = features[position]

    }


}