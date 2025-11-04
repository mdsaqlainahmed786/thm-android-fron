package com.thehotelmedia.android.modals.forms.taggedPeople

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class BusinessProfileRef(
    @SerializedName("profilePic") var businessProfilePic: BusinessProfilePic? = BusinessProfilePic(),
    @SerializedName("name") var name: String? = null,
    @SerializedName("address") var address: Address? = Address(),
    @SerializedName("businessTypeRef") var businessTypeRef: BusinessTypeRef? = BusinessTypeRef()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ProfilePic::class.java.classLoader),
        parcel.readString(),
        parcel.readParcelable(Address::class.java.classLoader),
        parcel.readParcelable(BusinessTypeRef::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(businessProfilePic, flags) // ProfilePic is now parcelable
        parcel.writeString(name)
        parcel.writeParcelable(address, flags) // Address is now parcelable
        parcel.writeParcelable(businessTypeRef, flags) // BusinessTypeRef is now parcelable
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BusinessProfileRef> {
        override fun createFromParcel(parcel: Parcel): BusinessProfileRef {
            return BusinessProfileRef(parcel)
        }

        override fun newArray(size: Int): Array<BusinessProfileRef?> {
            return arrayOfNulls(size)
        }
    }
}
