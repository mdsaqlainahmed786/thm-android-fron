package com.thehotelmedia.android.modals.search

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.BusinessProfileRef
import com.thehotelmedia.android.modals.search.ProfilePic


data class PostedBy (


    @SerializedName("_id"                ) var Id                 : String?             = null,
    @SerializedName("accountType"        ) var accountType        : String?             = null,
    @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
    @SerializedName("name"               ) var name               : String?             = null,
    @SerializedName("profilePic"         ) var profilePic         : ProfilePic?         = ProfilePic(),
    @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()

)