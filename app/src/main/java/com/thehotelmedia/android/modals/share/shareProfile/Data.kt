package com.thehotelmedia.android.modals.share.shareProfile

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.share.shareProfile.BusinessProfileRef


data class Data (

    @SerializedName("posts"               ) var posts               : Int?                = null,
    @SerializedName("follower"            ) var follower            : Int?                = null,
    @SerializedName("following"           ) var following           : Int?                = null,
    @SerializedName("_id"                 ) var Id                  : String?             = null,
    @SerializedName("bio"                 ) var bio                 : String?             = null,
    @SerializedName("accountType"         ) var accountType         : String?             = null,
    @SerializedName("privateAccount"      ) var privateAccount      : Boolean?            = null,
    @SerializedName("notificationEnabled" ) var notificationEnabled : Boolean?            = null,
    @SerializedName("role"                ) var role                : String?             = null,
    @SerializedName("username"            ) var username            : String?             = null,
    @SerializedName("name"                ) var name                : String?             = null,
    @SerializedName("businessProfileID"   ) var businessProfileID   : String?             = null,
    @SerializedName("lastSeen"            ) var lastSeen            : String?             = null,
    @SerializedName("businessProfileRef"  ) var businessProfileRef  : BusinessProfileRef? = BusinessProfileRef(),
    @SerializedName("isConnected"         ) var isConnected         : Boolean?            = null,
    @SerializedName("isRequested"         ) var isRequested         : Boolean?            = null,
    @SerializedName("isBlockedByMe"       ) var isBlockedByMe       : Boolean?            = null

)