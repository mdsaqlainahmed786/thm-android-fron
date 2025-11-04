package com.thehotelmedia.android.modals.helpAndSupport.faqs

import com.google.gson.annotations.SerializedName


data class FAQsData (

  @SerializedName("_id"      ) var Id       : String? = null,
  @SerializedName("question" ) var question : String? = null,
  @SerializedName("answer"   ) var answer   : String? = null

)