package com.thehotelmedia.android.SocketModals.privateMessage

import com.google.gson.annotations.SerializedName


data class PrivateMessageModal (

    @SerializedName("message" ) var message : Message? = Message(),
    @SerializedName("from"    ) var from    : String?  = null,
    @SerializedName("to"      ) var to      : String?  = null,
    @SerializedName("time"    ) var time    : String?  = null,
    @SerializedName("isSeen"  ) var isSeen  : Boolean? = null

)