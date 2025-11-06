package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName

data class Collaborator(
    @SerializedName("_id") val _id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("profilePic") val profilePic: CollaboratorProfilePic? = null
)

data class CollaboratorProfilePic(
    @SerializedName("small") var small: String? = null,
    @SerializedName("medium") var medium: String? = null,
    @SerializedName("large") var large: String? = null
)


