package com.thehotelmedia.android.modals.forms.createEvent

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("isPublished"   ) var isPublished   : Boolean?          = null,
  @SerializedName("media"         ) var media         : ArrayList<String> = arrayListOf(),
  @SerializedName("tagged"        ) var tagged        : ArrayList<String> = arrayListOf(),
  @SerializedName("feelings"      ) var feelings      : String?           = null,
  @SerializedName("_id"           ) var id            : String?           = null,
  @SerializedName("reviews"       ) var reviews       : ArrayList<String> = arrayListOf(),
  @SerializedName("postType"      ) var postType      : String?           = null,
  @SerializedName("userID"        ) var userID        : String?           = null,
  @SerializedName("content"       ) var content       : String?           = null,
  @SerializedName("name"          ) var name          : String?           = null,
  @SerializedName("venue"         ) var venue         : String?           = null,
  @SerializedName("streamingLink" ) var streamingLink : String?           = null,
  @SerializedName("time"          ) var time          : String?           = null,
  @SerializedName("date"          ) var date          : String?           = null,
  @SerializedName("type"          ) var type          : String?           = null,
  @SerializedName("location"      ) var location      : Location?         = Location(),
  @SerializedName("createdAt"     ) var createdAt     : String?           = null,
  @SerializedName("updatedAt"     ) var updatedAt     : String?           = null,
  @SerializedName("__v"           ) var _v            : Int?              = null

)