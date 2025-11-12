package com.thehotelmedia.android.requestModel.auth

data class VerifyOtpRequest(
    val phoneNumber: String,
    val dialCode: String,
    val deviceID: String,
    val devicePlatform: String,
    val notificationToken: String,
    val lat: Double,
    val lng: Double,
    val language: String
)

