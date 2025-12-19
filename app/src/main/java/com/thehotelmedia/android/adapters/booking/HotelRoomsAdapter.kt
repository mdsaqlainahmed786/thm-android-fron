package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.booking.RoomDetailsActivity
import com.thehotelmedia.android.databinding.BookingRoomItemLayoutBinding
import com.thehotelmedia.android.extensions.setRoomTypeImage
import com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms


class HotelRoomsAdapter(
    private val context: Context,
    private val availableRoomsList: ArrayList<AvailableRooms>,
    private val bookingId: String,
    private val checkInDate: String,
    private val checkOutDate: String,
    private val roomsRequired: Int,
    private val guestCount: Int,
    private val dialCode: String,
    private val phoneNumber: String,
    private val isNumberVerified: Boolean,
) : RecyclerView.Adapter<HotelRoomsAdapter.MyViewHolder>() {

    inner class MyViewHolder(val binding: BookingRoomItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = BookingRoomItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val binding = holder.binding

        val rooms = availableRoomsList[position]
        val roomId = rooms.Id ?: ""
        val title = rooms.title ?: ""
        val description = rooms.description ?: ""
        val pricePerNight = rooms.pricePerNight ?: 0.0
        val currency = rooms.currency ?: ""
        val coverImage = selectBestRoomImageUrl(rooms)
        val amenitiesRef = rooms.amenitiesRef

        val bedType = rooms.bedType ?: ""


        Glide.with(context)
            .load(coverImage)
            .placeholder(R.drawable.room_image)
            .error(R.drawable.room_image)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.coverIv)
        binding.roomTypeTv.text = title
        binding.roomPriceTv.text = formatPrice(pricePerNight, currency)
        binding.roomDescriptionTv.text = description


        binding.roomTypeIv.setRoomTypeImage(bedType)



        val itemList = rooms.amenitiesRef.mapNotNull { it.name }
        // Example List
//        val itemList = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7", "Item 8")
        val adapter = FeaturesAdapter(itemList, true)

        binding.recyclerView.adapter = adapter


//        binding.root.setOnClickListener {
//            val intent = Intent(context, RoomDetailsActivity::class.java)
//            context.startActivity(intent)
//        }

        binding.root.setOnClickListener {
            val intent = Intent(context, RoomDetailsActivity::class.java).apply {
                putExtra("ROOM_ID", roomId)
                putExtra("BOOKING_ID", bookingId)
                putExtra("CHECK_IN_DATE", checkInDate)
                putExtra("CHECK_OUT_DATE", checkOutDate)
                putExtra("PRICE_PER_NIGHT", pricePerNight)
                putExtra("ROOM_REQUIRED", roomsRequired)
                putExtra("GUEST_COUNT", guestCount)
                putExtra("DIAL_CODE", dialCode)
                putExtra("PHONE_NUMBER", phoneNumber)
                putExtra("IS_NUMBER_VERIFIED", isNumberVerified)
            }
            context.startActivity(intent)
        }




    }

    override fun getItemCount(): Int = availableRoomsList.size

    private fun selectBestRoomImageUrl(room: AvailableRooms): String {
        val fromCover = room.cover?.sourceUrl?.takeIf { it.isNotBlank() }
            ?: room.cover?.thumbnailUrl?.takeIf { it.isNotBlank() }
        if (!fromCover.isNullOrBlank()) return normalizeUrl(fromCover)

        val fromImages = room.roomImagesRef
            .firstOrNull { it.isCoverImage == true }?.sourceUrl?.takeIf { it.isNotBlank() }
            ?: room.roomImagesRef.firstOrNull()?.sourceUrl?.takeIf { it.isNotBlank() }
            ?: room.roomImagesRef.firstOrNull { it.thumbnailUrl?.isNotBlank() == true }?.thumbnailUrl

        return normalizeUrl(fromImages ?: "")
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        // Force https for common misconfigured http URLs.
        return if (trimmed.startsWith("http://")) "https://${trimmed.removePrefix("http://")}" else trimmed
    }

    private fun formatPrice(pricePerNight: Double, currency: String): String {
        val symbol = when (currency.trim().uppercase()) {
            "INR", "₹" -> "₹"
            "USD", "$" -> "$"
            "EUR", "€" -> "€"
            "GBP", "£" -> "£"
            else -> if (currency.isNotBlank()) currency.trim() else "₹"
        }
        return "${symbol}${pricePerNight.toInt()}/"
    }
}
