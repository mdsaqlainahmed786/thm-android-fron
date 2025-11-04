package com.thehotelmedia.android.modals.viewPostEvent

import com.google.gson.annotations.SerializedName


data class EventJoinsRef (

    @SerializedName("userID"     ) var userID     : String?     = null,
    @SerializedName("profilePic" ) var profilePic : ProfilePic? = ProfilePic()

)
