package com.thehotelmedia.android.adapters.helpAndSupports

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FaqRecyclerItemsBinding
import com.thehotelmedia.android.modals.helpAndSupport.faqs.FAQsData

class FAQRecyclerAdapter(
    private val context: Context
) : PagingDataAdapter<FAQsData, FAQRecyclerAdapter.ViewHolder>(COMPARATOR) {

    private val itemVisibilityMap = mutableMapOf<Int, Boolean>()

    inner class ViewHolder(private val binding: FaqRecyclerItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FAQsData, position: Int) {
            binding.apply {
                title.text = item.question
                descriptionTv.text = item.answer


                if (!itemVisibilityMap.containsKey(position)) {
                    itemVisibilityMap[position] = false
                }

                dropdownBtn.setImageResource(
                    if (itemVisibilityMap[position] == true)
                        R.drawable.ic_up
                    else
                        R.drawable.ic_down
                )
                descriptionTv.visibility =
                    if (itemVisibilityMap[position] == true)
                        View.VISIBLE
                    else
                        View.GONE
                descriptionView.visibility =
                    if (itemVisibilityMap[position] == true)
                        View.VISIBLE
                    else
                        View.GONE

                actionDownBtn.setOnClickListener {
                    itemVisibilityMap[position] = !itemVisibilityMap[position]!!
                    notifyItemChanged(position)
                }

                // Add top margin to the first item only
                val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
                if (position == 0) {
                    layoutParams.topMargin = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                } else {
                    layoutParams.topMargin = 0 // Reset margin for other items
                }
                binding.root.layoutParams = layoutParams


            }
        }
    }
    // Function to hide all descriptions
    fun hideAllDescriptions() {
        // Set all items' visibility to false
        itemVisibilityMap.keys.forEach { position ->
            itemVisibilityMap[position] = false
        }
        // Notify the adapter to refresh all items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FaqRecyclerItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, position)
        }
    }

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<FAQsData>() {
            override fun areItemsTheSame(oldItem: FAQsData, newItem: FAQsData): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: FAQsData, newItem: FAQsData): Boolean {
                return oldItem == newItem
            }
        }
    }
}
