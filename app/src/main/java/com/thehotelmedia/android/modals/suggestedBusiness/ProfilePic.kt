package com.thehotelmedia.android.modals.suggestedBusiness

import com.google.gson.annotations.SerializedName


data class ProfilePic (

  @SerializedName("small"  ) var small  : String? = null,
  @SerializedName("medium" ) var medium : String? = null,
  @SerializedName("large"  ) var large  : String? = null

)