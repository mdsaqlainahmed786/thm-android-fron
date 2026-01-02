package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.userTypes.individual.BookTableBanquetActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.BookingSummaryActivity
import com.thehotelmedia.android.databinding.BookingHistoryItemsLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.extensions.toFormattedDateTime
import com.thehotelmedia.android.extensions.updateBookingStatusColor
import com.thehotelmedia.android.modals.booking.bookingHistory.BookingHistoryData

class BookingHistoryAdapter(
    private val context: Context,
    private val onCancelClick: (String) -> Unit
) : PagingDataAdapter<BookingHistoryData, BookingHistoryAdapter.BookingViewHolder>(
    BookingHistoryDiffCallback()
) {

    inner class BookingViewHolder(private val binding: BookingHistoryItemsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingHistoryData) {

            val businessProfileRef = booking.businessProfileRef

            val bookingId = booking.Id ?: ""
            val bookingType = booking.bookingType ?: ""
            val typeOfEvent = booking.metadata?.typeOfEvent ?: ""
            val guestValue = booking.adults ?: 0

            val businessName = businessProfileRef?.name ?: ""
            val businessProfilePic = businessProfileRef?.profilePic?.large ?: ""
            val address = businessProfileRef?.address

            val street = address?.street ?: ""
            val city = address?.city ?: ""
            val state = address?.state ?: ""
            val zipCode = address?.zipCode ?: ""
            val country = address?.country ?: ""

            val fullAddress = if (street.isEmpty()){
                "$city, $state, $country, $zipCode"
            }else{
                "$street, $city, $state, $country, $zipCode"
            }

            val grandTotal = booking.grandTotal ?: 0.0
            val bookedRoom = booking.bookedRoom

            val nightsNumber = bookedRoom?.nights ?: 0
            val roomQuantity = bookedRoom?.quantity ?: 0



            val createdAt = booking.createdAt ?: ""
            val bookingStatus = booking.status ?: ""
            val roomsRef = booking.roomsRef

            val roomType = roomsRef?.roomType ?: ""



            when (bookingType) {
                "book-banquet" -> {
                    binding.nightsTv.text = typeOfEvent
                    binding.roomTypeTv.text = "$guestValue ${context.getString(R.string.guest)}"

                }
                "book-table" -> {
                    binding.nightsTv.text = "Table Booking"
                    binding.roomTypeTv.text = "$guestValue ${context.getString(R.string.guest)}"
                }
                else -> {
                    binding.nightsTv.text = "$nightsNumber * ${context.getString(R.string.nights)}"
                    binding.roomTypeTv.text = "$roomQuantity * ${roomType.capitalizeFirstLetter()} ${context.getString(R.string.room)}"

                }
            }



            binding.nameTv.text = businessName
            binding.locationTv.text = fullAddress
            binding.priceTv.text = "₹${grandTotal.toInt()}"
            binding.bookingTime.text = "${context.getString(R.string.booked_on)} ${createdAt.toFormattedDateTime()}"



            binding.bookingStatus.text = bookingStatus.capitalizeFirstLetter()
            Glide.with(context).load(businessProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
            binding.bookingCard.updateBookingStatusColor(context, bookingStatus)

            // Show cancel button only for pending hotel bookings
            if (bookingStatus.lowercase() == "pending" && bookingType != "book-banquet" && bookingType != "book-table") {
                binding.cancelBtn.visibility = android.view.View.VISIBLE
                binding.cancelBtn.setOnClickListener {
                    onCancelClick(bookingId)
                }
            } else {
                binding.cancelBtn.visibility = android.view.View.GONE
            }


            binding.root.setOnClickListener {

                when (bookingType) {
                    "book-banquet" -> {
                        val intent = Intent(context, BookTableBanquetActivity::class.java)
                        intent.putExtra("BOOKING_ID", bookingId)
                        intent.putExtra("BOOKING_TYPE", bookingType)
                        context.startActivity(intent)
                    }
                    "book-table" -> {
                        val intent = Intent(context, BookTableBanquetActivity::class.java)
                        intent.putExtra("BOOKING_ID", bookingId)
                        intent.putExtra("BOOKING_TYPE", bookingType)
                        context.startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(context, BookingSummaryActivity::class.java)
                        intent.putExtra("BOOKING_ID", bookingId)
                        context.startActivity(intent)
                    }
                }



            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = BookingHistoryItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = getItem(position)
        booking?.let { holder.bind(it) }
    }



    class BookingHistoryDiffCallback : DiffUtil.ItemCallback<BookingHistoryData>() {
        override fun areItemsTheSame(oldItem: BookingHistoryData, newItem: BookingHistoryData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: BookingHistoryData, newItem: BookingHistoryData): Boolean {
            return oldItem == newItem
        }
    }


}
