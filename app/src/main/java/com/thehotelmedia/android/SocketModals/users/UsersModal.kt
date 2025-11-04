package com.thehotelmedia.android.SocketModals.users

import com.google.gson.annotations.SerializedName


data class UsersModal (

    @SerializedName("profilePic" ) var profilePic : ProfilePic? = ProfilePic(),
    @SerializedName("username"   ) var username   : String?     = null,
    @SerializedName("name"       ) var name       : String?     = null,
    @SerializedName("isOnline"   ) var isOnline   : Boolean?    = null,
    @SerializedName("_id"        ) var Id         : String?     = null

)