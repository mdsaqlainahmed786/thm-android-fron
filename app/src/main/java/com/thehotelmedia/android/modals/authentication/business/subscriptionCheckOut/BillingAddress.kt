package com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut.Address


data class BillingAddress (

    @SerializedName("name"        ) var name        : String?  = null,
    @SerializedName("address"     ) var address     : Address? = Address(),
    @SerializedName("dialCode"    ) var dialCode    : String?  = null,
    @SerializedName("phoneNumber" ) var phoneNumber : String?  = null,
    @SerializedName("gstn"        ) var gstn        : String?  = null

)