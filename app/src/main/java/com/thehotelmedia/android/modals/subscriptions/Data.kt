package com.thehotelmedia.android.modals.subscriptions

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.SubscriptionData


data class Data (

  @SerializedName("subscription" ) var subscription : Subscription?               = Subscription(),
  @SerializedName("plans"        ) var plans        : ArrayList<SubscriptionData> = arrayListOf()

)