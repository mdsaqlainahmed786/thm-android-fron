package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName

data class Reviews(
    @SerializedName("_id"       ) var Id        : String? = null,
    @SerializedName("mediaType" ) var mediaType : String? = null,
    @SerializedName("mimeType"  ) var mimeType  : String? = null,
    @SerializedName("sourceUrl" ) var sourceUrl : String? = null
)
