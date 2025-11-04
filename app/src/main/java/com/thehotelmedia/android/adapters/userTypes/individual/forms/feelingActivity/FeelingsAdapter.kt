package com.thehotelmedia.android.adapters.userTypes.individual.forms.feelingActivity

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.FeelingsItemsLayoutBinding

class FeelingsAdapter(
    private val context: Context,
    private val feelingsList: ArrayList<String>,
    private val onFeelingClick: (String) -> Unit
) : RecyclerView.Adapter<FeelingsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: FeelingsItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeelingsItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feelingsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        val feelings = feelingsList[position]
        binding.feelingTv.text = feelings

        binding.root.setOnClickListener {
            onFeelingClick(feelings)
        }
    }

}