package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class MediaRef (

  @SerializedName("_id"               ) var Id                : String? = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("mediaType"         ) var mediaType         : String? = null,
  @SerializedName("mimeType"          ) var mimeType          : String? = null,
  @SerializedName("sourceUrl"         ) var sourceUrl         : String? = null,
  @SerializedName("thumbnailUrl"      ) var thumbnailUrl      : String? = null,

)