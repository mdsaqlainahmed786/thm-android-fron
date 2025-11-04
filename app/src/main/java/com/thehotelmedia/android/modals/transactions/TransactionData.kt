package com.thehotelmedia.android.modals.transactions

import com.google.gson.annotations.SerializedName


data class TransactionData (

  @SerializedName("_id"                 ) var Id                  : String?              = null,
  @SerializedName("grandTotal"          ) var grandTotal          : Double?                 = null,
  @SerializedName("orderID"             ) var orderID             : String?              = null,
  @SerializedName("createdAt"           ) var createdAt           : String?              = null,
  @SerializedName("updatedAt"           ) var updatedAt           : String?              = null,
  @SerializedName("paymentDetail"       ) var paymentDetail       : PaymentDetail?       = PaymentDetail(),
  @SerializedName("subscriptionPlanRef" ) var subscriptionPlanRef : SubscriptionPlanRef? = SubscriptionPlanRef()

)