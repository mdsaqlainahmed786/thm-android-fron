package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class Payment (

  @SerializedName("subtotal"        ) var subtotal        : Double? = null,
  @SerializedName("convinceCharges" ) var convinceCharges : Double? = null,
  @SerializedName("gst"             ) var gst             : Double? = null,
  @SerializedName("gstRate"         ) var gstRate         : Double? = null,
  @SerializedName("total"           ) var total           : Double? = null,
  @SerializedName("discount"           ) var discount     : Double? = null,
  @SerializedName("promocode"       ) var promoCode       : PromoCode? = PromoCode()

)