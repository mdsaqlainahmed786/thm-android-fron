package com.thehotelmedia.android.modals.feeds.createComment

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("isParent"          ) var isParent          : Boolean? = null,
  @SerializedName("_id"               ) var id                : String?  = null,
  @SerializedName("userID"            ) var userID            : String?  = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String?  = null,
  @SerializedName("postID"            ) var postID            : String?  = null,
  @SerializedName("message"           ) var message           : String?  = null,
  @SerializedName("parentID"          ) var parentID          : String?  = null,
  @SerializedName("createdAt"         ) var createdAt         : String?  = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String?  = null,
  @SerializedName("__v"               ) var _v                : Int?     = null,

)