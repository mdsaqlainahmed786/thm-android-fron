package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class Data (

    @SerializedName("_id"                 ) var Id                  : String?                 = null,
    @SerializedName("status"              ) var status              : String?                 = null,
    @SerializedName("adults"              ) var adults              : Int?                    = null,
    @SerializedName("children"            ) var children            : Int?                    = null,
    @SerializedName("childrenAge"         ) var childrenAge         : ArrayList<Int>          = arrayListOf(),
    @SerializedName("checkIn"             ) var checkIn             : String?                 = null,
    @SerializedName("checkOut"            ) var checkOut            : String?                 = null,
    @SerializedName("bookingID"           ) var bookingID           : String?                 = null,
    @SerializedName("userID"              ) var userID              : String?                 = null,
    @SerializedName("businessProfileID"   ) var businessProfileID   : String?                 = null,
    @SerializedName("createdAt"           ) var createdAt           : String?                 = null,
    @SerializedName("updatedAt"           ) var updatedAt           : String?                 = null,
    @SerializedName("__v"                 ) var _v                  : Int?                    = null,
    @SerializedName("bookedRoom"          ) var bookedRoom          : BookedRoom?             = BookedRoom(),
    @SerializedName("isTravellingWithPet" ) var isTravellingWithPet : Boolean?                = null,
    @SerializedName("bookedFor"           ) var bookedFor           : String?                 = null,
    @SerializedName("user"                ) var user                : User?               = User(),
//    @SerializedName("guestDetails"        ) var guestDetails        : ArrayList<GuestDetails> = arrayListOf(),
    @SerializedName("razorPayOrder"       ) var razorPayOrder       : RazorPayOrder?          = RazorPayOrder(),
    @SerializedName("businessProfileRef"  ) var businessProfileRef  : BusinessProfileRef?     = BusinessProfileRef(),
    @SerializedName("payment"             ) var payment             : Payment?                = Payment(),
    @SerializedName("room"                ) var room                : Room?                   = Room()

)