package com.thehotelmedia.android.modals.booking.bookTable

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("status"              ) var status              : String?           = null,
  @SerializedName("adults"              ) var adults              : Int?              = null,
  @SerializedName("children"            ) var children            : Int?              = null,
  @SerializedName("childrenAge"         ) var childrenAge         : ArrayList<String> = arrayListOf(),
  @SerializedName("isTravellingWithPet" ) var isTravellingWithPet : Boolean?          = null,
  @SerializedName("bookedFor"           ) var bookedFor           : String?           = null,
  @SerializedName("subTotal"            ) var subTotal            : Int?              = null,
  @SerializedName("discount"            ) var discount            : Int?              = null,
  @SerializedName("tax"                 ) var tax                 : Int?              = null,
  @SerializedName("convinceCharge"      ) var convinceCharge      : Int?              = null,
  @SerializedName("grandTotal"          ) var grandTotal          : Int?              = null,
  @SerializedName("type"                ) var type                : String?           = null,
  @SerializedName("_id"                 ) var Id                  : String?           = null,
  @SerializedName("checkIn"             ) var checkIn             : String?           = null,
  @SerializedName("checkOut"            ) var checkOut            : String?           = null,
  @SerializedName("guestDetails"        ) var guestDetails        : ArrayList<String> = arrayListOf(),
  @SerializedName("bookingID"           ) var bookingID           : String?           = null,
  @SerializedName("userID"              ) var userID              : String?           = null,
  @SerializedName("businessProfileID"   ) var businessProfileID   : String?           = null,
  @SerializedName("createdAt"           ) var createdAt           : String?           = null,
  @SerializedName("updatedAt"           ) var updatedAt           : String?           = null,
  @SerializedName("__v"                 ) var _v                  : Int?              = null

)