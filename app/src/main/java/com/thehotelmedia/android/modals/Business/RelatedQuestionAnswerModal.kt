package com.thehotelmedia.android.modals.Business

import com.google.gson.annotations.SerializedName


data class RelatedQuestionAnswerModal (

  @SerializedName("question" ) var question : String?           = null,
  @SerializedName("answers"  ) var answers  : ArrayList<String> = arrayListOf()

)