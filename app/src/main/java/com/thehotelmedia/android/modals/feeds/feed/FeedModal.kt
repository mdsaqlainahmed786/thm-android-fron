package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName


data class FeedModal (

    @SerializedName("status"         ) var status         : Boolean?        = null,
    @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
    @SerializedName("message"        ) var message        : String?         = null,
    @SerializedName("data"           ) var data           : ArrayList<Data> = arrayListOf(),
    @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
    @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
    @SerializedName("totalResources" ) var totalResources : Int?            = null

)