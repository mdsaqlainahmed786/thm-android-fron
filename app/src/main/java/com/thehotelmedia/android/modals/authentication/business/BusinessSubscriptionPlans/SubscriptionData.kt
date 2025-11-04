package com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans

import com.google.gson.annotations.SerializedName


data class SubscriptionData (

  @SerializedName("_id"               ) var id                : String?           = null,
  @SerializedName("features"          ) var features          : ArrayList<String> = arrayListOf(),
  @SerializedName("businessSubtypeID" ) var businessSubtypeID : ArrayList<String> = arrayListOf(),
  @SerializedName("businessTypeID"    ) var businessTypeID    : ArrayList<String> = arrayListOf(),
  @SerializedName("name"              ) var name              : String?           = null,
  @SerializedName("description"       ) var description       : String?           = null,
  @SerializedName("googleSubscriptionID"       ) var googleSubscriptionID       : String?           = null,
  @SerializedName("price"             ) var price             : Int?              = null,
  @SerializedName("duration"          ) var duration          : String?           = null,
  @SerializedName("image"             ) var image             : String?           = null,
  @SerializedName("level"             ) var level             : String?           = null,
  @SerializedName("currency"          ) var currency          : String?           = null,
  @SerializedName("createdAt"         ) var createdAt         : String?           = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String?           = null,
  @SerializedName("__v"               ) var _v                : Int?              = null

)