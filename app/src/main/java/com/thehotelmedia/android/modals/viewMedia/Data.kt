package com.thehotelmedia.android.modals.viewMedia

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"               ) var Id                : String? = null,
  @SerializedName("userID"            ) var userID            : String? = null,
  @SerializedName("postID"            ) var postID            : String? = null,
  @SerializedName("mediaID"           ) var mediaID           : String? = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("createdAt"         ) var createdAt         : String? = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String? = null,
  @SerializedName("__v"               ) var _v                : Int?    = null,

)