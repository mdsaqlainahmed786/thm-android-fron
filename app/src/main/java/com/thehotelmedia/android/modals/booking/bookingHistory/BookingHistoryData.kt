package com.thehotelmedia.android.modals.booking.bookingHistory

import com.google.gson.annotations.SerializedName


data class BookingHistoryData (

    @SerializedName("_id"                 ) var Id                  : String?                 = null,
    @SerializedName("status"              ) var status              : String?                 = null,
    @SerializedName("adults"              ) var adults              : Int?                    = null,
    @SerializedName("children"            ) var children            : Int?                    = null,
    @SerializedName("childrenAge"         ) var childrenAge         : ArrayList<Int>          = arrayListOf(),
    @SerializedName("bookedFor"           ) var bookedFor           : String?                 = null,
    @SerializedName("subTotal"            ) var subTotal            : Double?                    = null,
    @SerializedName("discount"            ) var discount            : Double?                    = null,
    @SerializedName("tax"                 ) var tax                 : Double?                    = null,
    @SerializedName("convinceCharge"      ) var convinceCharge      : Double?                    = null,
    @SerializedName("grandTotal"          ) var grandTotal          : Double?                    = null,
    @SerializedName("checkIn"             ) var checkIn             : String?                 = null,
    @SerializedName("checkOut"            ) var checkOut            : String?                 = null,
    @SerializedName("guestDetails"        ) var guestDetails        : ArrayList<GuestDetails> = arrayListOf(),
    @SerializedName("bookingID"           ) var bookingID           : String?                 = null,
    @SerializedName("type"                ) var bookingType           : String?                 = null,
    @SerializedName("metadata"            ) var metadata            : Metadata?           = Metadata(),
    @SerializedName("userID"              ) var userID              : String?                 = null,
    @SerializedName("businessProfileID"   ) var businessProfileID   : String?                 = null,
    @SerializedName("createdAt"           ) var createdAt           : String?                 = null,
    @SerializedName("updatedAt"           ) var updatedAt           : String?                 = null,
    @SerializedName("bookedRoom"          ) var bookedRoom          : BookedRoom?             = BookedRoom(),
    @SerializedName("isTravellingWithPet" ) var isTravellingWithPet : Boolean?                = null,
    @SerializedName("promoCode"           ) var promoCode           : String?                 = null,
    @SerializedName("promoCodeID"         ) var promoCodeID         : String?                 = null,
    @SerializedName("razorPayOrderID"     ) var razorPayOrderID     : String?                 = null,
    @SerializedName("roomsRef"            ) var roomsRef            : RoomsRef?               = RoomsRef(),
    @SerializedName("usersRef"            ) var usersRef            : UsersRef?               = UsersRef(),
    @SerializedName("businessProfileRef"  ) var businessProfileRef  : BusinessProfileRef?     = BusinessProfileRef()

)