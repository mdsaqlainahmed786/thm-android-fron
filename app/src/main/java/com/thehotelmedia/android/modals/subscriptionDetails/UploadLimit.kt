package com.thehotelmedia.android.modals.subscriptionDetails

import com.google.gson.annotations.SerializedName


data class UploadLimit (

  @SerializedName("fileType" ) var fileType : String? = null,
  @SerializedName("unit"     ) var unit     : String? = null,
  @SerializedName("size"     ) var size     : Int?    = null

)