package com.thehotelmedia.android.modals.search

import com.google.gson.annotations.SerializedName


data class SearchModel (

  @SerializedName("status"         ) var status         : Boolean?        = null,
  @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
  @SerializedName("message"        ) var message        : String?         = null,
  @SerializedName("data"           ) var searchData     : ArrayList<SearchData> = arrayListOf(),
  @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
  @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
  @SerializedName("totalResources" ) var totalResources : Int?            = null

)