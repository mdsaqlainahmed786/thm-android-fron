package com.thehotelmedia.android.modals.profileData.video

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"       ) var Id        : String? = null,
  @SerializedName("mediaType" ) var mediaType : String? = null,
  @SerializedName("mimeType"  ) var mimeType  : String? = null,
  @SerializedName("sourceUrl" ) var sourceUrl : String? = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String? = null,
  @SerializedName("views"     ) var views     : Int? = null

)