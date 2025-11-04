package com.thehotelmedia.android.modals.job.jobDetails

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.SocketModals.chatScreen.ProfilePic

data class BusinessProfileRef (

    @SerializedName("_id"        ) var Id         : String?     = null,
    @SerializedName("profilePic" ) var profilePic : ProfilePic? = ProfilePic(),
    @SerializedName("username"   ) var username   : String?     = null,
    @SerializedName("name"       ) var name       : String?     = null

)
