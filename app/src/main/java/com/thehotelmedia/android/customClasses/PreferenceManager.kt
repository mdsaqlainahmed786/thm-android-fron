package com.thehotelmedia.android.customClasses

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thehotelmedia.android.customClasses.billing.BillingManager
import com.thehotelmedia.android.modals.profileData.profile.AmenitiesRef
import com.thehotelmedia.android.modals.profileData.profile.BusinessAnswerRef

class PreferenceManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences
    private val gson: Gson = Gson()
    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_NAME = "YOWS_USER_PREF"
        private var instance: PreferenceManager? = null

        @Synchronized
        fun getInstance(context: Context): PreferenceManager {
            if (instance == null) {
                instance = PreferenceManager(context.applicationContext)
            }
            return instance!!
        }
    }

    fun putString(key: Keys, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key.name, value)
        editor.apply()
    }

    fun getString(key: Keys, defaultValue: String): String? {
        return sharedPreferences.getString(key.name, defaultValue)
    }
    fun putInt(key: Keys, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key.name, value) // Corrected to putInt
        editor.apply()
    }

    fun getInt(key: Keys, defaultValue: Int): Int {
        return sharedPreferences.getInt(key.name, defaultValue)
    }

    fun putDouble(key: Keys, value: Double) {
        val editor = sharedPreferences.edit()
        editor.putString(key.name, value.toString()) // Convert Double to String
        editor.apply()
    }

    fun getDouble(key: Keys, defaultValue: Double): Double {
        val value = sharedPreferences.getString(key.name, null)
        return value?.toDoubleOrNull() ?: defaultValue // Convert String to Double or use default
    }

    // putBoolean function
    fun putBoolean(key: Keys, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key.name, value)
        editor.apply()
    }

    // getBoolean function
    fun getBoolean(key: Keys, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key.name, defaultValue)
    }


    // Method to store a list of AmenitiesRef
    fun putAmenitiesList(key: Keys, list: ArrayList<AmenitiesRef>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(list) // Convert list to JSON
        editor.putString(key.name, json)
        editor.apply()
    }

    // Method to retrieve a list of AmenitiesRef
    fun getAmenitiesList(key: Keys): ArrayList<AmenitiesRef>? {
        val json = sharedPreferences.getString(key.name, null) ?: return null
        val type = object : TypeToken<ArrayList<AmenitiesRef>>() {}.type
        return gson.fromJson(json, type) // Convert JSON back to ArrayList
    }

    // Method to store a list of BusinessAnswerRef
    fun putAnswerAmenitiesList(key: Keys, list: ArrayList<BusinessAnswerRef>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(list) // Convert list to JSON
        editor.putString(key.name, json)
        editor.apply()
    }

    // Method to retrieve a list of BusinessAnswerRef
    fun getAnswerAmenitiesList(key: Keys): ArrayList<BusinessAnswerRef>? {
        val json = sharedPreferences.getString(key.name, null) ?: return null
        val type = object : TypeToken<ArrayList<BusinessAnswerRef>>() {}.type
        return gson.fromJson(json, type) // Convert JSON back to ArrayList
    }

    fun clearPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun removePreference(key: Keys) {
        val editor = sharedPreferences.edit()
        editor.remove(key.name)
        editor.apply()
    }

    enum class Keys {
        WEATHER_TEMP,
        WEATHER_TYPE,
        AQI,
        AQI_PM25,



        PDF_SIZE_INT,
        VIDEO_DURATION_INT,
        BUSINESS_TYPE,
        USER_ACCEPTED_TERMS,
        LANGUAGE_CODE,

        BUSINESS_ACC_APPROVED,USER_DESCRIPTION,
        USER_ID,USER_USER_NAME,USER_FULL_NAME,USER_EMAIL,USER_DIAL_CODE,USER_PHONE_NUMBER,USER_SMALL_PROFILE_PIC,USER_MEDIUM_PROFILE_PIC,USER_LARGE_PROFILE_PIC,

        USER_STREET,USER_CITY,USER_STATE,USER_ZIPCODE,USER_COUNTRY,USER_LATITUDE,USER_LONGITUDE,
        COOKIES,ACCESS_TOKEN,REFRESH_TOKEN,
        USER_BUSINESS_ID,USER_SUB_BUSINESS_ID,
        USER_BUSINESS_NAME,
        AMENITIES_REF_LIST,ANSWER_AMENITIES_REF_LIST,
        HAS_UNREAD_NOTIFICATIONS,
        HAS_UNREAD_MESSAGES,
        UNREAD_CHAT_COUNT,

        IS_NOTIFICATION_ENABLED,
        IS_PRIVATE_ACCOUNT,
    }

}

