package com.thehotelmedia.android.modals.suggestedBusiness

import com.google.gson.annotations.SerializedName


data class SuggestedBusinessModal (

    @SerializedName("status"         ) var status         : Boolean?        = null,
    @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
    @SerializedName("message"        ) var message        : String?         = null,
    @SerializedName("data"           ) var suggestionData           : ArrayList<SuggestionData> = arrayListOf(),
    @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
    @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
    @SerializedName("totalResources" ) var totalResources : Int?            = null

)