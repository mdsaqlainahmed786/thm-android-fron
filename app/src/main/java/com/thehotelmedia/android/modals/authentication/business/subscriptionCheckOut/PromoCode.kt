package com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut

import com.google.gson.annotations.SerializedName

data class PromoCode (

    @SerializedName("code"      ) var code      : String? = null,
    @SerializedName("priceType" ) var priceType : String? = null,
    @SerializedName("value"     ) var value     : Int?    = null

)