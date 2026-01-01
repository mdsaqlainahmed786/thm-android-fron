package com.thehotelmedia.android.SocketModals.privateMessage

import com.google.gson.annotations.SerializedName

data class EditMessageResponse(
    @SerializedName("messageID") val messageID: String,
    @SerializedName("_id") val _id: String? = null,
    @SerializedName("message") val message: String,
    @SerializedName("isEdited") val isEdited: Boolean = true,
    @SerializedName("editedAt") val editedAt: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String
)


