package com.thehotelmedia.android.modals.forms.taggedPeople

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class BusinessTypeRef(
    @SerializedName("_id") var Id: String? = null,
    @SerializedName("icon") var icon: String? = null,
    @SerializedName("name") var name: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Id)
        parcel.writeString(icon)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BusinessTypeRef> {
        override fun createFromParcel(parcel: Parcel): BusinessTypeRef {
            return BusinessTypeRef(parcel)
        }

        override fun newArray(size: Int): Array<BusinessTypeRef?> {
            return arrayOfNulls(size)
        }
    }
}
