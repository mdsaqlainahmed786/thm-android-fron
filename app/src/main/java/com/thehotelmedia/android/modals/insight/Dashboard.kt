package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Dashboard (

  @SerializedName("accountReached"     ) var accountReached     : Int? = null,
  @SerializedName("websiteRedirection" ) var websiteRedirection : Int? = null,
  @SerializedName("totalFollowers"     ) var totalFollowers     : Int? = null,
  @SerializedName("engagements"        ) var engagements        : Int? = null

)