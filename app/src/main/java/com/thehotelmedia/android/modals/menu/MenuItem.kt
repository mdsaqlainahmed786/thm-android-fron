package com.thehotelmedia.android.modals.menu

import com.google.gson.annotations.SerializedName

data class MenuItem(
    @SerializedName("id") var Id: String? = null,
    @SerializedName("businessProfileID") var businessProfileID: String? = null,
    @SerializedName("mediaID") var mediaID: String? = null,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("media") var media: MenuMedia? = null
)

data class MenuMedia(
    @SerializedName("id") var Id: String? = null,
    @SerializedName("mediaType") var mediaType: String? = null, // "im" for image, "pdf" for PDF
    @SerializedName("sourceUrl") var sourceUrl: String? = null,
    @SerializedName("thumbnailUrl") var thumbnailUrl: String? = null,
    @SerializedName("mimeType") var mimeType: String? = null // "image/jpeg", "application/pdf", etc.
)



