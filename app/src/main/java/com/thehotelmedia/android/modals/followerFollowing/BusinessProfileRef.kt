package com.thehotelmedia.android.modals.followerFollowing

import com.google.gson.annotations.SerializedName

data class BusinessProfileRef (

    @SerializedName("profilePic"      ) var profilePic      : ProfilePic?      = ProfilePic(),
    @SerializedName("name"            ) var name            : String?          = null,
    @SerializedName("address"         ) var address         : Address?         = Address(),
    @SerializedName("businessTypeRef" ) var businessTypeRef : BusinessTypeRef? = BusinessTypeRef()

)
