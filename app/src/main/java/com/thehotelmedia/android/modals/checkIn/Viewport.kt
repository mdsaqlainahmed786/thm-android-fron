package com.thehotelmedia.android.modals.checkIn

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.checkIn.Northeast
import com.thehotelmedia.android.modals.checkIn.Southwest


data class Viewport (

    @SerializedName("northeast" ) var northeast : Northeast? = Northeast(),
    @SerializedName("southwest" ) var southwest : Southwest? = Southwest()

)