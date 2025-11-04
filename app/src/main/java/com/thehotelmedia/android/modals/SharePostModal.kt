package com.thehotelmedia.android.modals

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.Data

data class SharePostModal (

    @SerializedName("status"     ) var status     : Boolean? = null,
    @SerializedName("statusCode" ) var statusCode : Int?     = null,
    @SerializedName("message"    ) var message    : String?  = null,
    @SerializedName("data"       ) var data       : Data?    = Data()

)
