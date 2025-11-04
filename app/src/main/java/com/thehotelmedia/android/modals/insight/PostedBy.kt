package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.insight.BusinessProfileRef


data class PostedBy (

  @SerializedName("_id"                ) var Id                 : String?             = null,
  @SerializedName("accountType"        ) var accountType        : String?             = null,
  @SerializedName("name"               ) var name               : String?             = null,
  @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
  @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()

)