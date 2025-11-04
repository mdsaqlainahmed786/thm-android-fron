package com.thehotelmedia.android.modals.checkIn

import com.google.gson.annotations.SerializedName


data class PlusCode (

  @SerializedName("compound_code" ) var compoundCode : String? = null,
  @SerializedName("global_code"   ) var globalCode   : String? = null

)