package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms
import com.thehotelmedia.android.modals.booking.checkIn.Booking


data class Data (

  @SerializedName("booking"        ) var booking        : Booking?                  = Booking(),
  @SerializedName("roomsRequired"  ) var roomsRequired  : Int?                      = null,
  // Backend may return a lightweight availability list instead of full room objects.
  // We use it as a fallback to fetch full room details.
  @SerializedName("availability"   ) var availability   : ArrayList<Availability>   = arrayListOf(),
  @SerializedName("availableRooms" ) var availableRooms : ArrayList<AvailableRooms> = arrayListOf(),
  @SerializedName("user"           ) var user           : User?                     = User()
)