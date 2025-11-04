package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class WebsiteRedirection (

    @SerializedName("redirection" ) var redirection : Int?    = null,
    @SerializedName("labelName"   ) var labelName   : String? = null

)
