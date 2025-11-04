package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Stories (

    @SerializedName("_id"       ) var Id        : String? = null,
    @SerializedName("mediaID"   ) var mediaID   : String? = null,
    @SerializedName("createdAt" ) var createdAt : String? = null,
    @SerializedName("mimeType"  ) var mimeType  : String? = null,
    @SerializedName("sourceUrl" ) var sourceUrl : String? = null,
    @SerializedName("thumbnailUrl" ) var thumbnailUrl : String? = null

)
