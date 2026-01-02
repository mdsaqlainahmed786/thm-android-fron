package com.thehotelmedia.android.SocketModals.privateMessage

import com.google.gson.annotations.SerializedName


data class Message (

  @SerializedName("type"    ) var type    : String? = null,
  @SerializedName("message" ) var message : String? = null,
  @SerializedName("mediaUrl" ) var mediaUrl : String? = null,
  @SerializedName("storyID" ) var storyID : String? = null,
  @SerializedName("postID" ) var postID : String? = null,
  @SerializedName("postOwnerUsername" ) var postOwnerUsername : String? = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String? = null,
  @SerializedName("mediaID" ) var mediaID : String? = null,
  @SerializedName("isStoryAvailable"  ) var isStoryAvailable  : Boolean? = null,
  @SerializedName("messageID" ) var messageID : String? = null,
  @SerializedName("_id"       ) var _id       : String? = null,
  @SerializedName("isEdited"  ) var isEdited  : Boolean? = null,
  @SerializedName("editedAt"  ) var editedAt  : String?  = null

)