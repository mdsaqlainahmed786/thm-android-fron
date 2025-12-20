package com.thehotelmedia.android.activity.userTypes.forms.createStory

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.camera.CustomCameraActivity
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity.Companion.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity.Companion.PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
import com.thehotelmedia.android.activity.userTypes.forms.VideoTrimmerActivity
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeople
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeopleActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.TagsAdapter
import com.thehotelmedia.android.adapters.imageEditor.FilterAdapter
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.GiffProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.customClasses.imageEditor.CustomEmojiEntryDialog
import com.thehotelmedia.android.customClasses.imageEditor.CustomTextEntryDialog
import com.thehotelmedia.android.customDialog.PhotoVideoDialog
import com.thehotelmedia.android.databinding.ActivityCreateStoryBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.setOnSwipeListener
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.yalantis.ucrop.UCrop       
import com.yalantis.ucrop.model.AspectRatio
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class CreateStoryActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateStoryBinding
    private val activity = this@CreateStoryActivity
    private var croppedImageUri: Uri? = null
    private var selectedBackgroundResId: Int = R.drawable.text_background_transparent
    private lateinit var photoEditor: PhotoEditor
    private var businessesType = ""
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var progressBar : CustomProgressBar
    private lateinit var giffProgressBar : GiffProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var textEntryDialog: CustomTextEntryDialog
    private var activeTextOverlay: View? = null
    private var lastSelectedTextColor: Int = Color.WHITE
    private var currentVideoUri: Uri? = null
    private var videoPlayer: ExoPlayer? = null
    private var isFilterAvailable = true
    private var isUploading = false // Flag to prevent duplicate uploads
    private var selectedTagPeopleList: ArrayList<TagPeople> = arrayListOf()
    private var selectedLocationLabel: String? = null
    private var selectedLocationLat: Double? = null
    private var selectedLocationLng: Double? = null
    private var selectedLocationX: Float? = null  // Normalized x position (0.0-1.0)
    private var selectedLocationY: Float? = null  // Normalized y position (0.0-1.0)
    private var locationOverlayView: View? = null  // Reference to the location overlay view
    private var selectedUserTagX: Float? = null  // Normalized x position (0.0-1.0) for first user tag
    private var selectedUserTagY: Float? = null  // Normalized y position (0.0-1.0) for first user tag
    private var userTagOverlayView: View? = null  // Reference to the first user tag overlay view
    private var selectedUserTagId: String? = null  // ID of the first tagged user
    private var selectedUserTagName: String? = null  // Name of the first tagged user
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locationHelper: LocationHelper


    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

            // Check if multiple files are selected
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    handleSelectedMedia(uri)
                }
            } else if (data?.data != null) { // Single file selected
                val uri = data.data!!
                handleSelectedMedia(uri)
            }
        }
    }

    private val customCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val mediaUri = result.data?.getStringExtra(CustomCameraActivity.RESULT_MEDIA_URI)?.let { Uri.parse(it) } ?: return@registerForActivityResult
            val mediaType = result.data?.getStringExtra(CustomCameraActivity.RESULT_MEDIA_TYPE)
            if (mediaType == CustomCameraActivity.MEDIA_TYPE_VIDEO) {
                showVideoPreview(mediaUri)
            } else {
                startCropActivity(mediaUri)
            }
        }
    }

    private val tagPeopleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            val selectedPeopleList = bundle?.getSerializable("selectedPeopleList") as? ArrayList<TagPeople>
            if (selectedPeopleList != null) {
                selectedTagPeopleList = selectedPeopleList
                setTagsFlexList(selectedPeopleList)
                // Update overlays shown directly on the story preview
                updateStoryTagOverlays()
            }
        }
    }

    private fun initLocationTagging() {
        locationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val grantedFine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                val grantedCoarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (grantedFine || grantedCoarse) {
                    // Permission granted, proceed to fetch location
                    locationHelper.checkAndRequestLocation()
                } else {
                    Toast.makeText(this, "Location permission is required to tag location", Toast.LENGTH_SHORT).show()
                }
            }

        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = locationPermissionLauncher,
            locationCallback = { latitude, longitude ->
                val label = getCityStateFromLatLng(latitude, longitude) ?: "Current location"
                selectedLocationLabel = label
                selectedLocationLat = latitude
                selectedLocationLng = longitude
                updateStoryLocationOverlay(label)
            },
            errorCallback = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun getCityStateFromLatLng(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                
                // Try to get full address first (most complete)
                val fullAddress = address.getAddressLine(0)?.trim()
                
                // If full address is not available or too short, build from components
                val locationText = if (fullAddress.isNullOrBlank()) {
                    // Combine address components: thoroughfare, locality, adminArea
                    val parts = mutableListOf<String>()
                    address.thoroughfare?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.locality?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.subLocality?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.adminArea?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    
                    if (parts.isNotEmpty()) {
                        parts.joinToString(", ")
                    } else {
                        // Fallback to subAdminArea or country
                        address.subAdminArea?.trim() ?: address.countryName?.trim() ?: "Unknown location"
                    }
                } else {
                    fullAddress
                }
                
                // Truncate if too long (max 45 characters to leave room for ellipsis)
                val maxLength = 45
                if (locationText.length > maxLength) {
                    locationText.substring(0, maxLength).trim() + "..."
                } else {
                    locationText
                }
            } else null
        } catch (e: Exception) {
            Log.e("CreateStoryActivity", "Geocoder failed: ${e.message}", e)
            null
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize PhotoEditor
        photoEditor = PhotoEditor.Builder(this, binding.photoEditorView)
            .setPinchTextScalable(true)
            .build()
        setupPhotoEditorListener()
        setupPositionMonitoring()
        binding.filterLayout.visibility = View.GONE
        initLocationTagging()

        inItUi()
    }

    private fun inItUi() {

        preferenceManager = PreferenceManager.getInstance(activity)
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()
        giffProgressBar = GiffProgressBar(activity) // 'this' refers to the context
        successGiff = SuccessGiff(activity) // 'this' refers to the context
        progressBar = CustomProgressBar(activity) // 'this' refers to the context
        textEntryDialog = CustomTextEntryDialog(this) { text, color, backgroundResId ->
            lastSelectedTextColor = color
            selectedBackgroundResId = backgroundResId
            activeTextOverlay?.let {
                updateExistingTextOverlay(it, text, color, backgroundResId)
            } ?: run {
                addTextOverlay(text, color, backgroundResId)
            }
            activeTextOverlay = null
        }

        // Check and request necessary permissions
        checkPermissions()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.doneButton.setOnClickListener {
            Log.d("CreateStoryActivity", "Done button clicked")
            try {
                saveEditedImage()
            } catch (e: Exception) {
                Log.e("CreateStoryActivity", "Error in done button click: ${e.message}", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.filterButton.setOnClickListener {
            if (!isFilterAvailable) {
                Toast.makeText(activity, "Filters are available for photos only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.filterLayout.visibility == View.VISIBLE){
                binding.filterLayout.visibility = View.GONE
                binding.filterTv.setTextColor(ContextCompat.getColor(activity, R.color.white_60))
                binding.filterIv.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_image_filter))
            }else{
                binding.filterLayout.visibility = View.VISIBLE
                binding.filterTv.setTextColor(ContextCompat.getColor(activity, R.color.blue))
                binding.filterIv.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_image_filter_blue))
            }
        }
        binding.emojiButton.setOnClickListener {
            val emojiEntryDialog = CustomEmojiEntryDialog(this) { emoji ->
                addEmoji(emoji)
            }
            emojiEntryDialog.show()
        }
        binding.addTextButton.setOnClickListener {
            activeTextOverlay = null
            textEntryDialog.show(
                initialColor = lastSelectedTextColor,
                initialBackgroundResId = selectedBackgroundResId
            )
        }

        binding.tagPeopleButton.setOnClickListener {
            val intent = Intent(this, TagPeopleActivity::class.java)
            intent.putExtra("selectedTagPeopleList", selectedTagPeopleList)
            intent.putExtra("searchAllUsers", true) // Search all users (individual + business) for story tagging
            tagPeopleLauncher.launch(intent)
        }

        binding.tagLocationButton.setOnClickListener {
            // Fetch current location and drop a draggable pill overlay on the story
            locationHelper.checkAndRequestLocation()
        }


        individualViewModal.createStoryResult.observe(activity){result->
            // Reset upload flag when upload completes (success or failure)
            isUploading = false
            
            if (result.status == true){
                result.message?.let { msg ->
                  successGiff.show(msg) {
                      navigateToMainActivity(businessesType == business_type_individual)
                  }
                }
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }


        individualViewModal.loading.observe(activity){
            if (it == true){
                giffProgressBar.show() // To show the giff progress bar
            }else{
                giffProgressBar.hide() // To hide the giff progress bar
            }
        }

        individualViewModal.toast.observe(activity){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

        setupSwipeGestures()
    }

    private fun setupSwipeGestures() {
        binding.root.setOnSwipeListener(
            onSwipeRight = {
                // Swipe left -> right: Open messages tab
                val intent = Intent(this, com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity::class.java).apply {
                    putExtra(com.thehotelmedia.android.customClasses.Constants.FROM, com.thehotelmedia.android.customClasses.Constants.notification)
                }
                startActivity(intent)
                finish()
            },
            onSwipeLeft = null // No action for right->left swipe on story creation
        )
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 14 and above: request permissions for CAMERA and Photo Picker
            requestPermissionForAndroid14()
        } else {
            // Android 13 and below: request READ_EXTERNAL_STORAGE and CAMERA permissions
            requestPermissionForOlderVersions()
        }
    }

    // For Android 14 and above: request the camera and Photo Picker permission
    private fun requestPermissionForAndroid14() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            showPhotoVideoDialog() // Show the dialog if permissions are granted
        } else {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE)
        }
    }

    // For Android 13 and below: request storage and camera permissions
    private fun requestPermissionForOlderVersions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            showPhotoVideoDialog() // Show the dialog if permissions are granted
        } else {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE)
        }
    }



    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    showPhotoVideoDialog()
                } else {
                    Toast.makeText(this, "Permission required to access storage", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveEditedImage() // Retry saving after permission is granted
                } else {
                    Toast.makeText(this, "Permission denied! Cannot save image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showPhotoVideoDialog() {
        // Create an instance of PhotoVideoDialog
        val dialog = PhotoVideoDialog(
            activity = this,
            title = "Upload",
            photoText = "Gallery",
            videoText = "Camera",
            photoIconResId = R.drawable.ic_gallery_blue,
            videoIconResId = R.drawable.ic_camera_blue,
            photoClickListener = { pickMultipleMedia() },
            videoClickListener = { launchCustomCamera() },
            onDismissListener = {
                // Handle dialog dismiss event
//                println("Dialog dismissed")
            },
            autoCancel = true
        )
        // Show the dialog
        dialog.show()
    }



    private fun pickMultipleMedia() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        pickMediaLauncher.launch(intent)
    }

    private fun handleSelectedMedia(uri: Uri) {
        val mimeType = contentResolver.getType(uri)

        // Check if the MIME type starts with "image/"
        if (mimeType?.startsWith("image/") == true) {
            startCropActivity(uri)
        }
        // Check if the MIME type starts with "video/"
        else if (mimeType?.startsWith("video/") == true) {
            showVideoPreview(uri)
        }
    }
    private fun captureOverlayAndTrimVideo(uri: Uri) {
        // Ensure overlays are visible before capturing
        ensureOverlaysVisible()
        
        binding.photoEditorView.postDelayed({
            try {
                // Capture only the overlay views (text/emojis), not the video
                val (overlayBitmap, overlayInfos) = captureOverlayBitmap()
                
                if (overlayBitmap == null) {
                    Log.w("CreateStoryActivity", "No overlay bitmap captured, proceeding without overlay")
                }
                
                val savedVideoUri = saveVideo(uri) ?: run {
                    Log.e("CreateStoryActivity", "Failed to save video")
                    runOnUiThread {
                        Toast.makeText(this, "Unable to prepare video", Toast.LENGTH_SHORT).show()
                    }
                    return@postDelayed
                }
                
                // Save overlay bitmap to a file and pass it to video trimmer
                val overlayFile = overlayBitmap?.let { saveOverlayBitmap(it) }
                if (overlayFile != null) {
                    Log.d("CreateStoryActivity", "Overlay saved to: ${overlayFile.absolutePath}")
                }
                showVideoTrimmer(savedVideoUri, overlayFile, overlayInfos)
            } catch (e: Exception) {
                Log.e("CreateStoryActivity", "Error capturing overlay: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Error processing video: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                // Fallback to normal video trimmer without overlay
                try {
                    val savedVideoUri = saveVideo(uri) ?: run {
                        runOnUiThread {
                            Toast.makeText(this, "Unable to prepare video", Toast.LENGTH_SHORT).show()
                        }
                        return@postDelayed
                    }
                    showVideoTrimmer(savedVideoUri, null, emptyList())
                } catch (e2: Exception) {
                    Log.e("CreateStoryActivity", "Error in fallback: ${e2.message}", e2)
                    runOnUiThread {
                        Toast.makeText(this, "Failed to process video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }, 200)
    }
    
    data class OverlayInfo(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val type: String // "TEXT" or "EMOJI"
    )
    
    private fun captureOverlayBitmap(): Pair<Bitmap?, List<OverlayInfo>> {
        return try {
            // Create a bitmap to capture only the overlay views (text/emojis)
            val width = binding.photoEditorView.width
            val height = binding.photoEditorView.height
            if (width <= 0 || height <= 0) {
                Log.e("CreateStoryActivity", "Invalid view dimensions: $width x $height")
                return Pair(null, emptyList())
            }
            
            // Create a transparent bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.TRANSPARENT) // Make it transparent
            val canvas = Canvas(bitmap)
            
            // Store overlay positions
            val overlayInfos = mutableListOf<OverlayInfo>()
            
            // Draw only the overlay children (text/emoji views), not the source image
            val childCount = binding.photoEditorView.childCount
            var overlayCount = 0
            for (i in 0 until childCount) {
                val child = binding.photoEditorView.getChildAt(i) ?: continue
                // Skip the source image view - only capture overlay views
                if (child != binding.photoEditorView.source && child.visibility == View.VISIBLE) {
                    // Check if this is a text overlay
                    val textView = child.findViewById<TextView>(R.id.tvPhotoEditorText)
                    val isTextOverlay = textView != null
                    
                    // Check if this is an emoji overlay (typically a ViewGroup with ImageView children)
                    // Also check if the child itself is an ImageView (some emoji implementations use ImageView directly)
                    val isEmojiOverlay = if (child is ImageView && child.drawable != null) {
                        true // Direct ImageView with drawable
                    } else if (child is ViewGroup) {
                        var hasEmojiImage = false
                        for (j in 0 until child.childCount) {
                            val grandChild = child.getChildAt(j)
                            if (grandChild is ImageView && grandChild.drawable != null) {
                                hasEmojiImage = true
                                break
                            }
                        }
                        hasEmojiImage
                    } else {
                        false
                    }
                    
                    if (isTextOverlay || isEmojiOverlay) {
                        // Ensure text view is visible and properly configured before capturing
                        if (isTextOverlay && textView != null) {
                            textView.visibility = View.VISIBLE
                            textView.alpha = 1f
                            // Force a layout pass to ensure text is rendered
                            textView.requestLayout()
                        }
                        
                        overlayCount++
                        // Get the view's bounds in parent coordinates (accounts for both layout and translation)
                        val bounds = android.graphics.Rect()
                        child.getHitRect(bounds)
                        
                        val actualLeft = bounds.left
                        val actualTop = bounds.top
                        val childWidth = bounds.width().coerceAtLeast(1)
                        val childHeight = bounds.height().coerceAtLeast(1)
                        
                        // Store the actual position for proper scaling later
                        overlayInfos.add(OverlayInfo(
                            left = actualLeft,
                            top = actualTop,
                            width = childWidth,
                            height = childHeight,
                            type = if (isTextOverlay) "TEXT" else "EMOJI"
                        ))
                        
                        // Draw the child at its actual position using the same bounds
                        // For both text and emoji overlays, ensure the view is properly laid out before drawing
                        if (isTextOverlay || isEmojiOverlay) {
                            // Ensure overlay view is properly measured and laid out
                            if (child is ViewGroup) {
                                child.measure(
                                    View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY)
                                )
                                child.layout(0, 0, childWidth, childHeight) // Layout relative to (0,0) since we'll translate
                                
                                // For text overlays, also ensure the TextView inside is properly measured
                                if (isTextOverlay && textView != null) {
                                    textView.measure(
                                        View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY)
                                    )
                                    textView.layout(0, 0, childWidth, childHeight)
                                }
                            } else if (child is ImageView) {
                                // For direct ImageView (emoji), ensure it's measured
                                child.measure(
                                    View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY)
                                )
                                child.layout(0, 0, childWidth, childHeight)
                            }
                            
                            // Log overlay view details for debugging
                            if (overlayCount == 1) {
                                if (isTextOverlay) {
                                    Log.d("CreateStoryActivity", "Text overlay details: class=${child.javaClass.simpleName}, width=$childWidth, height=$childHeight, textView=${textView?.text}, textView.visibility=${textView?.visibility}")
                                    if (child is ViewGroup) {
                                        Log.d("CreateStoryActivity", "Text ViewGroup has ${child.childCount} children")
                                        for (j in 0 until child.childCount) {
                                            val grandChild = child.getChildAt(j)
                                            Log.d("CreateStoryActivity", "  Child $j: ${grandChild.javaClass.simpleName}, visibility=${grandChild.visibility}")
                                        }
                                    }
                                } else {
                                    Log.d("CreateStoryActivity", "Emoji view details: class=${child.javaClass.simpleName}, width=$childWidth, height=$childHeight, drawable=${if (child is ImageView) child.drawable != null else "N/A"}")
                                    if (child is ViewGroup) {
                                        Log.d("CreateStoryActivity", "Emoji ViewGroup has ${child.childCount} children")
                                        for (j in 0 until child.childCount) {
                                            val grandChild = child.getChildAt(j)
                                            Log.d("CreateStoryActivity", "  Child $j: ${grandChild.javaClass.simpleName}, visibility=${grandChild.visibility}")
                                        }
                                    }
                                }
                            }
                        }
                        
                        canvas.save()
                        canvas.translate(actualLeft.toFloat(), actualTop.toFloat())
                        // Draw with hardware layer disabled to ensure proper rendering for both text and emoji
                        val wasHardwareAccelerated = child.isHardwareAccelerated
                        if (wasHardwareAccelerated) {
                            child.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            // Also disable hardware acceleration for TextView inside if it's a text overlay
                            if (isTextOverlay && textView != null && textView.isHardwareAccelerated) {
                                textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }
                        }
                        child.draw(canvas)
                        if (wasHardwareAccelerated) {
                            child.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            // Restore hardware acceleration for TextView
                            if (isTextOverlay && textView != null) {
                                textView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            }
                        }
                        canvas.restore()
                        
                        // Verify the drawn content by sampling pixels from the bitmap
                        if (isTextOverlay || isEmojiOverlay) {
                            // Sample pixels from the center of the drawn area to verify content
                            val centerX = (actualLeft + childWidth / 2).coerceIn(0, width - 1)
                            val centerY = (actualTop + childHeight / 2).coerceIn(0, height - 1)
                            val samplePixel = bitmap.getPixel(centerX, centerY)
                            val alpha = (samplePixel shr 24) and 0xFF
                            val r = (samplePixel shr 16) and 0xFF
                            val g = (samplePixel shr 8) and 0xFF
                            val b = samplePixel and 0xFF
                            val overlayType = if (isTextOverlay) "TEXT" else "EMOJI"
                            Log.d("CreateStoryActivity", "$overlayType overlay drawn - sampled pixel at center ($centerX, $centerY): ARGB($alpha, $r, $g, $b)")
                            
                            // Also sample a few more pixels around the area
                            var nonTransparentCount = 0
                            val sampleSize = minOf(9, childWidth * childHeight / 100) // Sample ~1% of pixels
                            for (sy in actualTop until minOf(actualTop + childHeight, height) step maxOf(1, childHeight / 3)) {
                                for (sx in actualLeft until minOf(actualLeft + childWidth, width) step maxOf(1, childWidth / 3)) {
                                    val pixel = bitmap.getPixel(sx, sy)
                                    if ((pixel shr 24) and 0xFF > 0) {
                                        nonTransparentCount++
                                    }
                                }
                            }
                            Log.d("CreateStoryActivity", "$overlayType area sampled: $nonTransparentCount non-transparent pixels found")
                        }
                        
                        Log.d("CreateStoryActivity", "Captured overlay: type=${if (isTextOverlay) "TEXT" else "EMOJI"}, pos=($actualLeft, $actualTop), size=($childWidth, $childHeight), bounds=(${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})")
                    }
                }
            }
            Log.d("CreateStoryActivity", "Captured $overlayCount overlay(s) from $childCount children")
            
            // Verify bitmap has content by checking if it's not completely transparent
            if (overlayCount > 0) {
                var hasContent = false
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                for (pixel in pixels) {
                    if ((pixel shr 24) and 0xFF != 0) { // Check alpha channel
                        hasContent = true
                        break
                    }
                }
                if (!hasContent) {
                    Log.w("CreateStoryActivity", "Overlay bitmap appears to be empty (all transparent) despite $overlayCount overlays")
                } else {
                    Log.d("CreateStoryActivity", "Overlay bitmap has content, size: ${bitmap.width}x${bitmap.height}")
                }
            }
            
            Pair(bitmap, overlayInfos)
        } catch (e: Exception) {
            Log.e("CreateStoryActivity", "Error capturing overlay bitmap: ${e.message}", e)
            Pair(null, emptyList())
        }
    }
    
    private fun saveOverlayBitmap(bitmap: Bitmap): File? {
        return try {
            val overlayDir = File(cacheDir, "video_overlays")
            if (!overlayDir.exists()) {
                overlayDir.mkdirs()
            }
            val overlayFile = File(overlayDir, "overlay_${System.currentTimeMillis()}.png")
            FileOutputStream(overlayFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            overlayFile
        } catch (e: Exception) {
            Log.e("CreateStoryActivity", "Error saving overlay bitmap: ${e.message}", e)
            null
        }
    }
    
    private fun checkVideoDurationAndTrim(uri: Uri) {
        val savedVideoUri = saveVideo(uri) ?: run {
            Toast.makeText(this, "Unable to prepare video", Toast.LENGTH_SHORT).show()
            return
        }
        showVideoTrimmer(savedVideoUri, null, emptyList())
    }

    private fun saveVideo(uri: Uri): Uri? {
        val contentResolver = applicationContext.contentResolver
        val inputStream = contentResolver.openInputStream(uri)

        // Create a unique file name using the current time in milliseconds
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFileName = "video_${System.currentTimeMillis()}.mp4" // Unique filename
        val outputFile = File(outputDir, outputFileName)

        return try {
            val outputStream = FileOutputStream(outputFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input?.copyTo(output) // Copy the video data
                }
            }
            Uri.fromFile(outputFile) // Return the URI of the saved video
        } catch (e: IOException) {
            Log.e("CreatePostActivity", "Failed to save video: ${e.message}")
            null
        }
    }
    private fun showVideoTrimmer(uri: Uri, overlayFile: File?, overlayInfos: List<OverlayInfo>) {
        val intent = Intent(this, VideoTrimmerActivity::class.java)
        intent.putExtra("video_uri", uri.toString())
        intent.putExtra("FROM", "CreateStory")
        overlayFile?.absolutePath?.let {
            intent.putExtra("overlay_bitmap_path", it)
            // Pass PhotoEditorView dimensions for proper scaling
            val viewWidth = binding.photoEditorView.width
            val viewHeight = binding.photoEditorView.height
            intent.putExtra("overlay_view_width", viewWidth)
            intent.putExtra("overlay_view_height", viewHeight)
            
            // Pass overlay positions as JSON string
            val overlayPositionsJson = overlayInfos.joinToString(separator = "|") { info ->
                "${info.left},${info.top},${info.width},${info.height},${info.type}"
            }
            intent.putExtra("overlay_positions", overlayPositionsJson)
            
            Log.d("CreateStoryActivity", "Passing overlay dimensions: ${viewWidth}x${viewHeight}, ${overlayInfos.size} overlays")
            Log.d("CreateStoryActivity", "Overlay positions: $overlayPositionsJson")
        }
        // Pass selected tagged people so they can be attached to the story after trimming
        if (selectedTagPeopleList.isNotEmpty()) {
            intent.putExtra("selectedTagPeopleList", selectedTagPeopleList)
        }
        startActivity(intent)
    }

    private fun launchCustomCamera() {
        val intent = Intent(this, CustomCameraActivity::class.java).apply {
            putExtra(CustomCameraActivity.EXTRA_CAMERA_TITLE, getString(R.string.create_story_large))
        }
        customCameraLauncher.launch(intent)
    }

    private fun startCropActivity(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {
            // Set toolbar and status bar colors
            setToolbarColor(ContextCompat.getColor(activity, R.color.black))
            setStatusBarColor(ContextCompat.getColor(activity, R.color.black))

            // Set toolbar title and widget colors
            setToolbarWidgetColor(ContextCompat.getColor(activity, R.color.white))
            setToolbarTitle("Edit Image")

            // Lock the aspect ratio to 3:4 (Portrait)
            setAspectRatioOptions(0, AspectRatio("Portrait", 3f, 4f))
            setFreeStyleCropEnabled(false)  // Disable freeform crop to enforce the aspect ratio
        }

        // Start UCrop with the destination URI and options
        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .withAspectRatio(3f, 4f)  // Set the default aspect ratio as portrait
            .withMaxResultSize(1080, 1440)  // Set the max size if necessary
            .start(this)
    }

    // Handle result from UCrop
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                croppedImageUri = it
                loadImage(it) // Load cropped image into the editor
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.printStackTrace()
            Log.e("EditImageActivity", "Crop error: ${cropError?.message}")
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // Handle cancel action here
            if (croppedImageUri == null){
                onBackPressedDispatcher.onBackPressed()
            }

        }
    }

    private fun loadImage(uri: Uri) {
        hideVideoPreview()
        updateFilterAvailability(true)
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .apply(
                RequestOptions()
                    .error(android.R.color.darker_gray)
            )
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Set the scaled bitmap to the PhotoEditorView
                    binding.photoEditorView.source.setImageBitmap(resource)
                    // Adjust the ImageView's scaleType if necessary
                    binding.photoEditorView.source.scaleType = ImageView.ScaleType.FIT_CENTER
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("EditImageActivity", "Failed to load image")
                }
            })
        setFilterAdapter(uri)
    }

    private fun setFilterAdapter(uri: Uri) {
        // Define filters and pass them to the adapter
        val filterList = listOf(
            PhotoFilter.NONE,
            PhotoFilter.AUTO_FIX,
            PhotoFilter.BLACK_WHITE,
            PhotoFilter.BRIGHTNESS,
            PhotoFilter.CONTRAST,
            PhotoFilter.CROSS_PROCESS,
            PhotoFilter.DOCUMENTARY,
            PhotoFilter.DUE_TONE,
            PhotoFilter.FILL_LIGHT,
            PhotoFilter.FISH_EYE,
            PhotoFilter.FLIP_VERTICAL,
            PhotoFilter.FLIP_HORIZONTAL,
            PhotoFilter.GRAIN,
            PhotoFilter.GRAY_SCALE,
            PhotoFilter.LOMISH,
            PhotoFilter.NEGATIVE,
            PhotoFilter.POSTERIZE,
            PhotoFilter.ROTATE,
            PhotoFilter.SATURATE,
            PhotoFilter.SEPIA,
            PhotoFilter.SHARPEN,
            PhotoFilter.TEMPERATURE,
            PhotoFilter.TINT,
            PhotoFilter.VIGNETTE

        )
        // Set up the adapter with the filter list
        val filterAdapter = FilterAdapter(activity, photoEditor, filterList, uri)
        binding.filterRecyclerView.adapter = filterAdapter
    }

    private fun setupPhotoEditorListener() {
        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
                rootView ?: return
                binding.photoEditorView.post {
                    configureTextOverlay(rootView)
                }
            }

            override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
                if (viewType == ViewType.TEXT || viewType == ViewType.EMOJI) {
                    binding.photoEditorView.postDelayed({
                        ensureOverlaysVisible()
                        if (viewType == ViewType.TEXT) {
                            getLatestTextOverlay()?.let { overlay ->
                                configureTextOverlay(overlay)
                                removeGravityConstraints(overlay)
                            }
                        } else if (viewType == ViewType.EMOJI) {
                            val childCount = binding.photoEditorView.childCount
                            if (childCount > 0) {
                                val lastChild = binding.photoEditorView.getChildAt(childCount - 1)
                                if (lastChild != binding.photoEditorView.source) {
                                    removeGravityConstraints(lastChild)
                                }
                            }
                        }
                    }, 50)
                }
            }

            override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
                if (viewType == ViewType.TEXT) {
                    // Check if any location overlay was removed
                    val hasLocationOverlay = (0 until binding.photoEditorView.childCount)
                        .map { binding.photoEditorView.getChildAt(it) }
                        .any { view ->
                            view.getTag(R.id.story_location_overlay_view) as? Boolean ?: false
                        }
                    
                    if (!hasLocationOverlay && locationOverlayView != null) {
                        // Location overlay was removed - clear location data
                        Log.d("CreateStoryActivity", "Location overlay removed - clearing location data")
                        locationOverlayView = null
                        selectedLocationLabel = null
                        selectedLocationLat = null
                        selectedLocationLng = null
                        selectedLocationX = null
                        selectedLocationY = null
                    } else if (hasLocationOverlay) {
                        // Update locationOverlayView reference if it changed
                        val currentLocationOverlay = (0 until binding.photoEditorView.childCount)
                            .map { binding.photoEditorView.getChildAt(it) }
                            .firstOrNull { view ->
                                view.getTag(R.id.story_location_overlay_view) as? Boolean ?: false
                            }
                        if (currentLocationOverlay != null && currentLocationOverlay != locationOverlayView) {
                            locationOverlayView = currentLocationOverlay
                        }
                    }
                    
                    // Check if the first user tag overlay was removed
                    val hasFirstUserTag = (0 until binding.photoEditorView.childCount)
                        .map { binding.photoEditorView.getChildAt(it) }
                        .any { view ->
                            view.getTag(R.id.story_user_tag_first) as? Boolean ?: false
                        }
                    
                    if (!hasFirstUserTag && userTagOverlayView != null) {
                        // User tag overlay was removed - clear user tag data
                        Log.d("CreateStoryActivity", "User tag overlay removed - clearing user tag data")
                        userTagOverlayView = null
                        selectedUserTagId = null
                        selectedUserTagName = null
                        selectedUserTagX = null
                        selectedUserTagY = null
                    } else if (hasFirstUserTag) {
                        // Update userTagOverlayView reference if it changed
                        val currentUserTagOverlay = (0 until binding.photoEditorView.childCount)
                            .map { binding.photoEditorView.getChildAt(it) }
                            .firstOrNull { view ->
                                view.getTag(R.id.story_user_tag_first) as? Boolean ?: false
                            }
                        if (currentUserTagOverlay != null && currentUserTagOverlay != userTagOverlayView) {
                            userTagOverlayView = currentUserTagOverlay
                        }
                    }
                    
                    // Check if activeTextOverlay was removed
                    if (activeTextOverlay != null) {
                        val stillPresent = (0 until binding.photoEditorView.childCount)
                            .map { binding.photoEditorView.getChildAt(it) }
                            .any { it == activeTextOverlay }
                        if (!stillPresent) {
                            activeTextOverlay = null
                        }
                    }
                }
            }

            override fun onStartViewChangeListener(viewType: ViewType?) {
                // Store positions BEFORE PhotoEditor potentially moves them to center
                // This prevents the flicker by capturing the position before the reset happens
                val childCount = binding.photoEditorView.childCount
                for (i in 0 until childCount) {
                    val child = binding.photoEditorView.getChildAt(i)
                    child?.let { view ->
                        val isOverlay = view.findViewById<TextView>(R.id.tvPhotoEditorText) != null
                        if (isOverlay) {
                            // Store the position before drag ends
                            viewPositions[view] = Pair(view.x, view.y)
                        }
                    }
                }
            }

                            override fun onStopViewChangeListener(viewType: ViewType?) {
                // Immediately restore positions to prevent flicker
                // First, try immediate restoration (synchronous if possible)
                val childCount = binding.photoEditorView.childCount
                for (i in 0 until childCount) {
                    val child = binding.photoEditorView.getChildAt(i)
                    child?.let { view ->
                        val isOverlay = view.findViewById<TextView>(R.id.tvPhotoEditorText) != null
                        if (isOverlay && viewPositions.containsKey(view)) {
                            val storedPos = viewPositions[view]!!
                            val currentY = view.y
                            
                            // Check if position was reset to center
                            val centerY = binding.photoEditorView.height / 2f
                            val isNearCenter = kotlin.math.abs(currentY - centerY) < 100
                            val wasNotAtCenter = kotlin.math.abs(storedPos.second - centerY) > 100
                            
                            // If Y was reset to center, immediately restore it
                            if (isNearCenter && wasNotAtCenter) {
                                view.x = storedPos.first
                                view.y = storedPos.second
                            }
                            
                            removeGravityConstraints(view)
                            
                            // Update location position if this is the location overlay
                            val isLocationOverlay = view.getTag(R.id.story_location_overlay_view) as? Boolean ?: false
                            if (isLocationOverlay) {
                                // Update normalized position when location overlay is moved
                                updateLocationPosition(view)
                            }
                        }
                    }
                }
                
                // Also use post as a backup to catch any delayed position changes
                binding.photoEditorView.post {
                    val childCount = binding.photoEditorView.childCount
                    for (i in 0 until childCount) {
                        val child = binding.photoEditorView.getChildAt(i)
                        child?.let { view ->
                            val isOverlay = view.findViewById<TextView>(R.id.tvPhotoEditorText) != null
                            if (isOverlay && viewPositions.containsKey(view)) {
                                val storedPos = viewPositions[view]!!
                                val currentX = view.x
                                val currentY = view.y
                                
                                // Check if position was reset to center
                                val centerY = binding.photoEditorView.height / 2f
                                val isNearCenter = kotlin.math.abs(currentY - centerY) < 100
                                val wasNotAtCenter = kotlin.math.abs(storedPos.second - centerY) > 100
                                
                                // If Y was reset to center, restore it
                                if (isNearCenter && wasNotAtCenter) {
                                    view.x = storedPos.first
                                    view.y = storedPos.second
                                } else {
                                    // Update stored position if it's a valid new position
                                    if (kotlin.math.abs(currentY - storedPos.second) > 10 && 
                                        kotlin.math.abs(currentY - centerY) > 100) {
                                        viewPositions[view] = Pair(currentX, currentY)
                                    }
                                }
                                
                                // Update location position if this is the location overlay
                                val isLocationOverlay = view.getTag(R.id.story_location_overlay_view) as? Boolean ?: false
                                if (isLocationOverlay) {
                                    // Update normalized position when location overlay is moved
                                    updateLocationPosition(view)
                                }
                                
                                // Update user tag position if this is the first user tag overlay
                                val isFirstUserTag = view.getTag(R.id.story_user_tag_first) as? Boolean ?: false
                                if (isFirstUserTag) {
                                    updateUserTagPosition(view)
                                }
                            }
                        }
                    }
                }
            }

            override fun onTouchSourceImage(event: MotionEvent?) {}
        })
    }

    // Add emoji to the image
    private fun addEmoji(emoji: String) {
        photoEditor.addEmoji(emoji)
        // Ensure emoji is visible after adding
        binding.photoEditorView.postDelayed({
            ensureOverlaysVisible()
        }, 100)
    }
    
    private fun ensureOverlaysVisible() {
        val childCount = binding.photoEditorView.childCount
        var overlayCount = 0
        for (i in 0 until childCount) {
            val child = binding.photoEditorView.getChildAt(i)
            if (child != null && child != binding.photoEditorView.source) {
                child.visibility = View.VISIBLE
                child.alpha = 1f
                // Also ensure all children of the overlay are visible
                if (child is ViewGroup) {
                    for (j in 0 until child.childCount) {
                        val grandChild = child.getChildAt(j)
                        grandChild?.visibility = View.VISIBLE
                        grandChild?.alpha = 1f
                    }
                }
                overlayCount++
            }
        }
        if (overlayCount > 0) {
            Log.d("CreateStoryActivity", "Ensured $overlayCount overlay(s) visible")
        }
    }
    private fun addTextOverlay(text: String, color: Int, backgroundResId: Int) {
        val textStyleBuilder = TextStyleBuilder().apply {
            withTextColor(color)
            ContextCompat.getDrawable(activity, backgroundResId)?.let { withBackgroundDrawable(it) }
            ResourcesCompat.getFont(activity, R.font.comic_regular)?.let { withTextFont(it) }
            // Remove Gravity.CENTER to allow free positioning
        }
        photoEditor.addText(text, textStyleBuilder)
        binding.photoEditorView.postDelayed({
            getLatestTextOverlay()?.let { overlay ->
                // Ensure text overlay is visible
                overlay.visibility = View.VISIBLE
                overlay.alpha = 1f
                overlay.setTag(R.id.story_text_background_res_id, backgroundResId)
                configureTextOverlay(overlay)
            }
            // Also ensure all overlays are visible
            ensureOverlaysVisible()
        }, 100)
    }

    private fun updateExistingTextOverlay(overlay: View, text: String, color: Int, backgroundResId: Int) {
        val textStyleBuilder = TextStyleBuilder().apply {
            withTextColor(color)
            ContextCompat.getDrawable(activity, backgroundResId)?.let { withBackgroundDrawable(it) }
            ResourcesCompat.getFont(activity, R.font.comic_regular)?.let { withTextFont(it) }
            // Remove Gravity.CENTER to allow free positioning
        }
        overlay.setTag(R.id.story_text_background_res_id, backgroundResId)
        photoEditor.editText(overlay, text, textStyleBuilder)
        binding.photoEditorView.post {
            configureTextOverlay(overlay)
        }
    }

    private fun showVideoPreview(uri: Uri) {
        currentVideoUri = uri
        // Keep photoEditorView visible but make source image transparent so text/emojis can overlay on video
        binding.photoEditorView.visibility = View.VISIBLE
        binding.photoEditorView.source.alpha = 0f // Make source image transparent
        binding.photoEditorView.background = null // Remove background
        binding.videoPreview.visibility = View.VISIBLE
        // Bring photoEditorView to front so text/emojis appear on top of video
        binding.photoEditorView.bringToFront()
        
        // Ensure all child views (text/emoji overlays) are visible immediately
        ensureOverlaysVisible()
        
        updateFilterAvailability(false)

        val playableUri = normalizeVideoUri(uri) ?: run {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("CreateStoryActivity", "Showing video at $playableUri")

        val player = videoPlayer ?: ExoPlayer.Builder(this).build().also {
            videoPlayer = it
            binding.videoPreview.player = it
        }
        player.setMediaItem(MediaItem.fromUri(playableUri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.prepare()
        player.playWhenReady = true
        player.play()
    }

    private fun hideVideoPreview() {
        videoPlayer?.pause()
        videoPlayer?.stop()
        binding.videoPreview.player = null
        videoPlayer?.release()
        videoPlayer = null
        binding.videoPreview.visibility = View.GONE
        binding.photoEditorView.visibility = View.VISIBLE
        // Restore photoEditorView source image visibility
        binding.photoEditorView.source.alpha = 1f
        binding.photoEditorView.background = ContextCompat.getDrawable(this, R.drawable.background_14_transparent)
        currentVideoUri = null
    }

    private fun updateFilterAvailability(isAvailable: Boolean) {
        isFilterAvailable = isAvailable
        binding.filterButton.alpha = if (isAvailable) 1f else 0.5f
        if (isAvailable) {
            binding.filterTv.setTextColor(ContextCompat.getColor(activity, R.color.text_color_60))
            binding.filterIv.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_image_filter))
        } else {
            binding.filterLayout.visibility = View.GONE
            binding.filterTv.setTextColor(ContextCompat.getColor(activity, R.color.white_60))
            binding.filterIv.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_image_filter))
        }
    }

    private fun normalizeVideoUri(uri: Uri): Uri? {
        return when {
            uri.scheme.isNullOrEmpty() -> {
                val path = uri.path ?: return null
                val file = File(path)
                if (file.exists()) Uri.fromFile(file) else null
            }
            uri.scheme == ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: return null
                val file = File(path)
                if (file.exists()) uri else null
            }
            else -> uri
        }
    }

    private fun getLatestTextOverlay(): View? {
        val parent = binding.photoEditorView
        for (index in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(index)
            if (child.findViewById<TextView>(R.id.tvPhotoEditorText) != null) {
                return child
            }
        }
        return null
    }

    private fun removeGravityConstraints(view: View?) {
        view?.let { v ->
            val layoutParams = v.layoutParams
            if (layoutParams is android.widget.FrameLayout.LayoutParams) {
                // Remove any gravity constraints that might force centering
                if (layoutParams.gravity != android.view.Gravity.NO_GRAVITY) {
                    layoutParams.gravity = android.view.Gravity.NO_GRAVITY
                    v.layoutParams = layoutParams
                }
                // Also remove any margin constraints that might limit positioning
                layoutParams.leftMargin = 0
                layoutParams.topMargin = 0
                layoutParams.rightMargin = 0
                layoutParams.bottomMargin = 0
                v.layoutParams = layoutParams
            }
            // Ensure the view uses absolute positioning (x, y coordinates)
            // PhotoEditor uses View.x and View.y for positioning
        }
    }

    private val viewPositions = mutableMapOf<View, Pair<Float, Float>>()
    private var positionMonitoringHandler: android.os.Handler? = null
    private var positionMonitoringRunnable: Runnable? = null
    
    private fun setupPositionMonitoring() {
        // Stop any existing monitoring
        positionMonitoringRunnable?.let { positionMonitoringHandler?.removeCallbacks(it) }
        
        // Use a post handler to periodically check and restore positions
        // This acts as a safety net in case positions are reset outside of drag events
        positionMonitoringHandler = android.os.Handler(android.os.Looper.getMainLooper())
        positionMonitoringRunnable = object : Runnable {
            override fun run() {
                // Ensure overlays are visible (especially important for video preview)
                if (currentVideoUri != null) {
                    ensureOverlaysVisible()
                }
                
                val childCount = binding.photoEditorView.childCount
                for (i in 0 until childCount) {
                    val child = binding.photoEditorView.getChildAt(i)
                    child?.let { view ->
                        // Check if this is a text or emoji overlay view
                        val isOverlay = view.findViewById<TextView>(R.id.tvPhotoEditorText) != null
                        
                        if (isOverlay && viewPositions.containsKey(view)) {
                            val storedPos = viewPositions[view]!!
                            val currentX = view.x
                            val currentY = view.y
                            
                            // If Y was reset to center (within 100px of center), restore stored Y
                            val centerY = binding.photoEditorView.height / 2f
                            val isNearCenter = kotlin.math.abs(currentY - centerY) < 100
                            val wasNotAtCenter = kotlin.math.abs(storedPos.second - centerY) > 100
                            
                            // Only restore if Y was reset to center and we have a valid stored position
                            if (isNearCenter && wasNotAtCenter) {
                                view.x = storedPos.first
                                view.y = storedPos.second
                            } else if (currentY > 0 && currentY < binding.photoEditorView.height) {
                                // Update stored position if it's a valid new position (not at center)
                                if (kotlin.math.abs(currentY - centerY) > 100) {
                                    viewPositions[view] = Pair(currentX, currentY)
                                }
                            }
                        }
                    }
                }
                positionMonitoringHandler?.postDelayed(this, 100) // Check every 100ms
            }
        }
        positionMonitoringRunnable?.let { positionMonitoringHandler?.post(it) }
    }

    private fun configureTextOverlay(overlay: View) {
        val textView = overlay.findViewById<TextView>(R.id.tvPhotoEditorText) ?: return
        val editIcon = overlay.findViewById<ImageView>(R.id.imgPhotoEditorEdit)
        val isTagOverlay = overlay.getTag(R.id.story_tag_overlay_view) as? Boolean ?: false
        val isLocationOverlay = overlay.getTag(R.id.story_location_overlay_view) as? Boolean ?: false

        val detectedBackgroundRes = (overlay.getTag(R.id.story_text_background_res_id) as? Int) ?: run {
            val currentBackground = textView.background
            val transparent = ContextCompat.getDrawable(this, R.drawable.text_background_transparent)
            val white = ContextCompat.getDrawable(this, R.drawable.text_background_white)
            val black = ContextCompat.getDrawable(this, R.drawable.text_background_black)
            when (currentBackground?.constantState) {
                white?.constantState -> R.drawable.text_background_white
                black?.constantState -> R.drawable.text_background_black
                else -> R.drawable.text_background_transparent
            }
        }
        overlay.setTag(R.id.story_text_background_res_id, detectedBackgroundRes)

        // For normal text overlays, keep existing behavior (editable with background).
        // For tag overlays (@username) and location overlays, we want blue, non-editable pill style.
        if (isTagOverlay || isLocationOverlay) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.blue))
            textView.background = ContextCompat.getDrawable(this, R.drawable.story_tag_background_blue)
            if (isLocationOverlay) {
                // Match viewing UI: 12sp text size, ellipsize, padding
                textView.textSize = 12f
                textView.maxLines = 1
                textView.ellipsize = android.text.TextUtils.TruncateAt.END
                textView.setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )
                applyLocationIcon(textView)
            } else {
                // Ensure people tags don't get an icon
                clearCompoundDrawables(textView)
            }
            editIcon?.visibility = View.GONE
            editIcon?.setOnClickListener(null)
        } else {
            textView.background = ContextCompat.getDrawable(this, detectedBackgroundRes)
            clearCompoundDrawables(textView)
            editIcon?.visibility = View.VISIBLE
        }

        // Remove gravity constraints to allow free positioning
        removeGravityConstraints(overlay)

        if (!isTagOverlay) {
            editIcon?.setOnClickListener {
                activeTextOverlay = overlay
                textEntryDialog.show(
                    initialText = textView.text.toString(),
                    initialColor = textView.currentTextColor,
                    initialBackgroundResId = detectedBackgroundRes
                )
            }
        }
    }

    private fun hideAllEditIcons() {
        try {
            val parent = binding.photoEditorView ?: return
            val childCount = parent.childCount
            for (index in 0 until childCount) {
                try {
                    val child = parent.getChildAt(index) ?: continue
                    val editIcon = child.findViewById<ImageView>(R.id.imgPhotoEditorEdit)
                    editIcon?.visibility = View.GONE
                } catch (e: Exception) {
                    Log.e("CreateStoryActivity", "Error hiding edit icon at index $index: ${e.message}")
                    // Continue with next child
                }
            }
        } catch (e: Exception) {
            Log.e("CreateStoryActivity", "Error in hideAllEditIcons: ${e.message}", e)
            // Don't crash if hiding icons fails, just log the error
        }
    }

    private fun saveEditedImage() {
        Log.d("CreateStoryActivity", "saveEditedImage called, currentVideoUri: $currentVideoUri")
        
        // Prevent duplicate uploads
        if (isUploading) {
            Log.w("CreateStoryActivity", "Upload already in progress, ignoring duplicate call")
            return
        }
        
        // Handle video case first
        currentVideoUri?.let { uri ->
            Log.d("CreateStoryActivity", "Processing video: $uri")
            // Capture overlay bitmap before going to video trimmer
            captureOverlayAndTrimVideo(uri)
            return
        }
        
        // For images, ensure photoEditorView is visible before saving
        if (binding.photoEditorView.visibility != View.VISIBLE) {
            Log.e("CreateStoryActivity", "Cannot save: photoEditorView is not visible")
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Ensure we have a valid image source
        if (croppedImageUri == null && binding.photoEditorView.source.drawable == null) {
            Log.e("CreateStoryActivity", "Cannot save: no image loaded")
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Request storage permission for Android 9 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                )
                return // Wait for permission result
            }
        }

        binding.photoEditorView.postDelayed({
            // Hide all edit icons before saving
            hideAllEditIcons()
            
            // Remove location overlay from photoEditor before saving
            // This prevents it from being merged into the bitmap
            // The location tag will be rendered as a View in the Story Viewer instead
            removeLocationOverlayFromEditor()
            
            // Remove user tag overlays from photoEditor before saving
            // This prevents them from being merged into the bitmap
            // User tags will be rendered as Views in the Story Viewer instead
            removeUserTagOverlaysFromEditor()
            
            // Give a small delay to ensure UI updates are complete
            binding.photoEditorView.postDelayed({
                val newDir = File(filesDir, "edited_images").apply {
                    if (!exists()) mkdirs()
                }

                val file = File(newDir, "edited_image_${System.currentTimeMillis()}.jpg")

                try {
                    // Set uploading flag to prevent duplicates
                    isUploading = true
                    
                    photoEditor.saveAsFile(file.absolutePath, object : PhotoEditor.OnSaveListener {
                    override fun onSuccess(imagePath: String) {
                        val imageFile = File(imagePath)
                        val originalSize = imageFile.length()
                        Log.d("CreateStoryActivity", "Image saved successfully: ${imageFile.absolutePath}, original size: ${originalSize} bytes")
                        
                        // Compress image to reduce file size and prevent "request entity too large" errors
                        val compressedFile = compressImageIfNeeded(imageFile)
                        val compressedSize = compressedFile.length()
                        val sizeReduction = originalSize - compressedSize
                        Log.d("CreateStoryActivity", "Compressed image size: $compressedSize bytes (reduced by $sizeReduction bytes)")
                        
                        // Check file size and warn if still too large (5MB limit - server may reject larger files)
                        val maxSizeBytes = 5 * 1024 * 1024L // 5MB
                        if (compressedSize > maxSizeBytes) {
                            val sizeMB = compressedSize / (1024.0 * 1024.0)
                            Log.w("CreateStoryActivity", "Image is still large after compression: ${String.format("%.2f", sizeMB)} MB")
                            Toast.makeText(this@CreateStoryActivity, "Image is very large (${String.format("%.1f", sizeMB)} MB). Upload may fail.", Toast.LENGTH_LONG).show()
                        } else {
                            val sizeMB = compressedSize / (1024.0 * 1024.0)
                            Log.d("CreateStoryActivity", "Image compressed to ${String.format("%.2f", sizeMB)} MB - ready for upload")
                        }

                        // Get URI using FileProvider (Recommended for Android 7+)
                        val editedImageUri = FileProvider.getUriForFile(
                            this@CreateStoryActivity,
                            "${packageName}.provider",
                            compressedFile
                        )

                        // Only upload the compressed image (with text/overlays)
                        // Do NOT upload the original croppedImageUri
                        postStory(compressedFile, null)

                        // Return URI as result (but don't upload again)
                        val resultIntent = Intent().apply {
                            putExtra("edited_image_uri", editedImageUri.toString())
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        
                        // Reset flag after upload starts
                        // Note: We don't reset it here because the upload is async
                        // It will be reset in the observer when upload completes
                    }

                    override fun onFailure(exception: Exception) {
                        isUploading = false // Reset flag on failure
                        Toast.makeText(this@CreateStoryActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
                        Log.e("EditImageActivity", "Error: ${exception.message}", exception)
                    }
                })
            } catch (e: SecurityException) {
                Log.e("EditImageActivity", "Permission issue: ${e.message}", e)
                Toast.makeText(this, "Storage permission is required to save the image", Toast.LENGTH_SHORT).show()
            }
                }, 100) // Small delay after hiding icons
        }, 300)
    }

    private fun postStory(imageFile: File?, videoFile: File?) {
        val selectedTagIdList = selectedTagPeopleList.map { it.id }
        // Pass location position (x/y) to API so it can be stored and used in Story Viewer
        // Pass first user tag position and info to API
        Log.d("CreateStoryActivity", "Posting story with location - placeName: $selectedLocationLabel, lat: $selectedLocationLat, lng: $selectedLocationLng, x: $selectedLocationX, y: $selectedLocationY")
        Log.d("CreateStoryActivity", "Posting story with user tag - userId: $selectedUserTagId, name: $selectedUserTagName, x: $selectedUserTagX, y: $selectedUserTagY")
        individualViewModal.createStory(
            imageFile, 
            videoFile, 
            selectedTagIdList,
            selectedLocationLabel,
            selectedLocationLat,
            selectedLocationLng,
            selectedLocationX,  // Normalized x position (0.0-1.0)
            selectedLocationY,  // Normalized y position (0.0-1.0)
            selectedUserTagId,  // First tagged user ID
            selectedUserTagName,  // First tagged user name
            selectedUserTagX,  // Normalized x position (0.0-1.0)
            selectedUserTagY   // Normalized y position (0.0-1.0)
        )
    }

    private fun setTagsFlexList(selectedPeopleList: ArrayList<TagPeople>) {
        // For stories we only want the in-image tag overlay (like @username),
        // not the chip list UI below the preview, so always hide this layout.
        binding.userTagLayout.visibility = View.GONE

        // Still wire the adapter so existing logic that relies on it (like remove callbacks)
        // continues to work if needed in future.
        val tagAdapter = TagsAdapter(this, selectedPeopleList, ::onTagUpdated)
        binding.tagsRv.adapter = tagAdapter

        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
            flexWrap = FlexWrap.WRAP
        }
        binding.tagsRv.layoutManager = layoutManager
    }

    private fun onTagUpdated(updatedList: ArrayList<TagPeople>) {
        selectedTagPeopleList = updatedList
        // Keep the chip section hidden and just update the overlays on the story
        binding.userTagLayout.visibility = View.GONE
        updateStoryTagOverlays()
    }

    /**
     * Create non-editable, movable blue @username overlays directly on the story preview.
     * These behave like text stickers but can only be removed (via the X button), not edited.
     */
    private fun updateStoryTagOverlays() {
        // Remove any existing tag overlays first
        val parent = binding.photoEditorView
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val isTagOverlay = child.getTag(R.id.story_tag_overlay_view) as? Boolean ?: false
            if (isTagOverlay) {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { view ->
            try {
                parent.removeView(view)
            } catch (_: Exception) {
                // Ignore if removal fails; PhotoEditor might already manage this view.
            }
        }

        // Add a new overlay for each selected person
        if (selectedTagPeopleList.isEmpty()) return

        selectedTagPeopleList.forEachIndexed { index, tagPerson ->
            val displayName = "@${tagPerson.name ?: ""}".trim()
            if (displayName.length <= 1) return@forEachIndexed

            val textStyleBuilder = TextStyleBuilder().apply {
                withTextColor(ContextCompat.getColor(activity, R.color.blue))
                // Blue pill background for better visibility of the tag
                ContextCompat.getDrawable(activity, R.drawable.story_tag_background_blue)
                    ?.let { withBackgroundDrawable(it) }
                ResourcesCompat.getFont(activity, R.font.comic_regular)?.let { withTextFont(it) }
            }

            photoEditor.addText(displayName, textStyleBuilder)

            // Configure the newly added overlay
            binding.photoEditorView.postDelayed({
                val overlay = getLatestTextOverlay() ?: return@postDelayed

                overlay.setTag(R.id.story_tag_overlay_view, true)

                val textView = overlay.findViewById<TextView>(R.id.tvPhotoEditorText)
                val editIcon = overlay.findViewById<ImageView>(R.id.imgPhotoEditorEdit)

                textView?.apply {
                    text = displayName
                    setTextColor(ContextCompat.getColor(activity, R.color.blue))
                    background = ContextCompat.getDrawable(activity, R.drawable.story_tag_background_blue)
                    // Match location tag UI: 12sp text size, ellipsize, padding
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(
                        (12 * resources.displayMetrics.density).toInt(),
                        (8 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt(),
                        (8 * resources.displayMetrics.density).toInt()
                    )
                }
                
                // Track first user tag for position and API
                if (index == 0) {
                    overlay.setTag(R.id.story_user_tag_first, true)
                    userTagOverlayView = overlay
                    selectedUserTagId = tagPerson.id
                    selectedUserTagName = tagPerson.name
                    // Update position after layout
                    binding.photoEditorView.post {
                        updateUserTagPosition(overlay)
                    }
                }

                // Hide edit pencil so user cannot edit the mention text
                editIcon?.visibility = View.GONE
                editIcon?.setOnClickListener(null)

                // Allow the user to freely drag the tag around
                removeGravityConstraints(overlay)

                // Slightly stagger starting positions so multiple tags don't overlap perfectly
                val startY = (binding.photoEditorView.height * 0.25f) +
                        (index * 60 * resources.displayMetrics.density)
                overlay.x = binding.photoEditorView.width * 0.25f
                overlay.y = startY.coerceAtMost(binding.photoEditorView.height * 0.75f)

                // Record initial position for position monitoring logic
                viewPositions[overlay] = Pair(overlay.x, overlay.y)
            }, 80)
        }
    }

    /**
     * Create/update a non-editable, movable blue location overlay directly on the story preview.
     * Example: "📍 Hyderabad, Telangana"
     */
    private fun updateStoryLocationOverlay(locationLabel: String) {
        val parent = binding.photoEditorView
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val isLocationOverlay = child.getTag(R.id.story_location_overlay_view) as? Boolean ?: false
            if (isLocationOverlay) {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { view ->
            try {
                parent.removeView(view)
            } catch (_: Exception) {
                // ignore
            }
        }

        val displayText = locationLabel.trim()
        if (displayText.isBlank()) return

        val textStyleBuilder = TextStyleBuilder().apply {
            withTextColor(ContextCompat.getColor(activity, R.color.blue))
            ContextCompat.getDrawable(activity, R.drawable.story_tag_background_blue)
                ?.let { withBackgroundDrawable(it) }
            ResourcesCompat.getFont(activity, R.font.comic_regular)?.let { withTextFont(it) }
        }
        photoEditor.addText(displayText, textStyleBuilder)

        binding.photoEditorView.postDelayed({
            val overlay = getLatestTextOverlay() ?: return@postDelayed
            overlay.setTag(R.id.story_location_overlay_view, true)
            
            // Store reference to location overlay
            locationOverlayView = overlay

            val textView = overlay.findViewById<TextView>(R.id.tvPhotoEditorText)
            val editIcon = overlay.findViewById<ImageView>(R.id.imgPhotoEditorEdit)

            textView?.apply {
                text = displayText
                setTextColor(ContextCompat.getColor(activity, R.color.blue))
                background = ContextCompat.getDrawable(activity, R.drawable.story_tag_background_blue)
                // Match viewing UI: 12sp text size
                textSize = 12f
                // Ensure text truncates with ellipsis if too long
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                // Match viewing UI padding: 12dp horizontal, 8dp vertical
                setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )
                applyLocationIcon(this)
            }

            editIcon?.visibility = View.GONE
            editIcon?.setOnClickListener(null)

            removeGravityConstraints(overlay)

            // Default position: near top-left-ish
            overlay.x = binding.photoEditorView.width * 0.20f
            overlay.y = binding.photoEditorView.height * 0.20f
            viewPositions[overlay] = Pair(overlay.x, overlay.y)
            
            // Store normalized position (0.0-1.0) for later use - use post to ensure dimensions are available
            binding.photoEditorView.post {
                updateLocationPosition(overlay)
            }
        }, 80)
    }

    private fun applyLocationIcon(textView: TextView) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_blue) ?: return
        // Match viewing UI: 16dp icon with 6dp margin
        val sizePx = (16 * resources.displayMetrics.density).toInt().coerceAtLeast(1)

        val wrapped = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.blue))
        wrapped.setBounds(0, 0, sizePx, sizePx)

        textView.setCompoundDrawablesRelative(wrapped, null, null, null)
        textView.compoundDrawablePadding = (6 * resources.displayMetrics.density).toInt()
    }

    private fun clearCompoundDrawables(textView: TextView) {
        textView.setCompoundDrawablesRelative(null, null, null, null)
        textView.compoundDrawablePadding = 0
    }
    
    /**
     * Update the stored location tag position (normalized 0.0-1.0)
     */
    private fun updateLocationPosition(overlay: View) {
        val viewWidth = binding.photoEditorView.width.toFloat()
        val viewHeight = binding.photoEditorView.height.toFloat()
        if (viewWidth > 0 && viewHeight > 0) {
            selectedLocationX = overlay.x / viewWidth
            selectedLocationY = overlay.y / viewHeight
            Log.d("CreateStoryActivity", "Updated location position - x: $selectedLocationX, y: $selectedLocationY (overlay.x: ${overlay.x}, overlay.y: ${overlay.y}, width: $viewWidth, height: $viewHeight)")
        } else {
            Log.w("CreateStoryActivity", "Cannot update location position - view dimensions are invalid (width: $viewWidth, height: $viewHeight)")
        }
    }
    
    /**
     * Update the stored user tag position (normalized 0.0-1.0) for the first tagged user
     */
    private fun updateUserTagPosition(overlay: View) {
        val viewWidth = binding.photoEditorView.width.toFloat()
        val viewHeight = binding.photoEditorView.height.toFloat()
        if (viewWidth > 0 && viewHeight > 0) {
            selectedUserTagX = overlay.x / viewWidth
            selectedUserTagY = overlay.y / viewHeight
            Log.d("CreateStoryActivity", "Updated user tag position - x: $selectedUserTagX, y: $selectedUserTagY (overlay.x: ${overlay.x}, overlay.y: ${overlay.y}, width: $viewWidth, height: $viewHeight)")
        } else {
            Log.w("CreateStoryActivity", "Cannot update user tag position - view dimensions are invalid (width: $viewWidth, height: $viewHeight)")
        }
    }
    
    /**
     * Compress image to reduce file size while maintaining quality.
     * Returns the original file if compression fails or is not needed.
     * Target size: Under 3MB to prevent "Request Entity Too Large" errors.
     */
    private fun compressImageIfNeeded(originalFile: File): File {
        try {
            val originalSize = originalFile.length()
            // Target max size: 3MB (more aggressive to prevent server errors)
            val targetSizeBytes = 3 * 1024 * 1024L // 3MB
            // Compress if over 2MB (be more proactive)
            val compressionThreshold = 2 * 1024 * 1024L // 2MB
            
            if (originalSize <= compressionThreshold) {
                Log.d("CreateStoryActivity", "Image size is acceptable (${originalSize} bytes), skipping compression")
                return originalFile
            }
            
            Log.d("CreateStoryActivity", "Starting compression - original size: ${originalSize / (1024 * 1024)} MB")
            
            // Load bitmap from file with options to reduce memory usage
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = if (originalSize > 5 * 1024 * 1024L) 2 else 1 // Downsample if very large
            }
            
            val bitmap = android.graphics.BitmapFactory.decodeFile(originalFile.absolutePath, options)
                ?: return originalFile
            
            // Calculate scale factor to reduce dimensions if too large (max 1920px on longest side for better compression)
            val maxDimension = 1920 // Reduced from 2048 for better file size
            val width = bitmap.width
            val height = bitmap.height
            var scaleFactor = 1f
            
            if (width > maxDimension || height > maxDimension) {
                scaleFactor = if (width > height) {
                    maxDimension.toFloat() / width
                } else {
                    maxDimension.toFloat() / height
                }
                Log.d("CreateStoryActivity", "Scaling image: ${width}x${height} -> scale factor: $scaleFactor")
            }
            
            // Scale bitmap if needed
            val scaledBitmap = if (scaleFactor < 1f) {
                val scaledWidth = (width * scaleFactor).toInt()
                val scaledHeight = (height * scaleFactor).toInt()
                android.graphics.Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true).also {
                    if (it != bitmap) {
                        bitmap.recycle() // Recycle original if we created a scaled version
                    }
                }
            } else {
                bitmap
            }
            
            // Create compressed file with unique name
            val compressedFile = File(originalFile.parent, "compressed_${System.currentTimeMillis()}_${originalFile.name}")
            
            // Start with moderate quality and decrease aggressively until target size is reached
            var quality = 80
            var outputStream: FileOutputStream? = null
            var compressed = false
            
            try {
                while (quality >= 40 && !compressed) {
                    outputStream?.close()
                    outputStream = FileOutputStream(compressedFile)
                    
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    outputStream = null
                    
                    val compressedSize = compressedFile.length()
                    Log.d("CreateStoryActivity", "Compression attempt - quality: $quality, size: ${compressedSize / 1024} KB")
                    
                    if (compressedSize <= targetSizeBytes) {
                        compressed = true
                        Log.d("CreateStoryActivity", "Image compressed successfully: ${originalSize / 1024} KB -> ${compressedSize / 1024} KB (quality: $quality)")
                    } else {
                        // Reduce quality more aggressively
                        quality -= if (quality > 60) 10 else 5
                    }
                }
                
                // If still too large after quality reduction, try reducing dimensions further
                if (!compressed && scaledBitmap.width > 1280) {
                    Log.d("CreateStoryActivity", "File still too large, reducing dimensions further")
                    val furtherScaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                        scaledBitmap,
                        (scaledBitmap.width * 0.8f).toInt(),
                        (scaledBitmap.height * 0.8f).toInt(),
                        true
                    )
                    if (furtherScaledBitmap != scaledBitmap) {
                        scaledBitmap.recycle()
                    }
                    
                    outputStream = FileOutputStream(compressedFile)
                    furtherScaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    outputStream = null
                    
                    furtherScaledBitmap.recycle()
                }
                
                // Clean up scaled bitmap
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                
                val finalSize = compressedFile.length()
                Log.d("CreateStoryActivity", "Final compressed size: ${finalSize / 1024} KB (${finalSize / (1024 * 1024)} MB)")
                
                // Replace original file with compressed version
                if (originalFile.exists() && originalFile.delete()) {
                    if (compressedFile.renameTo(originalFile)) {
                        return originalFile
                    } else {
                        Log.w("CreateStoryActivity", "Failed to rename compressed file, using compressed file directly")
                        return compressedFile
                    }
                } else {
                    Log.w("CreateStoryActivity", "Failed to delete original file, using compressed file directly")
                    return compressedFile
                }
            } catch (e: Exception) {
                outputStream?.close()
                Log.e("CreateStoryActivity", "Error during compression: ${e.message}", e)
                // Clean up
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                } else {
                    bitmap.recycle()
                }
                compressedFile.delete()
                return originalFile
            }
        } catch (e: Exception) {
            Log.e("CreateStoryActivity", "Error compressing image: ${e.message}", e)
            return originalFile
        }
    }
    
    /**
     * Remove location overlay from photoEditor before saving to prevent it from being merged into bitmap
     */
    private fun removeLocationOverlayFromEditor() {
        locationOverlayView?.let { overlay ->
            try {
                // Remove the view from photoEditorView to prevent it from being saved in the bitmap
                val parent = overlay.parent as? ViewGroup
                parent?.removeView(overlay)
                locationOverlayView = null
                Log.d("CreateStoryActivity", "Location overlay removed from photoEditor before saving")
            } catch (e: Exception) {
                Log.e("CreateStoryActivity", "Error removing location overlay: ${e.message}", e)
            }
        }
    }
    
    /**
     * Remove user tag overlays from PhotoEditor before saving the image.
     * This prevents user tags from being burned into the bitmap.
     */
    private fun removeUserTagOverlaysFromEditor() {
        val parent = binding.photoEditorView
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val isTagOverlay = child?.getTag(R.id.story_tag_overlay_view) as? Boolean ?: false
            if (isTagOverlay) {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { view ->
            try {
                // Remove the view from photoEditorView to prevent it from being saved in the bitmap
                val viewParent = view.parent as? ViewGroup
                viewParent?.removeView(view)
                if (view == userTagOverlayView) {
                    userTagOverlayView = null
                }
                Log.d("CreateStoryActivity", "User tag overlay removed from photoEditor before saving")
            } catch (e: Exception) {
                Log.e("CreateStoryActivity", "Error removing user tag overlay: ${e.message}", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoPreview.visibility == View.VISIBLE) {
            videoPlayer?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.videoPreview.visibility == View.VISIBLE && currentVideoUri != null) {
            videoPlayer?.playWhenReady = true
        }
    }

    override fun onDestroy() {
        // Stop position monitoring
        positionMonitoringRunnable?.let { positionMonitoringHandler?.removeCallbacks(it) }
        positionMonitoringHandler = null
        positionMonitoringRunnable = null
        
        videoPlayer?.release()
        videoPlayer = null
        super.onDestroy()
    }
}
