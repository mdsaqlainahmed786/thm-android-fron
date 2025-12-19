package com.thehotelmedia.android.modals.forms.createPost

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type


@JsonAdapter(LocationDeserializer::class)
data class Location (

  @SerializedName("lat"       ) var lat       : Any? = null,  // Changed to Any to accept both String and Double
  @SerializedName("lng"       ) var lng       : Any? = null,  // Changed to Any to accept both String and Double
  @SerializedName("placeName" ) var placeName : String? = null

) {
    // Custom getter to handle string-to-double conversion
    // Handles both numeric types (Double, Float, Int, Long, Number) and String types
    fun getLatAsDouble(): Double? {
        return when (val latValue = lat) {
            is Double -> {
                android.util.Log.d("Location", "lat is Double: $latValue")
                latValue
            }
            is Float -> {
                android.util.Log.d("Location", "lat is Float: $latValue, converting to Double")
                latValue.toDouble()
            }
            is Int -> {
                android.util.Log.d("Location", "lat is Int: $latValue, converting to Double")
                latValue.toDouble()
            }
            is Long -> {
                android.util.Log.d("Location", "lat is Long: $latValue, converting to Double")
                latValue.toDouble()
            }
            is Number -> {
                android.util.Log.d("Location", "lat is Number: $latValue (${latValue.javaClass.simpleName}), converting to Double")
                latValue.toDouble()
            }
            is String -> {
                android.util.Log.d("Location", "lat is String: '$latValue', parsing to Double")
                latValue.toDoubleOrNull()
            }
            null -> {
                android.util.Log.w("Location", "lat is null")
                null
            }
            else -> {
                android.util.Log.w("Location", "Unexpected lat type: ${latValue.javaClass.simpleName}, value: $latValue")
                // Try to convert via string as last resort
                latValue.toString().toDoubleOrNull()
            }
        }
    }
    
    fun getLngAsDouble(): Double? {
        return when (val lngValue = lng) {
            is Double -> {
                android.util.Log.d("Location", "lng is Double: $lngValue")
                lngValue
            }
            is Float -> {
                android.util.Log.d("Location", "lng is Float: $lngValue, converting to Double")
                lngValue.toDouble()
            }
            is Int -> {
                android.util.Log.d("Location", "lng is Int: $lngValue, converting to Double")
                lngValue.toDouble()
            }
            is Long -> {
                android.util.Log.d("Location", "lng is Long: $lngValue, converting to Double")
                lngValue.toDouble()
            }
            is Number -> {
                android.util.Log.d("Location", "lng is Number: $lngValue (${lngValue.javaClass.simpleName}), converting to Double")
                lngValue.toDouble()
            }
            is String -> {
                android.util.Log.d("Location", "lng is String: '$lngValue', parsing to Double")
                lngValue.toDoubleOrNull()
            }
            null -> {
                android.util.Log.w("Location", "lng is null")
                null
            }
            else -> {
                android.util.Log.w("Location", "Unexpected lng type: ${lngValue.javaClass.simpleName}, value: $lngValue")
                // Try to convert via string as last resort
                lngValue.toString().toDoubleOrNull()
            }
        }
    }
}

/**
 * Custom deserializer for Location to ensure proper parsing of lat/lng as numbers or strings
 */
class LocationDeserializer : JsonDeserializer<Location> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Location? {
        if (json == null || !json.isJsonObject) {
            android.util.Log.w("LocationDeserializer", "JSON is null or not an object")
            return null
        }

        val jsonObject = json.asJsonObject
        android.util.Log.d("LocationDeserializer", "Deserializing location: $jsonObject")

        val latElement = jsonObject.get("lat")
        val lngElement = jsonObject.get("lng")
        val placeNameElement = jsonObject.get("placeName")

        // Handle lat - can be number or string
        val lat: Any? = when {
            latElement == null || latElement.isJsonNull -> {
                android.util.Log.d("LocationDeserializer", "lat is null")
                null
            }
            latElement.isJsonPrimitive -> {
                val primitive = latElement.asJsonPrimitive
                when {
                    primitive.isNumber -> {
                        val num = primitive.asNumber
                        android.util.Log.d("LocationDeserializer", "lat is number: $num (${num.javaClass.simpleName})")
                        num.toDouble() // Convert to Double for consistency
                    }
                    primitive.isString -> {
                        val str = primitive.asString
                        android.util.Log.d("LocationDeserializer", "lat is string: '$str'")
                        str // Keep as string, getLatAsDouble() will parse it
                    }
                    else -> {
                        android.util.Log.w("LocationDeserializer", "lat is unexpected type")
                        null
                    }
                }
            }
            else -> {
                android.util.Log.w("LocationDeserializer", "lat is not a primitive")
                null
            }
        }

        // Handle lng - can be number or string
        val lng: Any? = when {
            lngElement == null || lngElement.isJsonNull -> {
                android.util.Log.d("LocationDeserializer", "lng is null")
                null
            }
            lngElement.isJsonPrimitive -> {
                val primitive = lngElement.asJsonPrimitive
                when {
                    primitive.isNumber -> {
                        val num = primitive.asNumber
                        android.util.Log.d("LocationDeserializer", "lng is number: $num (${num.javaClass.simpleName})")
                        num.toDouble() // Convert to Double for consistency
                    }
                    primitive.isString -> {
                        val str = primitive.asString
                        android.util.Log.d("LocationDeserializer", "lng is string: '$str'")
                        str // Keep as string, getLngAsDouble() will parse it
                    }
                    else -> {
                        android.util.Log.w("LocationDeserializer", "lng is unexpected type")
                        null
                    }
                }
            }
            else -> {
                android.util.Log.w("LocationDeserializer", "lng is not a primitive")
                null
            }
        }

        // Handle placeName - should be string
        val placeName: String? = when {
            placeNameElement == null || placeNameElement.isJsonNull -> {
                android.util.Log.d("LocationDeserializer", "placeName is null")
                null
            }
            placeNameElement.isJsonPrimitive && placeNameElement.asJsonPrimitive.isString -> {
                val str = placeNameElement.asString
                android.util.Log.d("LocationDeserializer", "placeName: '$str'")
                str
            }
            else -> {
                android.util.Log.w("LocationDeserializer", "placeName is not a string")
                null
            }
        }

        val location = Location(lat = lat, lng = lng, placeName = placeName)
        android.util.Log.d("LocationDeserializer", "Deserialized location: lat=$lat, lng=$lng, placeName=$placeName")
        return location
    }
}