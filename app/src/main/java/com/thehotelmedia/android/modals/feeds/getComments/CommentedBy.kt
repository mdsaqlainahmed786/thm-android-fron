package com.thehotelmedia.android.modals.feeds.getComments

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.getComments.BusinessProfileRef


data class CommentedBy (

  @SerializedName("_id"                ) var Id                 : String?             = null,
  @SerializedName("accountType"        ) var accountType        : String?             = null,
  @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
  @SerializedName("name"               ) var name               : String?             = null,
  @SerializedName("profilePic"         ) var profilePic         : ProfilePic?         = null,
  @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()

)