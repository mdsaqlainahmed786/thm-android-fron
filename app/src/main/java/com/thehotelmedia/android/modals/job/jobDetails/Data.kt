package com.thehotelmedia.android.modals.job.jobDetails

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"               ) var Id                : String? = null,
  @SerializedName("userID"            ) var userID            : String? = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("title"             ) var title             : String? = null,
  @SerializedName("designation"       ) var designation       : String? = null,
  @SerializedName("description"       ) var description       : String? = null,
  @SerializedName("jobType"           ) var jobType           : String? = null,
  @SerializedName("salary"            ) var salary            : String? = null,
  @SerializedName("joiningDate"       ) var joiningDate       : String? = null,
  @SerializedName("numberOfVacancies" ) var numberOfVacancies : String? = null,
  @SerializedName("experience"        ) var experience        : String? = null,
  @SerializedName("createdAt"         ) var createdAt         : String? = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String? = null,
  @SerializedName("__v"               ) var _v                : Int?    = null,
  @SerializedName("postedBy"          ) var postedBy          : PostedBy? = PostedBy()

)