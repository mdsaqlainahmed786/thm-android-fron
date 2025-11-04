package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName

data class DashboardData (

    @SerializedName("accountReached"     ) var accountReached     : ArrayList<AccountReached>     = arrayListOf(),
    @SerializedName("websiteRedirection" ) var websiteRedirection : ArrayList<WebsiteRedirection> = arrayListOf(),
    @SerializedName("totalFollowers"     ) var totalFollowers     : ArrayList<TotalFollowers>     = arrayListOf(),
    @SerializedName("engagements"        ) var engagements        : ArrayList<Engagements>        = arrayListOf(),

)
