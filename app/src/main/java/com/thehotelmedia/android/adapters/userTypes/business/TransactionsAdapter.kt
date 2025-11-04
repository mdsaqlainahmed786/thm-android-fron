package com.thehotelmedia.android.adapters.userTypes.business

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.theme.ThemeHelper
import com.thehotelmedia.android.databinding.TransactionItemsLayoutBinding
import com.thehotelmedia.android.extensions.toReadableDate
import com.thehotelmedia.android.extensions.toReadableTime
import com.thehotelmedia.android.modals.transactions.TransactionData

class TransactionsAdapter (private val context: Context) : PagingDataAdapter<TransactionData, TransactionsAdapter.ViewHolder>(
    TransactionsDiffCallback())   {
    inner class ViewHolder(val binding: TransactionItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TransactionItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        transaction?.let {
            val binding = holder.binding

            val grandTotal = it.grandTotal?.toInt()
            val subscriptionPlanRef = it.subscriptionPlanRef
            val planName = subscriptionPlanRef?.name
            val planImage = subscriptionPlanRef?.image

            val orderId = it.orderID
            var updatedAt = it.updatedAt

            if (updatedAt.isNullOrEmpty()){
                updatedAt = it.createdAt
            }
            val date = updatedAt.toString().toReadableDate()
            val time = updatedAt.toString().toReadableTime()

            binding.totalPrice.text = "₹${grandTotal}"
            binding.planName.text = planName
            binding.transactionId.text = orderId
            binding.dateTv.text = date
            binding.timeTv.text = time
            Glide.with(context).load(planImage).placeholder(R.drawable.ic_premium).into(binding.planIv)
            val isDarkTheme = ThemeHelper.isDarkModeEnabled(context)
            if (isDarkTheme){
                binding.planIv.setColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
            }else{
                binding.planIv.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN)
            }

        }
    }

    class TransactionsDiffCallback : DiffUtil.ItemCallback<TransactionData>() {
        override fun areItemsTheSame(oldItem: TransactionData, newItem: TransactionData): Boolean {
            return oldItem.Id == newItem.Id
        }
        override fun areContentsTheSame(oldItem: TransactionData, newItem: TransactionData): Boolean {
            return oldItem == newItem
        }
    }

}