package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName


data class AvailableRooms (

  @SerializedName("_id"           ) var Id            : String?                 = null,
  @SerializedName("bedType"       ) var bedType       : String?                 = null,
  @SerializedName("adults"        ) var adults        : Int?                    = null,
  @SerializedName("children"      ) var children      : Int?                    = null,
  @SerializedName("maxOccupancy"  ) var maxOccupancy  : Int?                    = null,
  @SerializedName("availability"  ) var availability  : Boolean?                = null,
  @SerializedName("amenities"     ) var amenities     : ArrayList<String>       = arrayListOf(),
  @SerializedName("title"         ) var title         : String?                 = null,
  @SerializedName("description"   ) var description   : String?                 = null,
  @SerializedName("pricePerNight" ) var pricePerNight : Double?                    = null,
  @SerializedName("currency"      ) var currency      : String?                 = null,
  @SerializedName("mealPlan"      ) var mealPlan      : String?                 = null,
  @SerializedName("cover"         ) var cover         : Cover?                  = Cover(),
  @SerializedName("amenitiesRef"  ) var amenitiesRef  : ArrayList<AmenitiesRef> = arrayListOf()

)