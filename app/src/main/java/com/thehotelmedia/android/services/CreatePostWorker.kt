package com.thehotelmedia.android.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.thehotelmedia.android.R
import com.thehotelmedia.android.apiService.Retrofit
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.interFaces.Application
import com.thehotelmedia.android.modals.forms.createPost.CreatePostModal
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors

class CreatePostWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val apiService: Application = Retrofit.apiService(context).create(Application::class.java)
    private val preferenceManager: PreferenceManager = PreferenceManager.getInstance(context)


    companion object {
        private const val CHANNEL_ID = "create_post_channel"
        private const val CHANNEL_NAME = "Create Post Notifications"
        private const val NOTIFICATION_ID = 1
    }

    override fun doWork(): Result {
        val mediaList = inputData.getStringArray("mediaList")?.toList() ?: emptyList()


        val selectedTagIdList = inputData.getStringArray("selectedTagIdList")?.toList() ?: emptyList()
        val content = inputData.getString("content") ?: ""
        val selectedPlaceName = inputData.getString("selectedPlaceName") ?: ""
        val selectedLat = inputData.getDouble("selectedLat", 0.0)
        val selectedLng = inputData.getDouble("selectedLng", 0.0)
        val selectedFeeling = inputData.getString("selectedFeeling") ?: ""
        val collaboratorUserIDs = inputData.getStringArray("collaboratorUserIDs")?.toList() ?: emptyList()
        
        Log.d("CreatePostWorker", "=== WORKER START DEBUG ===")
        Log.d("CreatePostWorker", "collaboratorUserIDs from inputData: ${collaboratorUserIDs.size} items")
        Log.d("CreatePostWorker", "collaboratorUserIDs values: $collaboratorUserIDs")
        Log.d("CreatePostWorker", "Raw inputData keys: ${inputData.keyValueMap.keys}")
        Log.d("CreatePostWorker", "==========================")

        createNotificationChannel()
        showNotification("Uploading Post...")

        sendServiceStartedBroadcast()

        return try {
            uploadPost(mediaList, selectedTagIdList, content, selectedPlaceName, selectedLat, selectedLng, selectedFeeling, collaboratorUserIDs)
            Result.success()
        } catch (e: Exception) {
            Log.e("CreatePostWorker", "Upload failed", e)
//            showFinalNotification("Upload Failed ❌", "Something went wrong!")
            showFinalNotification("Upload Failed ❌", "$e")
            Result.failure()
        }
    }

    private fun sendServiceStartedBroadcast() {
        val intent = Intent(Constants.CREATE_POST_BROADCAST)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun uploadPost(
        mediaList: List<String>,
        selectedTagIdList: List<String>,
        content: String,
        selectedPlaceName: String,
        selectedLat: Double,
        selectedLng: Double,
        selectedFeeling: String,
        collaboratorUserIDs: List<String>
    ) {
        createPost(content, selectedPlaceName, selectedLat, selectedLng, selectedFeeling, mediaList, selectedTagIdList, collaboratorUserIDs)
    }

    private fun createPost(
        content: String,
        selectedPlaceName: String,
        selectedLat: Double,
        selectedLng: Double,
        selectedFeeling: String,
        mediaList: List<String>,
        selectedTagIdList: List<String>,
        collaboratorUserIDs: List<String>
    ) {

        println("asfhdjkshfjksah  mediaList  $mediaList")

//        val mediaParts = mediaList.map { mediaPath ->
//            val file = if (mediaPath.startsWith("content://")) {
//                getFileFromContentUri(applicationContext, Uri.parse(mediaPath), "temp_image.jpg")
//            } else {
//                File(mediaPath)
//            }
//
//            file?.let {
//                val mimeType = Files.probeContentType(it.toPath()) ?: "application/octet-stream"
//                val requestBody = it.asRequestBody(mimeType.toMediaTypeOrNull())
//                MultipartBody.Part.createFormData("media", it.name, requestBody)
//            }
//        }.filterNotNull() // Null values remove karne ke liye

        val mediaParts = mediaList.mapIndexed { index, mediaPath ->
            val file = when {
                mediaPath.startsWith("content://") -> {
                    // Handle content:// URIs
                    val fileExtension = if (mediaPath.contains("video") || mediaPath.contains("mp4")) ".mp4" else ".jpg"
                    val filename = "temp_media_$index$fileExtension"
                    getFileFromContentUri(applicationContext, Uri.parse(mediaPath), filename)
                }
                mediaPath.startsWith("file://") || mediaPath.startsWith("file:") -> {
                    // Handle file:// URIs - remove the prefix and get the actual path
                    val cleanPath = mediaPath.replace("file://", "").replace("file:", "")
                    val file = File(cleanPath)
                    // Check if file exists, if not, try to get it from content URI
                    if (file.exists()) {
                        file
                    } else {
                        Log.w("CreatePostWorker", "File not found at path: $cleanPath, trying to get from URI")
                        // Try to parse as URI and get file
                        try {
                            val uri = Uri.parse(mediaPath)
                            if (uri.scheme == "file") {
                                val path = uri.path ?: cleanPath
                                File(path)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("CreatePostWorker", "Failed to parse URI: $mediaPath", e)
                            null
                        }
                    }
                }
                else -> {
                    // Direct file path
                    val file = File(mediaPath)
                    if (file.exists()) {
                        file
                    } else {
                        Log.w("CreatePostWorker", "File not found at path: $mediaPath")
                        null
                    }
                }
            }

            file?.let {
                if (!it.exists()) {
                    Log.e("CreatePostWorker", "File does not exist: ${it.absolutePath}")
                    null
                } else {
                    try {
                        val mimeType = Files.probeContentType(it.toPath()) ?: "application/octet-stream"
                        val requestBody = it.asRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("media", it.name, requestBody)
                    } catch (e: Exception) {
                        Log.e("CreatePostWorker", "Failed to create multipart for file: ${it.absolutePath}", e)
                        null
                    }
                }
            }
        }.filterNotNull()


        val taggedParts = selectedTagIdList.map {
            MultipartBody.Part.createFormData("tagged[]", it)
        }

        val accessToken = preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "") ?: ""
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val placeNameBody = selectedPlaceName.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = selectedLat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = selectedLng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val feelingsBody = selectedFeeling.toRequestBody("text/plain".toMediaTypeOrNull())

        val call = apiService.createPost(
            accessTokenBody, contentBody, placeNameBody, latBody, lngBody, feelingsBody, taggedParts, mediaParts
        )

        call.enqueue(object : Callback<CreatePostModal> {
            override fun onResponse(call: Call<CreatePostModal>, response: Response<CreatePostModal>) {
                Log.d("CreatePostWorker", "API Response: $response")
                Log.d("CreatePostWorker", "API Response Body: ${response.body()}")
                val status = response.body()?.status
                val msg = response.body()?.message ?: "Something went wrong!"
                if (status == true){
                    val postId = response.body()?.data?.id ?: ""
                    Log.d("CreatePostWorker", "=== POST CREATED DEBUG ===")
                    Log.d("CreatePostWorker", "Post created successfully. postId: $postId")
                    Log.d("CreatePostWorker", "collaboratorUserIDs at invite time: ${collaboratorUserIDs.size} items")
                    Log.d("CreatePostWorker", "collaboratorUserIDs values: $collaboratorUserIDs")
                    Log.d("CreatePostWorker", "==========================")
                    
                    if (postId.isNotEmpty()) {
                        // Send collaboration invites sequentially on background thread
                        Log.d("CreatePostWorker", "Sending collaboration invites for postId: $postId, collaborators: ${collaboratorUserIDs.size}")
                        if (collaboratorUserIDs.isNotEmpty()) {
                            // Run on background thread to avoid NetworkOnMainThreadException
                            val executor = Executors.newSingleThreadExecutor()
                            executor.execute {
                                try {
                                    val token = preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "") ?: ""
                                    collaboratorUserIDs.forEach { invitedUserId ->
                                        try {
                                            // Validate userId before sending
                                            if (invitedUserId.isNullOrEmpty() || invitedUserId == "null") {
                                                Log.e("CreatePostWorker", "Skipping invalid userId: '$invitedUserId'")
                                                return@forEach
                                            }
                                            
                                            Log.d("CreatePostWorker", "=== INVITING COLLABORATOR ===")
                                            Log.d("CreatePostWorker", "postId: $postId")
                                            Log.d("CreatePostWorker", "invitedUserId: $invitedUserId")
                                            Log.d("CreatePostWorker", "token length: ${token.length}")
                                            
                                            val inviteResponse = apiService.collaborationInvite(token, postId, invitedUserId).execute()
                                            
                                            Log.d("CreatePostWorker", "Response code: ${inviteResponse.code()}")
                                            Log.d("CreatePostWorker", "Response isSuccessful: ${inviteResponse.isSuccessful}")
                                            
                                            if (inviteResponse.isSuccessful) {
                                                val inviteBody = inviteResponse.body()
                                                Log.d("CreatePostWorker", "✅ Collaboration invite successful!")
                                                Log.d("CreatePostWorker", "Response message: ${inviteBody?.message}")
                                                Log.d("CreatePostWorker", "Response status: ${inviteBody?.status}")
                                            } else {
                                                val errorBody = inviteResponse.errorBody()?.string()
                                                Log.e("CreatePostWorker", "❌ Collaboration invite failed!")
                                                Log.e("CreatePostWorker", "Error code: ${inviteResponse.code()}")
                                                Log.e("CreatePostWorker", "Error message: ${inviteResponse.message()}")
                                                Log.e("CreatePostWorker", "Error body: $errorBody")
                                            }
                                            Log.d("CreatePostWorker", "============================")
                                        } catch (e: Exception) {
                                            Log.e("CreatePostWorker", "❌ Exception sending invite for userId: $invitedUserId", e)
                                            Log.e("CreatePostWorker", "Exception message: ${e.message}")
                                            Log.e("CreatePostWorker", "Exception stack trace: ${e.stackTraceToString()}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CreatePostWorker", "Failed sending collaboration invites", e)
                                } finally {
                                    executor.shutdown()
                                }
                            }
                        } else {
                            Log.d("CreatePostWorker", "No collaborators to invite")
                        }
                    } else {
                        Log.e("CreatePostWorker", "PostId is empty, cannot send collaboration invites")
                    }
                    showFinalNotification("Post Uploaded Successfully ✅", "Your post has been uploaded!")
                }else{
                    showFinalNotification("Upload Failed ❌", msg)
                }

            }

            override fun onFailure(call: Call<CreatePostModal>, t: Throwable) {
                Log.e("CreatePostWorker", "API call failed", t)
//                showFinalNotification("Upload Failed ❌", "Something went wrong!")
                showFinalNotification("Upload Failed ❌", "$t")
            }
        })
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // Check if the channel already exists to avoid recreating it
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: Exception) {
                Log.e("NotificationChannel", "Failed to create notification channel", e)
            }
        }
    }

    private fun showNotification(title: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle(title)
            .setOngoing(true)
            .setProgress(0, 0, true) // Indeterminate progress
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }



    private fun showFinalNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun getFileFromContentUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                Log.e("CreatePostWorker", "Failed to open input stream for URI: $uri")
                return null
            }
            
            // Use external files directory for videos to avoid cache size issues
            val outputDir = if (fileName.endsWith(".mp4") || fileName.endsWith(".mov") || fileName.endsWith(".avi")) {
                File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "UploadCache").apply {
                    if (!exists()) mkdirs()
                }
            } else {
                context.cacheDir
            }
            
            val file = File(outputDir, fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            if (!file.exists() || file.length() == 0L) {
                Log.e("CreatePostWorker", "File was not created or is empty: ${file.absolutePath}")
                return null
            }
            
            Log.d("CreatePostWorker", "Successfully copied file from URI to: ${file.absolutePath}, size: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            Log.e("CreatePostWorker", "Failed to get file from URI: $uri", e)
            null
        }
    }

}