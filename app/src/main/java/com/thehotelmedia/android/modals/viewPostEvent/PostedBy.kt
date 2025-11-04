package com.thehotelmedia.android.modals.viewPostEvent

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.ProfilePic
import com.thehotelmedia.android.modals.viewPostEvent.BusinessProfileRef


data class PostedBy (

  @SerializedName("_id"                ) var Id                 : String?             = null,
  @SerializedName("accountType"        ) var accountType        : String?             = null,
  @SerializedName("name"               ) var name               : String?             = null,
  @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
  @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef(),

  @SerializedName("profilePic"         ) var profilePic         : ProfilePic?         = ProfilePic(),


)