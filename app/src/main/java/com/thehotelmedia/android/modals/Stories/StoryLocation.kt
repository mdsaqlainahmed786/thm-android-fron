package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName

/**
 * Location data class for Stories.
 * Backend returns lat/lng as numbers (Double), not strings.
 * Note: Position coordinates (x/y) are stored at the root level of the story object
 * as locationPositionX and locationPositionY, not in this location object.
 */
data class StoryLocation(
    @SerializedName("lat")
    val lat: Double?,

    @SerializedName("lng")
    val lng: Double?,

    @SerializedName("placeName")
    val placeName: String?
)



