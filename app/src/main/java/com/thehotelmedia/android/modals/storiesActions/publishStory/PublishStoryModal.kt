package com.thehotelmedia.android.modals.storiesActions.publishStory

import com.google.gson.annotations.SerializedName

data class PublishStoryModal(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<StoryPublishData>? = null
)

data class StoryPublishData(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("userID") val userID: String? = null,
    @SerializedName("mediaID") val mediaID: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

