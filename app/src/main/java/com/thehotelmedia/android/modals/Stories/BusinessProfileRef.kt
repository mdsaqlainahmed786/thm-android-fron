package com.thehotelmedia.android.modals.Stories

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class BusinessProfileRef (

  @SerializedName("_id"        ) var Id         : String?     = null,
  @SerializedName("profilePic" ) var profilePic : ProfilePic? = ProfilePic(),
  @SerializedName("username"   ) var username   : String?     = null,
  @SerializedName("name"       ) var name       : String?     = null

)