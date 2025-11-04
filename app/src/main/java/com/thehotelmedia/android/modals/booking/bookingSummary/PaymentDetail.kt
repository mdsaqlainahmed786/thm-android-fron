package com.thehotelmedia.android.modals.booking.bookingSummary

import com.google.gson.annotations.SerializedName


data class PaymentDetail (

    @SerializedName("transactionID"     ) var transactionID     : String? = null,
    @SerializedName("paymentMethod"     ) var paymentMethod     : String? = null,
    @SerializedName("transactionAmount" ) var transactionAmount : Int?    = null

)
