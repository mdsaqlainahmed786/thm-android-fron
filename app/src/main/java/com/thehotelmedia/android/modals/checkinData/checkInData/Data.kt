package com.thehotelmedia.android.modals.checkinData.checkInData

import com.google.gson.annotations.SerializedName


data class Data (

    @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef?        = BusinessProfileRef(),
    @SerializedName("reviewQuestions"    ) var reviewQuestions    : ArrayList<ReviewQuestions> = arrayListOf()

)