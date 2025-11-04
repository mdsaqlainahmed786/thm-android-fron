package com.thehotelmedia.android.modals.viewPostEvent

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.Reviews
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.modals.forms.createEvent.Location


data class Data (

  @SerializedName("_id"              ) var Id               : String?             = null,
  @SerializedName("feelings"         ) var feelings         : String?             = null,
  @SerializedName("postType"         ) var postType         : String?             = null,
  @SerializedName("userID"           ) var userID           : String?             = null,
  @SerializedName("content"          ) var content          : String?             = null,
  @SerializedName("name"             ) var name             : String?             = null,
  @SerializedName("venue"            ) var venue            : String?             = null,
  @SerializedName("streamingLink"    ) var streamingLink    : String?             = null,
  @SerializedName("startDate"        ) var startDate        : String?             = null,
  @SerializedName("startTime"        ) var startTime        : String?             = null,
  @SerializedName("endDate"          ) var endDate          : String?             = null,
  @SerializedName("endTime"          ) var endTime          : String?             = null,
  @SerializedName("type"             ) var type             : String?             = null,
  @SerializedName("location"         ) var location         : Location?           = Location(),
  @SerializedName("createdAt"        ) var createdAt        : String?             = null,
  @SerializedName("mediaRef"         ) var mediaRef         : ArrayList<MediaRef> = arrayListOf(),
  @SerializedName("taggedRef"        ) var taggedRef        : ArrayList<String>   = arrayListOf(),
  @SerializedName("postedBy"         ) var postedBy         : PostedBy?           = PostedBy(),
  @SerializedName("likes"            ) var likes            : Int?                = null,
  @SerializedName("comments"         ) var comments         : Int?                = null,
  @SerializedName("shared"           ) var shared           : Int?                = null,
  @SerializedName("reviewedBusinessProfileRef" ) var reviewedBusinessProfileRef : ReviewedBusinessProfileRef? = ReviewedBusinessProfileRef(),
  @SerializedName("likedByMe"        ) var likedByMe        : Boolean?            = null,
  @SerializedName("savedByMe"        ) var savedByMe        : Boolean?            = null,
  @SerializedName("imJoining"        ) var imJoining        : Boolean?            = null,
  @SerializedName("eventJoinsRef"    ) var eventJoinsRef    : ArrayList<EventJoinsRef> = arrayListOf(),
  @SerializedName("interestedPeople" ) var interestedPeople : Int?                = null,


  @SerializedName("isPublished"                ) var isPublished                : Boolean?                    = null,
  @SerializedName("reviews"                    ) var reviews                    : ArrayList<Reviews>          = arrayListOf(),
  @SerializedName("businessProfileID"          ) var businessProfileID          : String?                     = null,
  @SerializedName("rating"                     ) var rating                     : Double?                     = null

)