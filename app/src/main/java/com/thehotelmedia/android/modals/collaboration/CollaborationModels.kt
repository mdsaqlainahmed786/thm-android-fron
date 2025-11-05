package com.thehotelmedia.android.modals.collaboration

import com.google.gson.annotations.SerializedName


data class CollaborationActionModal(
    val status: Boolean = false,
    val message: String = ""
)


data class CollaborationProfilePic(
    @SerializedName("small") var small: String? = null,
    @SerializedName("medium") var medium: String? = null,
    @SerializedName("large") var large: String? = null
)


data class CollaborationUser(
    @SerializedName("_id") val _id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("profilePic") val profilePic: CollaborationProfilePic? = null
)


data class CollaborationPostItem(
    val _id: String? = null,
    val userID: CollaborationUser? = null,
    val collaborators: List<CollaborationUser> = emptyList()
)


data class CollaborationPostsModal(
    val status: Boolean = false,
    val message: String = "",
    val data: List<CollaborationPostItem> = emptyList()
)


data class CollaboratorsListModal(
    val status: Boolean = false,
    val message: String = "",
    val data: List<CollaborationUser> = emptyList()
)
