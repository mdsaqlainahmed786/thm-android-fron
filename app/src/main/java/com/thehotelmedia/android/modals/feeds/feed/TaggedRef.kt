package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.storiesActions.likeStory.BusinessProfileRef

data class TaggedRef(
    @SerializedName("_id"         ) var Id          : String?     = null,
    @SerializedName("accountType" ) var accountType : String?     = null,
    @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
    @SerializedName("name"        ) var name        : String?     = null,
    @SerializedName("username"        ) var username        : String?     = null,
    @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef()
)
