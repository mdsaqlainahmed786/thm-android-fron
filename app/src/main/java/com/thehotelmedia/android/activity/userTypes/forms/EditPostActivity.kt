package com.thehotelmedia.android.activity.userTypes.forms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.forms.createPost.FeelingActivity
import com.thehotelmedia.android.activity.userTypes.forms.CheckInActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.AttachedMediaAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityEditPostBinding
import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.thehotelmedia.android.ViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class EditPostActivity : BaseActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private lateinit var attachedMediaAdapter: AttachedMediaAdapter
    private val mediaList = mutableListOf<String>() // Stores URIs as strings
    private val mediaIdMap = mutableMapOf<Int, String?>() // Maps position to original MediaRef ID (null if new)
    private val deletedMediaIds = mutableSetOf<String>() // IDs of media that were removed
    private var selectedFeeling: String = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var preferenceManager: PreferenceManager
    
    private var postId: String = ""
    private var initialContent: String = ""
    private var initialFeeling: String? = null
    private var initialMedia: List<MediaRef> = emptyList()
    private var editingImagePosition: Int = -1 // Track which image is being edited
    private var isDataLoaded: Boolean = false // Flag to prevent duplicate loading
    
    // Location/Check-in fields
    private var selectedPlaceName: String = ""
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private var initialPlaceName: String? = null
    private var initialLat: Double? = null
    private var initialLng: Double? = null

    private val feelingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            val feeling = bundle?.getSerializable("selectedFeeling") as? String ?: ""
            updateFeeling(feeling)
        }
    }

    private val checkInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        progressBar.hide()
        if (result.resultCode == Activity.RESULT_OK) {
            // Get the data from the intent
            val placeAddress = result.data?.getStringExtra("PLACE_ADDRESS") ?: ""
            val placeName = result.data?.getStringExtra("PLACE_NAME") ?: ""
            val placeID = result.data?.getStringExtra("PLACE_ID") ?: ""
            val street = result.data?.getStringExtra("STREET") ?: ""
            val city = result.data?.getStringExtra("CITY") ?: ""
            val state = result.data?.getStringExtra("STATE") ?: ""
            val zipcode = result.data?.getStringExtra("ZIPCODE") ?: ""
            val country = result.data?.getStringExtra("COUNTRY") ?: ""

            val lat = result.data?.getDoubleExtra("LAT", 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra("LNG", 0.0) ?: 0.0
            val coverImage = result.data?.getStringExtra("COVER_IMAGE") ?: ""
            val profileImage = result.data?.getStringExtra("PROFILE_PIC") ?: ""
            
            if (placeAddress.isNotBlank()) {
                binding.hotelDetailsCv.visibility = View.VISIBLE
                binding.hotelNameTv.text = placeName
                binding.hotelAddressTv.text = placeAddress

                Glide.with(this).load(profileImage).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
                Glide.with(this).load(coverImage).placeholder(R.drawable.ic_image_placeholder_image).error(R.drawable.ic_image_placeholder_image).into(binding.imageView)

                selectedPlaceName = "$placeName, $state"
                selectedLat = lat
                selectedLng = lng
            } else {
                binding.hotelDetailsCv.visibility = View.GONE
            }
        }
    }

    private val pickMultipleMediaLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true) {
                // For images, launch crop activity
                startCropActivity(uri)
            } else if (mimeType?.startsWith("video/") == true) {
                // For videos, add directly - new file, no original ID
                mediaList.add(uri.toString())
                mediaIdMap[mediaList.size - 1] = null
                updateMediaAdapter()
            }
        }
    }

    private val editImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedImageUri = result.data?.getStringExtra("edited_image_uri")
            editedImageUri?.let { newUri ->
                // Replace the old image with the edited one
                if (editingImagePosition >= 0 && editingImagePosition < mediaList.size) {
                    // Replace at the original position
                    mediaList[editingImagePosition] = newUri
                    mediaIdMap[editingImagePosition] = null // New file, no original ID
                    
                    // Force adapter refresh to show the new image
                    if (::attachedMediaAdapter.isInitialized) {
                        // Create a new list instance to force change detection
                        val updatedList = ArrayList(mediaList)
                        attachedMediaAdapter.updateMediaList(updatedList)
                        // Force refresh of the specific item to show the new image
                        attachedMediaAdapter.notifyItemChanged(editingImagePosition)
                    } else {
                        updateMediaAdapter()
                    }
                } else {
                    // Position invalid or not tracked, add at the end
                    mediaList.add(newUri)
                    mediaIdMap[mediaList.size - 1] = null // New file, no original ID
                    updateMediaAdapter()
                }
                editingImagePosition = -1
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            // If user cancelled, restore the editingImagePosition
            editingImagePosition = -1
        }
    }

    // UCrop uses onActivityResult, not ActivityResultContracts
    private var pendingCropUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        progressBar = CustomProgressBar(this)
        preferenceManager = PreferenceManager.getInstance(this)
        
        // Get post data from intent
        postId = intent.getStringExtra("POST_ID") ?: ""
        initialContent = intent.getStringExtra("CONTENT") ?: ""
        initialFeeling = intent.getStringExtra("FEELING")
        initialMedia = intent.getParcelableArrayListExtra<MediaRef>("MEDIA") ?: emptyList()
        
        // Get location data from intent
        initialPlaceName = intent.getStringExtra("PLACE_NAME")
        initialLat = intent.getDoubleExtra("LAT", Double.NaN).takeIf { !it.isNaN() }
        initialLng = intent.getDoubleExtra("LNG", Double.NaN).takeIf { !it.isNaN() }
        
        initUI()
        loadInitialData()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]

        // Setup toolbar
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.doneBtn.setOnClickListener {
            savePost()
        }

        // Setup check-in click
        binding.checkInLayout.setOnClickListener {
            progressBar.show()
            val intent = Intent(this, CheckInActivity::class.java)
            checkInLauncher.launch(intent)
        }

        // Setup feeling click
        binding.feelingLayout.setOnClickListener {
            val intent = Intent(this, FeelingActivity::class.java)
            intent.putExtra("selectedFeeling", selectedFeeling)
            feelingLauncher.launch(intent)
        }


        // Setup add media click
        binding.addMediaLayout.setOnClickListener {
            pickMultipleMediaLauncher.launch("*/*")
        }

        // Setup media grid
        binding.mediaRv.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadInitialData() {
        // Prevent duplicate loading
        if (isDataLoaded) {
            return
        }
        
        // Set content
        binding.contentEt.setText(initialContent)

        // Set feeling
        updateFeeling(initialFeeling ?: "")

        // Set location/check-in data
        if (initialPlaceName != null && initialLat != null && initialLng != null) {
            selectedPlaceName = initialPlaceName ?: ""
            selectedLat = initialLat ?: 0.0
            selectedLng = initialLng ?: 0.0
            binding.checkInTv.text = selectedPlaceName
            // Note: We don't have hotel details in the initial data, so hotelDetailsCv stays hidden
            // User can update check-in if needed
        }

        // Clear all media tracking data before loading
        mediaList.clear()
        mediaIdMap.clear()
        deletedMediaIds.clear()
        editingImagePosition = -1
        
        // Load media from backend and track original IDs
        // Use a set to track URLs to prevent duplicates
        val seenUrls = mutableSetOf<String>()
        initialMedia.forEachIndexed { index, mediaRef ->
            mediaRef.sourceUrl?.let { url ->
                // Only add if not already seen (prevent duplicates)
                if (!seenUrls.contains(url)) {
                    seenUrls.add(url)
                    mediaList.add(url)
                    mediaIdMap[mediaList.size - 1] = mediaRef.Id // Store original ID for tracking
                }
            }
        }
        
        isDataLoaded = true
        updateMediaAdapter()
    }

    private fun updateFeeling(feeling: String) {
        selectedFeeling = feeling
        if (feeling.isNotEmpty()) {
            binding.feelingTv.text = feeling
            binding.feelingTv.visibility = View.VISIBLE
            binding.feelingTv.setTextColor(ContextCompat.getColor(this, R.color.blue))
        } else {
            binding.feelingTv.visibility = View.GONE
        }
    }

    private fun updateMediaAdapter() {
        if (mediaList.isEmpty()) {
            binding.mediaRv.visibility = View.GONE
        } else {
            binding.mediaRv.visibility = View.VISIBLE
            
            // Initialize adapter only once, reuse if already exists
            if (!::attachedMediaAdapter.isInitialized) {
                attachedMediaAdapter = AttachedMediaAdapter(
                    this,
                    mediaList,
                    { updatedList ->
                        handleMediaListUpdate(updatedList)
                    },
                    { mediaUri, position ->
                        handleEditImageClick(mediaUri, position)
                    }
                )
                binding.mediaRv.adapter = attachedMediaAdapter
            } else {
                // Update existing adapter's list - create a new list to force refresh
                val newList = ArrayList(mediaList)
                attachedMediaAdapter.updateMediaList(newList)
            }
        }
    }
    
    private fun handleMediaListUpdate(updatedList: MutableList<String>) {
        // Track which items were removed by comparing with current mediaIdMap
        val currentUrls = mediaList.toSet()
        val newUrls = updatedList.toSet()
        val removedUrls = currentUrls - newUrls
        
        // Find removed items and add their IDs to deletedMediaIds
        mediaIdMap.forEach { (position, id) ->
            if (position < mediaList.size && id != null) {
                val url = mediaList[position]
                if (url in removedUrls) {
                    deletedMediaIds.add(id)
                }
            }
        }
        
        // Rebuild mediaIdMap based on new list
        val newMediaIdMap = mutableMapOf<Int, String?>()
        updatedList.forEachIndexed { newIndex, url ->
            // Try to find original position of this URL in the old list
            val originalIndex = mediaList.indexOf(url)
            if (originalIndex >= 0 && originalIndex < mediaIdMap.size) {
                newMediaIdMap[newIndex] = mediaIdMap[originalIndex]
            } else {
                newMediaIdMap[newIndex] = null // New item
            }
        }
        
        // Update lists
        mediaList.clear()
        mediaList.addAll(updatedList)
        mediaIdMap.clear()
        mediaIdMap.putAll(newMediaIdMap)
        
        // Update adapter with new list
        if (::attachedMediaAdapter.isInitialized) {
            attachedMediaAdapter.updateMediaList(mediaList)
        }
    }
    
    private fun handleEditImageClick(mediaUri: String, position: Int) {
        // Handle edit click - open crop for existing image
        // Check if it's an image (not a video)
        val isVideo = mediaUri.contains(".mp4") || mediaUri.contains("video") || 
                     mediaUri.contains(".mov") || mediaUri.contains(".mkv")
        
        if (!isVideo) {
            // Store the position and original ID for tracking replacement
            editingImagePosition = position
            val originalId = mediaIdMap[position]
            
            // If this was an existing media item, mark it for deletion
            if (originalId != null) {
                deletedMediaIds.add(originalId)
            }
            
            // For remote URLs, download first
            if (mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) {
                // Download image from URL
                downloadAndCropImage(mediaUri, position)
            } else {
                // Local URI - use directly
                val uri = Uri.parse(mediaUri)
                // Don't remove the old one yet - we'll replace it after editing
                // This ensures the position is still valid when we get the edited image back
                startCropActivity(uri)
            }
        }
    }

    private fun downloadAndCropImage(imageUrl: String, position: Int) {
        progressBar.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val tempFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                
                withContext(Dispatchers.Main) {
                    progressBar.hide()
                    // Store the position and original ID for tracking replacement
                    editingImagePosition = position
                    val originalId = mediaIdMap[position]
                    
                    // If this was an existing media item, mark it for deletion
                    if (originalId != null) {
                        deletedMediaIds.add(originalId)
                    }
                    
                    // Don't remove the old one yet - we'll replace it after editing
                    // This ensures the position is still valid when we get the edited image back
                    startCropActivity(Uri.fromFile(tempFile))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.hide()
                    editingImagePosition = -1
                    CustomSnackBar.showSnackBar(binding.root, "Failed to download image. Please remove and re-add it.")
                }
            }
        }
    }

    private fun startCropActivity(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))
        
        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(this@EditPostActivity, R.color.black))
            setStatusBarColor(ContextCompat.getColor(this@EditPostActivity, R.color.black))
            setToolbarWidgetColor(ContextCompat.getColor(this@EditPostActivity, R.color.white))
            setToolbarTitle("Edit Image")
            
            setAspectRatioOptions(0,
                AspectRatio("Original", 0f, 0f),
                AspectRatio("Square", 1f, 1f),
                AspectRatio("Portrait", 3f, 4f),
                AspectRatio("Landscape", 4f, 3f)
            )
        }

        pendingCropUri = uri
        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .withMaxResultSize(2000, 2000)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            val resultUri = UCrop.getOutput(data)
            resultUri?.let {
                // Send to EditImageActivity for further editing (filters, text, emoji)
                // Pass as Parcelable URI and also as string, and mark as already cropped
                val editIntent = Intent(this, EditImageActivity::class.java).apply {
                    putExtra("image_uri", it) // Pass as Parcelable
                    putExtra("image_uri_string", it.toString()) // Also pass as string for backup
                    putExtra("already_cropped", true) // Flag to indicate it's already cropped
                }
                editImageLauncher.launch(editIntent)
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val cropError = UCrop.getError(data)
            CustomSnackBar.showSnackBar(binding.root, "Error cropping image: ${cropError?.message}")
        }
    }

    private fun savePost() {
        val content = binding.contentEt.text?.toString()?.trim().orEmpty()
        val feeling = if (selectedFeeling.isNotEmpty()) selectedFeeling else ""
        
        if (postId.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root, "Post ID is missing")
            return
        }

        progressBar.show()
        
        // Convert media URIs to files and prepare for upload
        // Only include new/edited media (not existing URLs)
        val newMediaFiles = mutableListOf<String>()
        mediaList.forEach { mediaUri ->
            // If it's a local file (edited/cropped), include it
            // If it's a remote URL, it's an existing media item, skip it
            if (!mediaUri.startsWith("http://") && !mediaUri.startsWith("https://")) {
                newMediaFiles.add(mediaUri)
            }
        }
        
        // Remove any existing observer to prevent multiple calls
        individualViewModal.updatePostResult.removeObservers(this)
        
        // Observe result
        individualViewModal.updatePostResult.observe(this) { result ->
            progressBar.hide()
            if (result != null) {
                if (result.status == true) {
                    CustomSnackBar.showSnackBar(binding.root, result.message ?: "Post updated successfully")
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("POST_UPDATED", true)
                        putExtra("POST_ID", postId)
                    })
                    finish()
                } else {
                    CustomSnackBar.showSnackBar(binding.root, result.message ?: "Failed to update post")
                }
            } else {
                CustomSnackBar.showSnackBar(binding.root, "Failed to update post")
            }
            // Remove observer after handling
            individualViewModal.updatePostResult.removeObservers(this)
        }
        
        individualViewModal.updatePost(postId, content, feeling, newMediaFiles, deletedMediaIds.toList(), selectedPlaceName, selectedLat, selectedLng)
    }

    companion object {
        fun start(context: android.content.Context, postId: String, content: String, feeling: String?, media: List<MediaRef>, placeName: String? = null, lat: Double? = null, lng: Double? = null) {
            val intent = Intent(context, EditPostActivity::class.java).apply {
                putExtra("POST_ID", postId)
                putExtra("CONTENT", content)
                putExtra("FEELING", feeling)
                putParcelableArrayListExtra("MEDIA", ArrayList(media))
                placeName?.let { putExtra("PLACE_NAME", it) }
                lat?.let { putExtra("LAT", it) }
                lng?.let { putExtra("LNG", it) }
            }
            context.startActivity(intent)
        }
    }
}

