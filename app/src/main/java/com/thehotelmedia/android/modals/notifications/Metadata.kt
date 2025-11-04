package com.thehotelmedia.android.modals.notifications

import com.google.gson.annotations.SerializedName


data class Metadata (

  @SerializedName("postID"        ) var postID       : String? = null,
  @SerializedName("userID"        ) var userID       : String? = null,
  @SerializedName("message"       ) var message      : String? = null,
  @SerializedName("connectionID"  ) var connectionID : String? = null,
  @SerializedName("postType"      ) var postType     : String? = null,
  @SerializedName("jobID"         ) var jobID        : String? = null,
  @SerializedName("bookingID"     ) var bookingID        : String? = null,
  @SerializedName("type"          ) var type        : String? = null,

)