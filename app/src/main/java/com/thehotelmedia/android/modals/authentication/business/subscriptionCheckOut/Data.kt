package com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut

import com.google.gson.annotations.SerializedName


data class Data (
    @SerializedName("orderID"        ) var orderID        : String?         = null,
    @SerializedName("razorPayOrder"  ) var razorPayOrder  : RazorPayOrder?  = RazorPayOrder(),
    @SerializedName("billingAddress" ) var billingAddress : BillingAddress? = BillingAddress(),
    @SerializedName("plan"           ) var plan           : Plan?           = Plan(),
    @SerializedName("payment"        ) var payment        : Payment?        = Payment()

)