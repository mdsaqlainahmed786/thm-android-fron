package com.thehotelmedia.android.modals.job.jobDetails

import com.google.gson.annotations.SerializedName

data class PostedBy (

    @SerializedName("_id"                ) var Id                 : String?             = null,
    @SerializedName("accountType"        ) var accountType        : String?             = null,
    @SerializedName("username"           ) var username           : String?             = null,
    @SerializedName("name"               ) var name               : String?             = null,
    @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
    @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()

)
