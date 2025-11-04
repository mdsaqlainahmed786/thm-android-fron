package com.thehotelmedia.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.ItemReportReasonItemsBinding

class ReportReasonsAdapter
    (private val context: Context,
                           private val reasonsList: List<String>,
                           private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<ReportReasonsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReportReasonItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportReasonsAdapter.ViewHolder {
        val binding = ItemReportReasonItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return reasonsList.size
    }


    override fun onBindViewHolder(holder: ReportReasonsAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        val reason = reasonsList[position]
        binding.reportTitle.text = reason

        binding.root.setOnClickListener {
            onItemClick(reason)
        }

    }

}