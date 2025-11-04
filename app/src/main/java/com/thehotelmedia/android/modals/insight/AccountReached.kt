package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class AccountReached (

    @SerializedName("accountReach" ) var accountReach : Int?    = null,
    @SerializedName("labelName"    ) var labelName    : String? = null

)
