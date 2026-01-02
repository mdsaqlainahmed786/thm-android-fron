package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.feed.PostedBy

/**
 * Represents a tagged user in a story
 * Matches the backend API response structure
 */
data class TaggedUser(
    @SerializedName("userTagged") var userTagged: String? = null,
    @SerializedName("userTaggedId") var userTaggedId: String? = null,
    @SerializedName("positionX") var positionX: Float? = null,  // Pixel position
    @SerializedName("positionY") var positionY: Float? = null,  // Pixel position
    @SerializedName("user") var user: PostedBy? = null // Populated user details
)

