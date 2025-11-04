package com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut

import com.google.gson.annotations.SerializedName


data class Payment (

  @SerializedName("subtotal"  ) var subtotal  : Double?    = null,
  @SerializedName("gst"       ) var gst       : Double?    = null,
  @SerializedName("total"     ) var total     : Double?    = null,
  @SerializedName("gstRate"   ) var gstRate   : Double?    = null,
  @SerializedName("discount"  ) var discount  : Double?    = null,
  @SerializedName("promoCode" ) var promoCode : PromoCode? = PromoCode()

)