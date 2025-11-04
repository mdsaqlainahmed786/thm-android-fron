package com.thehotelmedia.android.modals.chat.exportChat

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("filename" ) var filename : String? = null,
  @SerializedName("filepath" ) var filePath : String? = null,
  @SerializedName("type"     ) var type     : String? = null

)