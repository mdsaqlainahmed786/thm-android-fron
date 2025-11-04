package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class TotalFollowers (

    @SerializedName("followers" ) var followers : Int?    = null,
    @SerializedName("labelName" ) var labelName : String? = null

)
