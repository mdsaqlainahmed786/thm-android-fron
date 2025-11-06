package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Custom deserializer for collaborators field that handles:
 * 1. Array of objects: [{_id: "...", name: "...", profilePic: {...}}]
 * 2. Array of strings: ["userID1", "userID2"] - converts to Collaborator objects with just _id
 * 3. Null or missing field
 */
class CollaboratorListTypeAdapter : JsonDeserializer<ArrayList<Collaborator>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ArrayList<Collaborator>? {
        if (json == null || json.isJsonNull) {
            return null
        }

        if (!json.isJsonArray) {
            return null
        }

        val result = ArrayList<Collaborator>()
        val jsonArray = json.asJsonArray

        for (element in jsonArray) {
            try {
                when {
                    // Case 1: It's a string (user ID) - convert to Collaborator with just _id
                    element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                        val userId = element.asString
                        if (userId.isNotEmpty()) {
                            result.add(Collaborator(_id = userId, name = null, profilePic = null))
                        }
                    }
                    // Case 2: It's an object (expected format with full data)
                    element.isJsonObject -> {
                        val collaborator = context?.deserialize<Collaborator>(
                            element,
                            Collaborator::class.java
                        )
                        if (collaborator != null) {
                            result.add(collaborator)
                        }
                    }
                    // Case 3: Other types - skip
                    else -> {
                        // Skip malformed entries
                    }
                }
            } catch (e: Exception) {
                // Skip malformed entries to prevent crashes
                continue
            }
        }

        return if (result.isEmpty()) null else result
    }
}

