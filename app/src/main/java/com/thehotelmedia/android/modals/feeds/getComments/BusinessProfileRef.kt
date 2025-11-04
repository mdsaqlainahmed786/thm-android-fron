package com.thehotelmedia.android.modals.feeds.getComments

import com.google.gson.annotations.SerializedName


data class BusinessProfileRef (

  @SerializedName("_id"             ) var Id              : String?          = null,
  @SerializedName("profilePic"      ) var profilePic      : ProfilePic?      = ProfilePic(),
  @SerializedName("name"            ) var name            : String?          = null,
  @SerializedName("businessTypeRef" ) var businessTypeRef : BusinessTypeRef? = BusinessTypeRef()

)