package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.GuestListItemLayoutBinding
import com.thehotelmedia.android.modals.booking.bookingSummary.GuestDetails

class GuestListAdapter(private val context: Context, private val guestList: ArrayList<GuestDetails>) : RecyclerView.Adapter<GuestListAdapter.NameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val binding = GuestListItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        holder.bind(position + 1, guestList[position])  // position +1 for numbering
    }

    override fun getItemCount(): Int = guestList.size

    class NameViewHolder(private val binding: GuestListItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int, guestDetails: GuestDetails) {
            val suffix = when {
                position % 10 == 1 && position % 100 != 11 -> "st"
                position % 10 == 2 && position % 100 != 12 -> "nd"
                position % 10 == 3 && position % 100 != 13 -> "rd"
                else -> "th"
            }
            val title = guestDetails.title ?: ""
            val guestName = guestDetails.fullName ?: ""
            binding.guestTv.text = "Guest $position$suffix"
            binding.guestNameTv.text = "$title $guestName"
        }
    }
}
