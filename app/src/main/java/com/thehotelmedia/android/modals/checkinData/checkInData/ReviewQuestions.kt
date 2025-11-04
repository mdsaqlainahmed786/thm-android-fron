package com.thehotelmedia.android.modals.checkinData.checkInData

import com.google.gson.annotations.SerializedName


data class ReviewQuestions (

  @SerializedName("question" ) var question : String? = null,
  @SerializedName("id"       ) var id       : String? = null

)