package com.thehotelmedia.android.SocketModals.privateMessage

import com.google.gson.annotations.SerializedName

data class DeleteMessageResponse(
    @SerializedName("messageID") val messageID: String,
    @SerializedName("_id") val _id: String? = null,
    @SerializedName("clientMessageID") val clientMessageID: String? = null,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("isDeleted") val isDeleted: Boolean? = true
)


