package com.thehotelmedia.android.modals.forms.createStory

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"       ) var id        : String? = null,
  @SerializedName("timeStamp" ) var timeStamp : String? = null,
  @SerializedName("userID"    ) var userID    : String? = null,
  @SerializedName("mediaID"   ) var mediaID   : String? = null,
  @SerializedName("createdAt" ) var createdAt : String? = null,
  @SerializedName("updatedAt" ) var updatedAt : String? = null,
  @SerializedName("__v"       ) var _v        : Int?    = null

)