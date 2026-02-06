package com.thehotelmedia.android.modals.storiesActions.publishStory

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class StoryPublishData(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("userID") val userID: String? = null,
    @SerializedName("mediaID") val mediaID: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

// Custom deserializer for PublishStoryModal to handle data field as both array and single object
class PublishStoryModalDeserializer : JsonDeserializer<PublishStoryModal> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PublishStoryModal? {
        if (json == null || !json.isJsonObject) {
            return null
        }

        val jsonObject = json.asJsonObject
        val status = if (jsonObject.has("status")) {
            jsonObject.get("status")?.asJsonPrimitive?.asBoolean
        } else null

        val statusCode = if (jsonObject.has("statusCode")) {
            jsonObject.get("statusCode")?.asJsonPrimitive?.asInt
        } else null

        val message = if (jsonObject.has("message")) {
            jsonObject.get("message")?.asJsonPrimitive?.asString
        } else null

        // Handle data field - can be array or single object
        val data: List<StoryPublishData>? = if (jsonObject.has("data")) {
            val dataElement = jsonObject.get("data")
            when {
                dataElement.isJsonNull -> null
                dataElement.isJsonArray -> {
                    // data is an array
                    val list = mutableListOf<StoryPublishData>()
                    dataElement.asJsonArray.forEach { element ->
                        context?.deserialize<StoryPublishData>(element, StoryPublishData::class.java)?.let {
                            list.add(it)
                        }
                    }
                    list
                }
                dataElement.isJsonObject -> {
                    // data is a single object (for videos), wrap it in a list
                    val item = context?.deserialize<StoryPublishData>(dataElement, StoryPublishData::class.java)
                    if (item != null) {
                        listOf(item)
                    } else {
                        null
                    }
                }
                else -> null
            }
        } else null

        return PublishStoryModal(
            status = status,
            statusCode = statusCode,
            message = message,
            data = data
        )
    }
}

@JsonAdapter(PublishStoryModalDeserializer::class)
data class PublishStoryModal(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<StoryPublishData>? = null
)

