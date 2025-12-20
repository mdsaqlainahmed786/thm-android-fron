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
  @SerializedName("location"  ) var location  : StoryLocation? = null,
  @SerializedName("locationPositionX" ) var locationPositionX : Float? = null,  // Normalized x position (0.0-1.0)
  @SerializedName("locationPositionY" ) var locationPositionY : Float? = null,  // Normalized y position (0.0-1.0)
  @SerializedName("userTagged") var userTaggedName : String? = null,  // Name of tagged user
  @SerializedName("userTaggedId") var userTaggedId : String? = null,  // ID of tagged user
  @SerializedName("userTaggedPositionX" ) var userTaggedPositionX : Float? = null,  // Normalized x position (0.0-1.0)
  @SerializedName("userTaggedPositionY" ) var userTaggedPositionY : Float? = null   // Normalized y position (0.0-1.0)

)