package com.thehotelmedia.android.adapters.authentication.business

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.databinding.SubscriptionItemBinding
import com.thehotelmedia.android.extensions.toTimePeriod
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.SubscriptionData

class SubscriptionAdapter(
    private val context: Context,
    private val subscriptionList: List<SubscriptionData>,
) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: SubscriptionItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SubscriptionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = subscriptionList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscription = subscriptionList[position]
        val binding = holder.binding


        Glide.with(context)
            .load(subscription.image)
            .placeholder(R.drawable.ic_standard)
            .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.faded_round_btn)))
            .into(binding.ivSubscription)


        binding.titleTv.text = subscription.name
        binding.tvAmount.text = "INR ${subscription.price}"
        binding.descriptionTv.text = subscription.description

        val timePeriod = subscription.duration?.toTimePeriod()
        binding.tvDuration.text = timePeriod

        binding.includeRv.adapter = SubscriptionIncludeLinesAdapter(context, subscription.features)
    }
}
