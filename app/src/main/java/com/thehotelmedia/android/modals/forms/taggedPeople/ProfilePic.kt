package com.thehotelmedia.android.modals.forms.taggedPeople

import com.google.gson.annotations.SerializedName


import android.os.Parcel
import android.os.Parcelable

data class ProfilePic (
  @SerializedName("small") var small: String? = null,
  @SerializedName("medium") var medium: String? = null,
  @SerializedName("large") var large: String? = null
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readString(),   // Read the 'small' field from the parcel
    parcel.readString(),   // Read the 'medium' field from the parcel
    parcel.readString()    // Read the 'large' field from the parcel
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(small)   // Write the 'small' field to the parcel
    parcel.writeString(medium)  // Write the 'medium' field to the parcel
    parcel.writeString(large)   // Write the 'large' field to the parcel
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ProfilePic> {
    override fun createFromParcel(parcel: Parcel): ProfilePic {
      return ProfilePic(parcel)
    }

    override fun newArray(size: Int): Array<ProfilePic?> {
      return arrayOfNulls(size)
    }
  }
}
