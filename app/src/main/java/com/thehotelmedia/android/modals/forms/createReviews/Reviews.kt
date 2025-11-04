package com.thehotelmedia.android.modals.forms.createReviews

import com.google.gson.annotations.SerializedName

data class Reviews (

    @SerializedName("rating"     ) var rating     : Double?    = null,
    @SerializedName("questionID" ) var questionID : String? = null

)
