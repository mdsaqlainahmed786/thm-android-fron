package com.thehotelmedia.android.modals.profileData.profile

import com.google.gson.annotations.SerializedName

data class BusinessAnswerRef (

    @SerializedName("_id"        ) var Id         : String? = null,
    @SerializedName("questionID" ) var questionID : String? = null,
    @SerializedName("answer"     ) var answer     : String? = null

)
