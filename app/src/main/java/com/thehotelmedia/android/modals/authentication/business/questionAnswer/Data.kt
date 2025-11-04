package com.thehotelmedia.android.modals.authentication.business.questionAnswer

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("answer"   ) var answer   : ArrayList<String> = arrayListOf(),
  @SerializedName("question" ) var question : String?           = null,
  @SerializedName("id"       ) var id       : String?           = null

)