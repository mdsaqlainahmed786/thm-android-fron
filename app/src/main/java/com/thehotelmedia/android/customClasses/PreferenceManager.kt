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
        if (value.isEmpty() && (key == Keys.ACCESS_TOKEN || key == Keys.REFRESH_TOKEN || key == Keys.USER_ID)) {
            android.util.Log.w("PreferenceManager", "Warning: Attempting to save empty value for critical key: ${key.name}")
        }
        
        val editor = sharedPreferences.edit()
        editor.putString(key.name, value)
        // Use commit() for critical authentication data to ensure immediate persistence
        // This prevents data loss on app restart/rebuild
        if (key == Keys.ACCESS_TOKEN || key == Keys.REFRESH_TOKEN || key == Keys.USER_ID || 
            key == Keys.COOKIES || key == Keys.BUSINESS_TYPE || key == Keys.BUSINESS_ACC_APPROVED) {
            val success = editor.commit() // Synchronous commit for critical data
            if (!success) {
                android.util.Log.e("PreferenceManager", "Failed to commit critical preference: ${key.name}, retrying...")
                // Create new editor and retry
                val retryEditor = sharedPreferences.edit()
                retryEditor.putString(key.name, value)
                val retrySuccess = retryEditor.commit()
                if (!retrySuccess) {
                    android.util.Log.e("PreferenceManager", "CRITICAL: Failed to commit after retry: ${key.name}")
                    // Last resort: use apply
                    retryEditor.apply()
                } else {
                    android.util.Log.d("PreferenceManager", "Successfully committed after retry: ${key.name}")
                }
            } else {
                android.util.Log.d("PreferenceManager", "Successfully committed: ${key.name}")
            }
        } else {
            editor.apply() // Async for non-critical data
        }
    }

    fun getString(key: Keys, defaultValue: String): String? {
        val value = sharedPreferences.getString(key.name, defaultValue)
        // Debug logging for critical keys to track data retrieval
        if (key == Keys.ACCESS_TOKEN || key == Keys.REFRESH_TOKEN || key == Keys.USER_ID) {
            if (value.isNullOrEmpty()) {
                android.util.Log.w("PreferenceManager", "Retrieved empty/null value for critical key: ${key.name}, defaultValue was: '$defaultValue'")
                // Check if key exists at all
                val keyExists = sharedPreferences.contains(key.name)
                android.util.Log.d("PreferenceManager", "Key '${key.name}' exists in SharedPreferences: $keyExists")
            } else {
                android.util.Log.d("PreferenceManager", "Retrieved value for ${key.name}, length: ${value.length}")
            }
        }
        return value
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
        // Use commit() for critical authentication flags to ensure immediate persistence
        if (key == Keys.BUSINESS_ACC_APPROVED || key == Keys.USER_ACCEPTED_TERMS) {
            val success = editor.commit() // Synchronous commit for critical data
            if (!success) {
                android.util.Log.e("PreferenceManager", "Failed to commit critical boolean preference: ${key.name}")
                // Retry with apply as fallback
                editor.apply()
            }
        } else {
            editor.apply() // Async for non-critical data
        }
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
        android.util.Log.d("PreferenceManager", "clearPreferences() called - This will clear ALL preferences including auth data!")
        val editor = sharedPreferences.edit()
        editor.clear()
        // Use commit() to ensure immediate clearing
        val success = editor.commit()
        if (!success) {
            android.util.Log.e("PreferenceManager", "Failed to commit clearPreferences, using apply()")
            editor.apply()
        } else {
            android.util.Log.d("PreferenceManager", "Successfully cleared all preferences")
        }
    }

    fun removePreference(key: Keys) {
        val editor = sharedPreferences.edit()
        editor.remove(key.name)
        editor.apply()
    }
    
    /**
     * Batch save multiple critical authentication values atomically
     * This ensures all auth data is saved together and prevents partial saves
     */
    fun saveAuthDataBatch(
        accessToken: String? = null,
        refreshToken: String? = null,
        userId: String? = null,
        cookies: String? = null,
        businessType: String? = null,
        businessAccApproved: Boolean? = null,
        userAcceptedTerms: Boolean? = null
    ) {
        android.util.Log.d("PreferenceManager", "saveAuthDataBatch called - Token: ${accessToken?.take(10)}..., UserId: $userId, BusinessType: $businessType, AcceptedTerms: $userAcceptedTerms")
        
        val editor = sharedPreferences.edit()
        
        accessToken?.let { 
            editor.putString(Keys.ACCESS_TOKEN.name, it)
            android.util.Log.d("PreferenceManager", "Adding ACCESS_TOKEN to batch, length: ${it.length}")
        }
        refreshToken?.let { editor.putString(Keys.REFRESH_TOKEN.name, it) }
        userId?.let { 
            editor.putString(Keys.USER_ID.name, it)
            android.util.Log.d("PreferenceManager", "Adding USER_ID to batch: $it")
        }
        cookies?.let { editor.putString(Keys.COOKIES.name, it) }
        businessType?.let { 
            editor.putString(Keys.BUSINESS_TYPE.name, it)
            android.util.Log.d("PreferenceManager", "Adding BUSINESS_TYPE to batch: $it")
        }
        businessAccApproved?.let { editor.putBoolean(Keys.BUSINESS_ACC_APPROVED.name, it) }
        userAcceptedTerms?.let { 
            editor.putBoolean(Keys.USER_ACCEPTED_TERMS.name, it)
            android.util.Log.d("PreferenceManager", "Adding USER_ACCEPTED_TERMS to batch: $it")
        }
        
        val success = editor.commit()
        if (!success) {
            android.util.Log.e("PreferenceManager", "CRITICAL: Failed to commit auth data batch!")
            // Retry
            val retryEditor = sharedPreferences.edit()
            accessToken?.let { retryEditor.putString(Keys.ACCESS_TOKEN.name, it) }
            refreshToken?.let { retryEditor.putString(Keys.REFRESH_TOKEN.name, it) }
            userId?.let { retryEditor.putString(Keys.USER_ID.name, it) }
            cookies?.let { retryEditor.putString(Keys.COOKIES.name, it) }
            businessType?.let { retryEditor.putString(Keys.BUSINESS_TYPE.name, it) }
            businessAccApproved?.let { retryEditor.putBoolean(Keys.BUSINESS_ACC_APPROVED.name, it) }
            userAcceptedTerms?.let { retryEditor.putBoolean(Keys.USER_ACCEPTED_TERMS.name, it) }
            val retrySuccess = retryEditor.commit()
            if (!retrySuccess) {
                android.util.Log.e("PreferenceManager", "CRITICAL: Retry also failed!")
            } else {
                android.util.Log.d("PreferenceManager", "Retry succeeded")
            }
        } else {
            android.util.Log.d("PreferenceManager", "Successfully committed auth data batch")
            // Immediately verify the data was saved
            val verifyToken = sharedPreferences.getString(Keys.ACCESS_TOKEN.name, null)
            val verifyUserId = sharedPreferences.getString(Keys.USER_ID.name, null)
            val verifyAcceptedTerms = sharedPreferences.getBoolean(Keys.USER_ACCEPTED_TERMS.name, false)
            android.util.Log.d("PreferenceManager", "Verification - Token saved: ${verifyToken != null}, UserId saved: ${verifyUserId != null}, AcceptedTerms saved: $verifyAcceptedTerms")
        }
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

