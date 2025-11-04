package com.thehotelmedia.android.modals.collaboration


data class CollaborationActionModal(
    val status: Boolean = false,
    val message: String = ""
)


data class CollaborationUser(
    val _id: String? = null,
    val name: String? = null,
    val profilePic: String? = null
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
