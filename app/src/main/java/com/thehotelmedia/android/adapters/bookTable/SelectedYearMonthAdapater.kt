package com.thehotelmedia.android.adapters.bookTable

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.MultipleSelectedItemsBinding
import com.thehotelmedia.android.R

class SelectedYearMonthAdapter(
    private val context: Context,
    private val options: List<String>,
    private val isYear: Boolean,
    private val onSelected: (Int, Boolean) -> Unit,
    private val selectedItem: String?
) : RecyclerView.Adapter<SelectedYearMonthAdapter.ViewHolder>() {

    private var selectedPosition: Int = options.indexOf(selectedItem)

    inner class ViewHolder(val binding: MultipleSelectedItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MultipleSelectedItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = options.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val option = options[position]

        binding.text.text = option

        if (position == selectedPosition) {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.selected_text_popup_background_selected)
        } else {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.selected_text_popup_background_unselected)
        }

        holder.itemView.setOnClickListener {
            onSelected(position, isYear)
            val prevSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(prevSelected)
            notifyItemChanged(selectedPosition)
        }
    }
}
