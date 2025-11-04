package com.thehotelmedia.android.modals.search

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.Location
import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import com.thehotelmedia.android.modals.feeds.feed.ReviewedBusinessProfileRef
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.modals.followerFollowing.BusinessProfileRef
import com.thehotelmedia.android.modals.followerFollowing.ProfilePic


data class SearchData (

  @SerializedName("role"                        ) var role                       : String?             = null,
  @SerializedName("_id"                         ) var Id                         : String?             = null,
  @SerializedName("feelings"                    ) var feelings                   : String?             = null,
  @SerializedName("postType"                    ) var postType                   : String?             = null,
  @SerializedName("userID"                      ) var userID                     : String?             = null,
  @SerializedName("content"                     ) var content                    : String?             = null,
  @SerializedName("location"                    ) var location                   : Location?           = Location(),
  @SerializedName("createdAt"                   ) var createdAt                  : String?             = null,
  @SerializedName("mediaRef"                    ) var mediaRef                   : ArrayList<MediaRef> = arrayListOf(),
  @SerializedName("taggedRef"                   ) var taggedRef                  : ArrayList<TaggedRef>        = arrayListOf(),
  @SerializedName("postedBy"                    ) var postedBy                   : PostedBy?           = PostedBy(),
  @SerializedName("likes"                       ) var likes                      : Int?                = null,
  @SerializedName("comments"                    ) var comments                   : Int?                = null,
  @SerializedName("shared"                      ) var shared                     : Int?                = null,
  @SerializedName("views"                       ) var views                      : Int?                = null,
  @SerializedName("googleReviewedBusiness"      ) var googleReviewedBusiness     : String?             = null,
  @SerializedName("publicUserID"                ) var publicUserID               : String?             = null,
  @SerializedName("likedByMe"                   ) var likedByMe                  : Boolean?            = null,
  @SerializedName("savedByMe"                   ) var savedByMe                  : Boolean?            = null,
  @SerializedName("reviewedBusinessProfileRef"  ) var reviewedBusinessProfileRef : ReviewedBusinessProfileRef? = ReviewedBusinessProfileRef(),
  @SerializedName("rating"                      ) var rating                     : Double?                     = null,
  @SerializedName("accountType"                 ) var accountType                : String?              = null,
  @SerializedName("profilePic"                  ) var profilePic                 : ProfilePic?          = ProfilePic(),
  @SerializedName("username"                    ) var username                   : String?              = null,
  @SerializedName("name"                        ) var name                       : String?              = null,
  @SerializedName("businessProfileRef"          ) var businessProfileRef         : BusinessProfileRef?  = BusinessProfileRef(),
  @SerializedName("venue"                       ) var venue                      : String?              = null,
  @SerializedName("startDate"                   ) var startDate                  : String?              = null,
  @SerializedName("startTime"                   ) var startTime                  : String?              = null,
  @SerializedName("endDate"                     ) var endDate                    : String?              = null,
  @SerializedName("endTime"                     ) var endTime                    : String?              = null,
  @SerializedName("imJoining"                   ) var imJoining                  : Boolean?             = null,
  @SerializedName("interestedPeople"            ) var interestedPeople           : Int?                 = null,


  )