package com.thehotelmedia.android.modals.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "notification_data")
data class NotificationData (
  @PrimaryKey
  @SerializedName("_id"         ) var Id          : String?   = null,
  @SerializedName("isSeen"      ) var isSeen      : Boolean?  = null,
  @SerializedName("userID"      ) var userID      : String?   = null,
  @SerializedName("title"       ) var title       : String?   = null,
  @SerializedName("description" ) var description : String?   = null,
  @SerializedName("type"        ) var type        : String?   = null,
  @SerializedName("metadata"    ) var metadata    : Metadata? = Metadata(),
  @SerializedName("createdAt"   ) var createdAt   : String?   = null,
  @SerializedName("usersRef"    ) var usersRef    : UsersRef? = UsersRef(),
  @SerializedName("isConnected" ) var isConnected : Boolean?  = null,
  @SerializedName("isRequested" ) var isRequested : Boolean?  = null,

)