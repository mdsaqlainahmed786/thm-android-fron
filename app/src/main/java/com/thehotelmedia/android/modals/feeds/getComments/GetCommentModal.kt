package com.thehotelmedia.android.modals.feeds.getComments

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.getComments.Data


data class GetCommentModal (

    @SerializedName("status"         ) var status         : Boolean?        = null,
    @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
    @SerializedName("message"        ) var message        : String?         = null,
    @SerializedName("data"           ) var data           : ArrayList<Data> = arrayListOf(),
    @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
    @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
    @SerializedName("totalResources" ) var totalResources : Int?            = null

)