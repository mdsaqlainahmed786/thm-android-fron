package com.thehotelmedia.android.SocketModals.fetchConversation

import com.google.gson.annotations.SerializedName


data class Messages (

  @SerializedName("_id"       ) var Id        : String?  = null,
  @SerializedName("message"   ) var message   : String?  = null,
  @SerializedName("mediaUrl"  ) var mediaUrl  : String?  = null,
  @SerializedName("thumbnailUrl"  ) var thumbnailUrl  : String?  = null,
  @SerializedName("isSeen"    ) var isSeen    : Boolean? = null,
  @SerializedName("type"      ) var type      : String?  = null,
  @SerializedName("createdAt" ) var createdAt : String?  = null,
  @SerializedName("mediaID" ) var mediaID : String?  = null,
  @SerializedName("storyID" ) var storyID : String?  = null,
  @SerializedName("postID" ) var postID : String?  = null,
  @SerializedName("postOwnerUsername" ) var postOwnerUsername : String?  = null,
  @SerializedName("__v"       ) var _v        : Int?     = null,
  @SerializedName("sentByMe"  ) var sentByMe  : Boolean? = null,
  @SerializedName("isStoryAvailable"  ) var isStoryAvailable  : Boolean? = null,

)