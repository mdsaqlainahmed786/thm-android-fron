package com.thehotelmedia.android.modals.Stories

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.Stories.BusinessProfileRef
import kotlinx.parcelize.Parcelize

data class Stories (

    @SerializedName("_id"                ) var id                 : String?               = null,
    @SerializedName("accountType"        ) var accountType        : String?               = null,
    @SerializedName("username"           ) var username           : String?               = null,
    @SerializedName("name"               ) var name               : String?               = null,
    @SerializedName("profilePic"         ) var profilePic         : ProfilePic?           = ProfilePic(),
    @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef?   = BusinessProfileRef(),
    @SerializedName("storiesRef"         ) var storiesRef         : ArrayList<StoriesRef> = arrayListOf(),
    @SerializedName("seenByMe"           ) var seenByMe           : Boolean?              = null
)