package com.thehotelmedia.android.modals.Stories

import com.google.gson.annotations.SerializedName


data class StoriesData (

  @SerializedName("myStories" ) var myStories : ArrayList<StoriesRef> = arrayListOf(),
  @SerializedName("stories"   ) var stories   : ArrayList<Stories>   = arrayListOf()

)