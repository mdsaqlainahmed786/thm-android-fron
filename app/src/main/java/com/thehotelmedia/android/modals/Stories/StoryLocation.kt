package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName

/**
 * Location data class for Stories.
 * Backend returns lat/lng as numbers (Double), not strings.
 */
data class StoryLocation(
    @SerializedName("lat")
    val lat: Double?,

    @SerializedName("lng")
    val lng: Double?,

    @SerializedName("placeName")
    val placeName: String?
)


