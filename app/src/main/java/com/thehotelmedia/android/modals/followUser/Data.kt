package com.thehotelmedia.android.modals.followUser

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("status"    ) var status    : String? = null,
  @SerializedName("_id"       ) var Id        : String? = null,
  @SerializedName("follower"  ) var follower  : String? = null,
  @SerializedName("following" ) var following : String? = null,
  @SerializedName("createdAt" ) var createdAt : String? = null,
  @SerializedName("updatedAt" ) var updatedAt : String? = null,
  @SerializedName("__v"       ) var _v        : Int?    = null

)