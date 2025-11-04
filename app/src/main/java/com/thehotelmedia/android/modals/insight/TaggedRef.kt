package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.insight.ProfilePic


data class TaggedRef (

    @SerializedName("_id"         ) var Id          : String?     = null,
    @SerializedName("accountType" ) var accountType : String?     = null,
    @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
    @SerializedName("name"        ) var name        : String?     = null

)