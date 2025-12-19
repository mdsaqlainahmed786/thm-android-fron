package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName


data class MyStories (

  @SerializedName("_id"       ) var Id        : String?           = null,
  @SerializedName("duration"  ) var duration  : Int?               = null,
  @SerializedName("mediaID"   ) var mediaID   : String?           = null,
  @SerializedName("createdAt" ) var createdAt : String?           = null,
  @SerializedName("mimeType"  ) var mimeType  : String?           = null,
  @SerializedName("sourceUrl" ) var sourceUrl : String?           = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String?     = null,
  @SerializedName("likesRef"  ) var likesRef  : ArrayList<LikesRef> = arrayListOf(),
  @SerializedName("viewsRef"  ) var viewsRef  : ArrayList<LikesRef> = arrayListOf(),
  @SerializedName("likes"     ) var likes     : Int?                = null,
  @SerializedName("views"     ) var views     : Int?                = null,
  @SerializedName("taggedRef" ) var taggedRef : ArrayList<com.thehotelmedia.android.modals.feeds.feed.TaggedRef> = arrayListOf(),
  @SerializedName("location"  ) var location  : StoryLocation? = null

)