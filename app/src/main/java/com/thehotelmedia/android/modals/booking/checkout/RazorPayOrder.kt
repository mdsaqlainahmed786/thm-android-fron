package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class RazorPayOrder (

    // Razorpay Order API returns amounts in the smallest currency unit (e.g. paise) as integers.
    @SerializedName("amount"      ) var amount     : Long?    = null,
    @SerializedName("amount_due"  ) var amountDue  : Long?    = null,
    @SerializedName("amount_paid" ) var amountPaid : Long?    = null,
    @SerializedName("attempts"    ) var attempts   : Int?    = null,
    @SerializedName("created_at"  ) var createdAt  : Int?    = null,
    @SerializedName("currency"    ) var currency   : String? = null,
    @SerializedName("entity"      ) var entity     : String? = null,
    @SerializedName("id"          ) var id         : String? = null,
    @SerializedName("notes"       ) var notes      : Notes?  = Notes(),
    @SerializedName("offer_id"    ) var offerId    : String? = null,
    @SerializedName("receipt"     ) var receipt    : String? = null,
    @SerializedName("status"      ) var status     : String? = null

)
