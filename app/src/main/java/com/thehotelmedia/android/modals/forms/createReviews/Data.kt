package com.thehotelmedia.android.modals.forms.createReviews

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("isPublished"               ) var isPublished               : Boolean?          = null,
  @SerializedName("media"                     ) var media                     : ArrayList<String> = arrayListOf(),
  @SerializedName("tagged"                    ) var tagged                    : ArrayList<String> = arrayListOf(),
  @SerializedName("feelings"                  ) var feelings                  : String?           = null,
  @SerializedName("reviews"                   ) var reviews                   : ArrayList<Reviews> = arrayListOf(),
  @SerializedName("postType"                  ) var postType                  : String?           = null,
  @SerializedName("userID"                    ) var userID                    : String?           = null,
  @SerializedName("content"                   ) var content                   : String?           = null,
  @SerializedName("reviewedBusinessProfileID" ) var reviewedBusinessProfileID : String?           = null,
  @SerializedName("location"                  ) var location                  : String?           = null,
  @SerializedName("placeID"                   ) var placeID                   : String?           = null,
  @SerializedName("rating"                    ) var rating                    : Double?           = null,
  @SerializedName("createdAt"                 ) var createdAt                 : String?           = null,
  @SerializedName("updatedAt"                 ) var updatedAt                 : String?           = null,
  @SerializedName("__v"                       ) var _v                        : Int?              = null,
  @SerializedName("id"                        ) var id                        : String?           = null

)