package com.thehotelmedia.android.modals.forms.createPost

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("isPublished" ) var isPublished : Boolean?          = null,
  @SerializedName("media"       ) var media       : ArrayList<String> = arrayListOf(),
  @SerializedName("tagged"      ) var tagged      : ArrayList<String> = arrayListOf(),
  @SerializedName("feelings"    ) var feelings    : String?           = null,
  @SerializedName("postType"    ) var postType    : String?           = null,
  @SerializedName("userID"      ) var userID      : String?           = null,
  @SerializedName("content"     ) var content     : String?           = null,
  @SerializedName("location"    ) var location    : Location?         = Location(),
  @SerializedName("createdAt"   ) var createdAt   : String?           = null,
  @SerializedName("updatedAt"   ) var updatedAt   : String?           = null,
  @SerializedName("__v"         ) var _v          : Int?              = null,
  @SerializedName("id"          ) var id          : String?           = null

)