package com.thehotelmedia.android.modals.Stories

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef

data class StoriesRef (

    @SerializedName("_id"       ) var Id        : String?  = null,
    @SerializedName("mediaID"   ) var mediaID   : String?  = null,
    @SerializedName("createdAt" ) var createdAt : String?  = null,
    @SerializedName("likedByMe" ) var likedByMe : Boolean? = null,
    @SerializedName("mimeType"  ) var mimeType  : String?  = null,
    @SerializedName("sourceUrl" ) var sourceUrl : String?  = null,
    @SerializedName("likesRef"  ) var likesRef  : ArrayList<LikesRef> = arrayListOf(),
    @SerializedName("viewsRef"  ) var viewsRef  : ArrayList<LikesRef> = arrayListOf(),
    @SerializedName("likes"     ) var likes     : Int?                = null,
    @SerializedName("views"     ) var views     : Int?                = null,
    @SerializedName("taggedRef" ) var taggedRef : ArrayList<TaggedRef> = arrayListOf(),
    @SerializedName("location"  ) var location  : StoryLocation? = null

)
