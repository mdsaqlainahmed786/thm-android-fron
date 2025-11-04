package com.thehotelmedia.android.SocketModals.sendMedia

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("to"      ) var to      : String?  = null,
  @SerializedName("message" ) var message : Message? = Message()

)