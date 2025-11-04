package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Posts (

    @SerializedName("_id"               ) var Id                : String?              = null,
    @SerializedName("feelings"          ) var feelings          : String?              = null,
    @SerializedName("businessProfileID" ) var businessProfileID : String?              = null,
    @SerializedName("postType"          ) var postType          : String?              = null,
    @SerializedName("userID"            ) var userID            : String?              = null,
    @SerializedName("content"           ) var content           : String?              = null,
    @SerializedName("location"          ) var location          : Location?            = Location(),
    @SerializedName("createdAt"         ) var createdAt         : String?              = null,
    @SerializedName("mediaRef"          ) var mediaRef          : ArrayList<MediaRef>  = arrayListOf(),
    @SerializedName("taggedRef"         ) var taggedRef         : ArrayList<TaggedRef> = arrayListOf(),
    @SerializedName("postedBy"          ) var postedBy          : PostedBy?            = PostedBy(),
    @SerializedName("likes"             ) var likes             : Int?                 = null,
    @SerializedName("comments"          ) var comments          : Int?                 = null,
    @SerializedName("shared"            ) var shared            : Int?                 = null,
    @SerializedName("interestedPeople"  ) var interestedPeople  : Int?                 = null,
    @SerializedName("likedByMe"         ) var likedByMe         : Boolean?             = null,
    @SerializedName("savedByMe"         ) var savedByMe         : Boolean?             = null,
    @SerializedName("imJoining"         ) var imJoining         : Boolean?             = null

)