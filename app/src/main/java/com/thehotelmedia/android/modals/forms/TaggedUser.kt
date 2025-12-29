package com.thehotelmedia.android.modals.forms

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing a tagged user in a story
 * @param userTagged Name of the tagged user
 * @param userTaggedId ID of the tagged user
 * @param positionX X position (normalized 0.0-1.0 or pixel value)
 * @param positionY Y position (normalized 0.0-1.0 or pixel value)
 */
data class TaggedUser(
    @SerializedName("userTagged") val userTagged: String,
    @SerializedName("userTaggedId") val userTaggedId: String,
    @SerializedName("positionX") val positionX: Float,
    @SerializedName("positionY") val positionY: Float
) : Serializable

