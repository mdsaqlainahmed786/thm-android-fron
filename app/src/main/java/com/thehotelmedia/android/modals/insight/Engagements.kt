package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Engagements (

    @SerializedName("labelName"  ) var labelName  : String? = null,
    @SerializedName("engagement" ) var engagement : Int?    = null

)
