package com.thehotelmedia.android.extensions

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

class AppUpdateHelper(private val context: Context, private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private val REQUEST_CODE_UPDATE = 123
    private val TAG = "AppUpdateHelper"

    suspend fun checkForUpdate() {
        try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startUpdateFlow(appUpdateInfo)
            } else {
                Log.w(TAG, "No Update available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                activity,
                REQUEST_CODE_UPDATE
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to start update flow", e)
            Toast.makeText(context, "Failed to start update flow.", Toast.LENGTH_SHORT).show()
        }
    }

    fun resumeUpdateIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startUpdateFlow(appUpdateInfo)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get update info", e)
        }
    }

}
