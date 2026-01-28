package com.thehotelmedia.android.modals.booking.roomDetails

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.utils.gson.StringListFlexibleAdapter
import com.thehotelmedia.android.utils.gson.StringOrObjectIdAdapter


data class Data (

  @SerializedName("_id"               ) var Id                : String?                  = null,
  @SerializedName("bedType"           ) var bedType           : String?                  = null,
  @SerializedName("adults"            ) var adults            : Int?                     = null,
  @SerializedName("children"          ) var children          : Int?                     = null,
  @SerializedName("maxOccupancy"      ) var maxOccupancy      : Int?                     = null,
  @SerializedName("availability"      ) var availability      : Boolean?                 = null,
  // Some backend deployments return amenities as a list of objects instead of strings.
  @JsonAdapter(StringListFlexibleAdapter::class)
  @SerializedName("amenities"         ) var amenities         : ArrayList<String>        = arrayListOf(),
  @SerializedName("roomType"          ) var roomType          : String?                  = null,
  // Some backend deployments return businessProfileID as an object (e.g. { _id: "..." }).
  @JsonAdapter(StringOrObjectIdAdapter::class)
  @SerializedName("businessProfileID" ) var businessProfileID : String?                  = null,
  @SerializedName("title"             ) var title             : String?                  = null,
  @SerializedName("description"       ) var description       : String?                  = null,
  @SerializedName("pricePerNight"     ) var pricePerNight     : Double?                  = null,
  @SerializedName("currency"          ) var currency          : String?                  = null,
  @SerializedName("mealPlan"          ) var mealPlan          : String?                  = null,
  @SerializedName("createdAt"         ) var createdAt         : String?                  = null,
  @SerializedName("checkIn"           ) var checkIn           : String?                   = null,
  @SerializedName("checkOut"          ) var checkOut          : String?                   = null,
  @SerializedName("languageSpoken"    ) var languageSpoken    : ArrayList<LanguageSpoken> = arrayListOf(),
  @SerializedName("roomImagesRef"     ) var roomImagesRef     : ArrayList<RoomImagesRef> = arrayListOf(),
  @SerializedName("cover"             ) var cover             : Cover?                   = Cover(),
  @SerializedName("amenitiesRef"      ) var amenitiesRef      : ArrayList<AmenitiesRef>  = arrayListOf()

)