package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName

/**
 * Lightweight availability payload returned by some backend versions from `bookings/check-in`.
 * The UI expects `availableRooms` (full room objects), so we use this list as a fallback to
 * fetch full room details by id.
 */
data class Availability(
    @SerializedName("_id") var id: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("totalRooms") var totalRooms: Int? = null,
    @SerializedName("availableRooms") var availableRooms: Int? = null,
)


