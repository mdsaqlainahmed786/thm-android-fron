package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName


data class Data (

    @SerializedName("_id"                        ) var Id                         : String?                     = null,
    @SerializedName("isPublished"                ) var isPublished                : Boolean?                    = null,
    @SerializedName("feelings"                   ) var feelings                   : String?                     = null,
    @SerializedName("reviews"                    ) var reviews                    : ArrayList<Reviews>          = arrayListOf(),
    @SerializedName("businessProfileID"          ) var businessProfileID          : String?                     = null,
    @SerializedName("postType"                   ) var postType                   : String?                     = null,
    @SerializedName("data"                       ) var suggestionData             : ArrayList<SuggestionData>   = arrayListOf(),
    @SerializedName("userID"                     ) var userID                     : String?                     = null,
    @SerializedName("content"                    ) var content                    : String?                     = null,
    @SerializedName("name"                       ) var name                       : String?                     = null,
    @SerializedName("venue"                      ) var venue                      : String?                     = null,
    @SerializedName("startDate"                  ) var startDate                  : String?                     = null,
    @SerializedName("startTime"                  ) var startTime                  : String?                     = null,
    @SerializedName("endDate"                    ) var endDate                    : String?                     = null,
    @SerializedName("endTime"                    ) var endTime                    : String?                     = null,
    @SerializedName("type"                       ) var type                       : String?                     = null,
    @SerializedName("streamingLink"              ) var streamingLink              : String?                     = null,
    @SerializedName("location"                   ) var location                   : Location?                   = Location(),
    @SerializedName("createdAt"                  ) var createdAt                  : String?                     = null,
    @SerializedName("mediaRef"                   ) var mediaRef                   : ArrayList<MediaRef>         = arrayListOf(),
    @SerializedName("taggedRef"                  ) var taggedRef                  : ArrayList<TaggedRef>        = arrayListOf(),
    @SerializedName("postedBy"                   ) var postedBy                   : PostedBy?                   = PostedBy(),
    @SerializedName("rating"                     ) var rating                     : Double?                     = null,
    @SerializedName("shared"                     ) var shared                     : Int?                        = null,
    @SerializedName("likes"                      ) var likes                      : Int?                        = null,
    @SerializedName("views"                      ) var views                      : Int?                        = null,
    @SerializedName("googleReviewedBusiness"     ) var googleReviewedBusiness     : String?                     = null,
    @SerializedName("publicUserID"               ) var publicUserID               : String?                     = null,
    @SerializedName("comments"                   ) var comments                   : Int?                        = null,
    @SerializedName("reviewedBusinessProfileRef" ) var reviewedBusinessProfileRef : ReviewedBusinessProfileRef? = ReviewedBusinessProfileRef(),
    @SerializedName("interestedPeople"           ) var interestedPeople           : Int?                        = null,
    @SerializedName("likedByMe"                  ) var likedByMe                  : Boolean?                    = null,
    @SerializedName("savedByMe"                  ) var savedByMe                  : Boolean?                    = null,
    @SerializedName("imJoining"                  ) var imJoining                  : Boolean?                    = null

)