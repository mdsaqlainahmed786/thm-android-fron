package com.thehotelmedia.android.modals.checkinData.checkInData

import com.google.gson.annotations.SerializedName


data class BusinessProfileRef (

    @SerializedName("coverImage"        ) var coverImage        : String?     = null,
    @SerializedName("profilePic"        ) var profilePic        : ProfilePic? = ProfilePic(),
    @SerializedName("businessTypeID"    ) var businessTypeID    : String?     = null,
    @SerializedName("businessSubTypeID" ) var businessSubTypeID : String?     = null,
    @SerializedName("name"              ) var name              : String?     = null,
    @SerializedName("address"           ) var address           : Address?    = Address(),
    @SerializedName("id"                ) var id                : String?     = null,
    @SerializedName("type"              ) var type              : String?     = null

)