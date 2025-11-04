package com.thehotelmedia.android.modals.followerFollowing

import com.google.gson.annotations.SerializedName


data class FollowFollowingData (

  @SerializedName("_id"         ) var Id          : String?     = null,
  @SerializedName("accountType" ) var accountType : String?     = null,
  @SerializedName("role"        ) var role        : String?     = null,
  @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
  @SerializedName("username"    ) var username    : String?     = null,
  @SerializedName("name"        ) var name        : String?     = null,
  @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()

)