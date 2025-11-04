package com.thehotelmedia.android.SocketModals.fetchConversation

import com.google.gson.annotations.SerializedName


data class FetchConversationModal (

  @SerializedName("totalMessages" ) var totalMessages : Int?                = null,
  @SerializedName("totalPages"    ) var totalPages    : Int?                = null,
  @SerializedName("pageNo"        ) var pageNo        : Int?                = null,
  @SerializedName("messages"      ) var messages      : ArrayList<Messages> = arrayListOf()

)