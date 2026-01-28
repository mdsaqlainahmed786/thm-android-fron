package com.thehotelmedia.android.utils.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Backend can return:
 * - "amenities": ["WiFi", "Parking"]
 * - "amenities": [{ "name": "WiFi" }, { "_id": "..." }]
 *
 * This adapter normalizes everything into a list of strings.
 */
class StringListFlexibleAdapter : JsonDeserializer<ArrayList<String>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ArrayList<String> {
        if (json == null || json.isJsonNull) return arrayListOf()

        fun extractFromObject(obj: JsonObject): String? {
            val candidates = listOf("name", "_id", "id", "value")
            for (key in candidates) {
                if (obj.has(key) && obj.get(key).isJsonPrimitive) return obj.get(key).asString
            }
            return null
        }

        return when {
            json.isJsonArray -> {
                val out = arrayListOf<String>()
                for (el in json.asJsonArray) {
                    when {
                        el.isJsonNull -> Unit
                        el.isJsonPrimitive -> out.add(el.asString)
                        el.isJsonObject -> extractFromObject(el.asJsonObject)?.let(out::add)
                        else -> Unit
                    }
                }
                out
            }
            json.isJsonPrimitive -> arrayListOf(json.asString)
            json.isJsonObject -> extractFromObject(json.asJsonObject)?.let { arrayListOf(it) } ?: arrayListOf()
            else -> arrayListOf()
        }
    }
}











