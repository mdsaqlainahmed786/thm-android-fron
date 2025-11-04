package com.thehotelmedia.android.modals.updateAddress

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("geoCoordinate" ) var geoCoordinate : GeoCoordinate? = GeoCoordinate(),
  @SerializedName("_id"           ) var id            : String?        = null,
  @SerializedName("street"        ) var street        : String?        = null,
  @SerializedName("city"          ) var city          : String?        = null,
  @SerializedName("state"         ) var state         : String?        = null,
  @SerializedName("zipCode"       ) var zipCode       : String?        = null,
  @SerializedName("country"       ) var country       : String?        = null,
  @SerializedName("userID"        ) var userID        : String?        = null,
  @SerializedName("lat"           ) var lat           : Double?        = null,
  @SerializedName("lng"           ) var lng           : Double?        = null,
  @SerializedName("createdAt"     ) var createdAt     : String?        = null,
  @SerializedName("updatedAt"     ) var updatedAt     : String?        = null,
  @SerializedName("__v"           ) var _v            : Int?           = null

)