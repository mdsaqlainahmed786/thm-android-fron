package com.thehotelmedia.android.utils.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Some backend deployments sometimes return an object instead of a plain string, e.g.:
 * - "businessProfileID": "abc123"
 * - "businessProfileID": { "_id": "abc123", ... }
 *
 * This adapter safely extracts a string from either shape.
 */
class StringOrObjectIdAdapter : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) return null

        return when {
            json.isJsonPrimitive -> json.asString
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val candidates = listOf("_id", "id", "value", "name")
                for (key in candidates) {
                    if (obj.has(key) && obj.get(key).isJsonPrimitive) {
                        return obj.get(key).asString
                    }
                }
                // Fallback: keep as JSON string to avoid hard crashes.
                obj.toString()
            }
            else -> json.toString()
        }
    }
}





















