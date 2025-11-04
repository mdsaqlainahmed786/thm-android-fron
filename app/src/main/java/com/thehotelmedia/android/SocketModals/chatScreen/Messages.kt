package com.thehotelmedia.android.SocketModals.chatScreen

import com.google.gson.annotations.SerializedName


data class Messages (

  @SerializedName("unseenCount" ) var unseenCount : Int?        = null,
  @SerializedName("message"     ) var message     : String?     = null,
  @SerializedName("isSeen"      ) var isSeen      : Boolean?    = null,
  @SerializedName("type"        ) var type        : String?     = null,
  @SerializedName("createdAt"   ) var createdAt   : String?     = null,
  @SerializedName("lookupID"    ) var lookupID    : String?     = null,
  @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
  @SerializedName("username"    ) var username    : String?     = null,
  @SerializedName("name"        ) var name        : String?     = null

)