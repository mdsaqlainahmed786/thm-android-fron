package com.thehotelmedia.android.modals.forms.taggedPeople

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.forms.taggedPeople.ProfilePic
import java.io.Serializable


//data class TaggedData (
//
//    @SerializedName("_id"         ) var Id          : String?     = null,
//    @SerializedName("accountType" ) var accountType : String?     = null,
//    @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
//    @SerializedName("username"    ) var username    : String?     = null,
//    @SerializedName("name"        ) var name        : String?     = null
//
//): Serializable

import android.os.Parcel
import android.os.Parcelable


data class TaggedData(
    @SerializedName("_id") var Id: String? = null,
    @SerializedName("accountType") var accountType: String? = null,
    @SerializedName("profilePic") var profilePic: ProfilePic? = ProfilePic(),
    @SerializedName("username") var username: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("businessProfileRef") var businessProfileRef: BusinessProfileRef? = BusinessProfileRef(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(ProfilePic::class.java.classLoader),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Id)
        parcel.writeString(accountType)
        parcel.writeParcelable(profilePic, flags)  // ProfilePic is now parcelable
        parcel.writeString(username)
        parcel.writeString(name)
        parcel.writeParcelable(businessProfileRef, flags)  // ProfilePic is now parcelable
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TaggedData> {
        override fun createFromParcel(parcel: Parcel): TaggedData {
            return TaggedData(parcel)
        }

        override fun newArray(size: Int): Array<TaggedData?> {
            return arrayOfNulls(size)
        }
    }
}



