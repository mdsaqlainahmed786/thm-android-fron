package com.thehotelmedia.android.modals.getProfessions

import com.google.gson.annotations.SerializedName


data class GetProfessionModal (

  @SerializedName("status"     ) var status     : Boolean?        = null,
  @SerializedName("statusCode" ) var statusCode : Int?            = null,
  @SerializedName("message"    ) var message    : String?         = null,
  @SerializedName("data"       ) var professionData       : ArrayList<ProfessionData> = arrayListOf()

)