package com.thehotelmedia.android.modals.subscriptionDetails

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("uploadLimit"     ) var uploadLimit     : ArrayList<UploadLimit> = arrayListOf(),
  @SerializedName("hasSubscription" ) var hasSubscription : Boolean?               = null

)