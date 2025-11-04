package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Data (

    @SerializedName("dashboard" ) var dashboard : Dashboard?        = Dashboard(),
    @SerializedName("data"      ) var dashboardData      : DashboardData?             = DashboardData(),
    @SerializedName("stories"   ) var stories   : ArrayList<Stories> = arrayListOf(),
    @SerializedName("posts"     ) var posts     : ArrayList<Posts>  = arrayListOf()


)