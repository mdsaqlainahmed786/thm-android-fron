package com.thehotelmedia.android.modals.booking.bookingSummary

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.booking.bookingSummary.AmenitiesRef
import com.thehotelmedia.android.modals.booking.bookingSummary.RoomImagesRef


data class RoomsRef (

    @SerializedName("_id"           ) var Id            : String?                  = null,
    @SerializedName("bedType"       ) var bedType       : String?                  = null,
    @SerializedName("roomType"      ) var roomType      : String?                  = null,
    @SerializedName("title"         ) var title         : String?                  = null,
    @SerializedName("description"   ) var description   : String?                  = null,
    @SerializedName("amenitiesRef"  ) var amenitiesRef  : ArrayList<AmenitiesRef>  = arrayListOf(),
    @SerializedName("roomImagesRef" ) var roomImagesRef : ArrayList<RoomImagesRef> = arrayListOf()

)