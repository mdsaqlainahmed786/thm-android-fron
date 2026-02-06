package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.utils.gson.StringListFlexibleAdapter


data class AvailableRooms (

  @SerializedName("_id"           ) var Id            : String?                 = null,
  @SerializedName("bedType"       ) var bedType       : String?                 = null,
  @SerializedName("adults"        ) var adults        : Int?                    = null,
  @SerializedName("children"      ) var children      : Int?                    = null,
  @SerializedName("maxOccupancy"  ) var maxOccupancy  : Int?                    = null,
  @SerializedName("availability"  ) var availability  : Boolean?                = null,
  // Some backend deployments return amenities as a list of objects instead of strings.
  @JsonAdapter(StringListFlexibleAdapter::class)
  @SerializedName("amenities"     ) var amenities     : ArrayList<String>       = arrayListOf(),
  @SerializedName("title"         ) var title         : String?                 = null,
  @SerializedName("description"   ) var description   : String?                 = null,
  @SerializedName("pricePerNight" ) var pricePerNight : Double?                    = null,
  @SerializedName("currency"      ) var currency      : String?                 = null,
  @SerializedName("mealPlan"      ) var mealPlan      : String?                 = null,
  @SerializedName("cover"         ) var cover         : Cover?                  = Cover(),
  // Used as fallback for image rendering when cover is missing
  @SerializedName("roomImagesRef" ) var roomImagesRef : ArrayList<Cover>        = arrayListOf(),
  @SerializedName("amenitiesRef"  ) var amenitiesRef  : ArrayList<AmenitiesRef> = arrayListOf()

)