package com.thehotelmedia.android.modals.userProfile

import com.google.gson.annotations.SerializedName


data class AirPollution (

    @SerializedName("list"  ) var list  : ArrayList<AirPollutionList> = arrayListOf()

)
