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
    private val mediaList = mutableListOf<String>()
    private var selectedFeeling: String = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var preferenceManager: PreferenceManager
    
    private var postId: String = ""
    private var initialContent: String = ""
    private var initialFeeling: String? = null
    private var initialMedia: List<MediaRef> = emptyList()

    private val feelingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            val feeling = bundle?.getSerializable("selectedFeeling") as? String ?: ""
            updateFeeling(feeling)
        }
    }

    private val pickMultipleMediaLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true) {
                // For images, launch crop activity
                startCropActivity(uri)
            } else if (mimeType?.startsWith("video/") == true) {
                // For videos, add directly
                mediaList.add(uri.toString())
                updateMediaAdapter()
            }
        }
    }

    private val editImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedImageUri = result.data?.getStringExtra("edited_image_uri")
            editedImageUri?.let {
                mediaList.add(it)
                updateMediaAdapter()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            // If user cancelled image editing, we can still add the cropped image directly
            pendingCropUri?.let { uri ->
                // Get the last cropped result from UCrop
                // For now, we'll skip if cancelled
            }
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
        // Set content
        binding.contentEt.setText(initialContent)

        // Set feeling
        updateFeeling(initialFeeling ?: "")

        // Set media
        mediaList.clear()
        initialMedia.forEach { mediaRef ->
            mediaRef.sourceUrl?.let { mediaList.add(it) }
        }
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
            
            attachedMediaAdapter = AttachedMediaAdapter(
                this,
                mediaList,
                { updatedList ->
                    mediaList.clear()
                    mediaList.addAll(updatedList)
                    updateMediaAdapter()
                },
                { mediaUri, position ->
                    // Handle edit click - open crop for existing image
                    // Check if it's an image (not a video)
                    val isVideo = mediaUri.contains(".mp4") || mediaUri.contains("video") || 
                                 mediaUri.contains(".mov") || mediaUri.contains(".mkv")
                    
                    if (!isVideo) {
                        // For remote URLs, download first
                        if (mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) {
                            // Download image from URL
                            downloadAndCropImage(mediaUri, position)
                        } else {
                            // Local URI - use directly
                            val uri = Uri.parse(mediaUri)
                            // Remove the old one - it will be replaced after cropping
                            mediaList.removeAt(position)
                            updateMediaAdapter()
                            startCropActivity(uri)
                        }
                    }
                }
            )
            binding.mediaRv.adapter = attachedMediaAdapter
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
                    // Remove the old one - it will be replaced after cropping
                    mediaList.removeAt(position)
                    updateMediaAdapter()
                    startCropActivity(Uri.fromFile(tempFile))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.hide()
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
                val editIntent = Intent(this, EditImageActivity::class.java).apply {
                    putExtra("image_uri", it.toString())
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
        val feeling = if (selectedFeeling.isNotEmpty()) selectedFeeling else null
        
        if (postId.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root, "Post ID is missing")
            return
        }

        progressBar.show()
        
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
        
        individualViewModal.updatePost(postId, content, feeling, mediaList)
    }

    companion object {
        fun start(context: android.content.Context, postId: String, content: String, feeling: String?, media: List<MediaRef>) {
            val intent = Intent(context, EditPostActivity::class.java).apply {
                putExtra("POST_ID", postId)
                putExtra("CONTENT", content)
                putExtra("FEELING", feeling)
                putParcelableArrayListExtra("MEDIA", ArrayList(media))
            }
            context.startActivity(intent)
        }
    }
}

