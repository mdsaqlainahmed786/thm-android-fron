package com.thehotelmedia.android.modals.storiesActions.likeStory

import com.google.gson.annotations.SerializedName


data class BusinessProfileRef (

    @SerializedName("_id"        ) var Id         : String?     = null,
    @SerializedName("profilePic" ) var profilePic : ProfilePic? = ProfilePic(),
    @SerializedName("username"   ) var username   : String?     = null,
    @SerializedName("name"       ) var name       : String?     = null

)
