package com.thehotelmedia.android.modals.booking.roomsList

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.booking.roomDetails.Data as RoomData
import com.google.gson.reflect.TypeToken

/**
 * Server response for GET /rooms?businessProfileID=...
 * We reuse the same room schema as RoomDetails (`modals.booking.roomDetails.Data`).
 *
 * Backend responses vary across deployments:
 * - data can be an array:  { data: [ {room}, ... ] }
 * - or an object:         { data: { rooms: [ {room}, ... ] } }
 */
data class RoomsListModal(
    @SerializedName("status") var status: Boolean? = null,
    @SerializedName("statusCode") var statusCode: Int? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("data") var data: JsonElement? = null,
) {
    fun rooms(gson: Gson = Gson()): ArrayList<RoomData> {
        val el = data ?: return arrayListOf()
        return try {
            when {
                el.isJsonArray -> {
                    val type = object : TypeToken<ArrayList<RoomData>>() {}.type
                    gson.fromJson(el, type) ?: arrayListOf()
                }
                el.isJsonObject && el.asJsonObject.has("rooms") -> {
                    val roomsEl = el.asJsonObject.get("rooms")
                    val type = object : TypeToken<ArrayList<RoomData>>() {}.type
                    gson.fromJson(roomsEl, type) ?: arrayListOf()
                }
                else -> arrayListOf()
            }
        } catch (_: Exception) {
            arrayListOf()
        }
    }
}


