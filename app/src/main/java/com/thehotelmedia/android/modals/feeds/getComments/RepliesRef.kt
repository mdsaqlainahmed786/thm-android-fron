package com.thehotelmedia.android.modals.feeds.getComments

import com.google.gson.annotations.SerializedName

data class RepliesRef (

    @SerializedName("_id"               ) var id                : String?      = null,
    @SerializedName("isParent"          ) var isParent          : Boolean?     = null,
    @SerializedName("userID"            ) var userID            : String?      = null,
    @SerializedName("businessProfileID" ) var businessProfileID : String?      = null,
    @SerializedName("postID"            ) var postID            : String?      = null,
    @SerializedName("message"           ) var message           : String?      = null,
    @SerializedName("parentID"          ) var parentID          : String?      = null,
    @SerializedName("createdAt"         ) var createdAt         : String?      = null,
    @SerializedName("commentedBy"       ) var commentedBy       : CommentedBy? = CommentedBy(),
    @SerializedName("likes"             ) var likes             : Int?         = null,
    @SerializedName("likedByMe"         ) var likedByMe         : Boolean?     = null

)
