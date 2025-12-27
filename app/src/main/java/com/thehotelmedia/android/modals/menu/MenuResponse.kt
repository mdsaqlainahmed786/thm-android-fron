package com.thehotelmedia.android.modals.menu

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// Custom deserializer to handle both array and single object in data field
class MenuDataDeserializer : JsonDeserializer<ArrayList<MenuItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ArrayList<MenuItem> {
        if (json == null || json.isJsonNull) {
            return arrayListOf()
        }
        
        return when {
            json.isJsonArray -> {
                // data is an array
                val list = arrayListOf<MenuItem>()
                json.asJsonArray.forEach { element ->
                    context?.deserialize<MenuItem>(element, MenuItem::class.java)?.let {
                        list.add(it)
                    }
                }
                list
            }
            json.isJsonObject -> {
                // data is a single object, wrap it in an array
                val item = context?.deserialize<MenuItem>(json, MenuItem::class.java)
                if (item != null) {
                    arrayListOf(item)
                } else {
                    arrayListOf()
                }
            }
            else -> arrayListOf()
        }
    }
}

// Custom deserializer for MenuResponse to handle different response formats
class MenuResponseDeserializer : JsonDeserializer<MenuResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MenuResponse {
        if (json == null || json.isJsonNull) {
            return MenuResponse()
        }
        
        return when {
            json.isJsonArray -> {
                // Response is an array directly: [{...}, {...}]
                val items = arrayListOf<MenuItem>()
                json.asJsonArray.forEach { element ->
                    context?.deserialize<MenuItem>(element, MenuItem::class.java)?.let {
                        items.add(it)
                    }
                }
                MenuResponse(status = true, message = null, data = items)
            }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val status = obj.get("status")?.asBoolean
                val message = obj.get("message")?.asString
                val dataElement = obj.get("data")
                
                val items = when {
                    dataElement == null || dataElement.isJsonNull -> arrayListOf<MenuItem>()
                    dataElement.isJsonArray -> {
                        // data is an array
                        val list = arrayListOf<MenuItem>()
                        dataElement.asJsonArray.forEach { element ->
                            context?.deserialize<MenuItem>(element, MenuItem::class.java)?.let {
                                list.add(it)
                            }
                        }
                        list
                    }
                    dataElement.isJsonObject -> {
                        // data is a single object, wrap it in an array
                        val item = context?.deserialize<MenuItem>(dataElement, MenuItem::class.java)
                        if (item != null) {
                            arrayListOf(item)
                        } else {
                            arrayListOf()
                        }
                    }
                    else -> arrayListOf()
                }
                
                MenuResponse(status = status, message = message, data = items)
            }
            else -> MenuResponse()
        }
    }
}

@JsonAdapter(MenuResponseDeserializer::class)
data class MenuResponse(
    var status: Boolean? = null,
    var message: String? = null,
    var data: ArrayList<MenuItem>? = arrayListOf()
) {
    // Helper method to get menu items safely
    fun getMenuItems(): ArrayList<MenuItem> {
        return data ?: arrayListOf()
    }
}



