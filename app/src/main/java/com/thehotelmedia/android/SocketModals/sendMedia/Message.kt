package com.thehotelmedia.android.SocketModals.sendMedia

import com.google.gson.annotations.SerializedName


data class Message (

  @SerializedName("type"     ) var type     : String? = null,
  @SerializedName("message"  ) var message  : String? = null,
  @SerializedName("mediaUrl" ) var mediaUrl : String? = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String? = null,
  @SerializedName("mediaID" ) var mediaID : String? = null,

)