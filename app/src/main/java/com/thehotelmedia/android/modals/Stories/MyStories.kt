package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName


data class MyStories (

  @SerializedName("_id"       ) var Id        : String?           = null,
  @SerializedName("mediaID"   ) var mediaID   : String?           = null,
  @SerializedName("createdAt" ) var createdAt : String?           = null,
  @SerializedName("mimeType"  ) var mimeType  : String?           = null,
  @SerializedName("sourceUrl" ) var sourceUrl : String?           = null,
  @SerializedName("likesRef"  ) var likesRef  : ArrayList<LikesRef> = arrayListOf()

)