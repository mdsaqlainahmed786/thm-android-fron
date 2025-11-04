package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class RazorPayOrder (

    @SerializedName("amount"      ) var amount     : Double?    = null,
    @SerializedName("amount_due"  ) var amountDue  : Int?    = null,
    @SerializedName("amount_paid" ) var amountPaid : Double?    = null,
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
