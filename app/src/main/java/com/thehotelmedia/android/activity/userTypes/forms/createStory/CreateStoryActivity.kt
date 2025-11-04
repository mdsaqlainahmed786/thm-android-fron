package com.thehotelmedia.android.activity.userTypes.forms.createStory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.individual.IndividualMediaActivity.Companion.CAMERA_PERMISSION_CODE
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity.Companion.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity.Companion.PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
import com.thehotelmedia.android.activity.userTypes.forms.VideoTrimmerActivity
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
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CreateStoryActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateStoryBinding
    private val activity = this@CreateStoryActivity
    private var croppedImageUri: Uri? = null
    private var selectedBackgroundResId: Int = android.R.color.transparent
    private lateinit var photoEditor: PhotoEditor
    private var businessesType = ""
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var progressBar : CustomProgressBar
    private lateinit var giffProgressBar : GiffProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var individualViewModal: IndividualViewModal


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

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val savedUri = saveImageToStorage(imageBitmap)
                savedUri?.let { uri ->
                    startCropActivity(uri)
                }
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
        binding.filterLayout.visibility = View.GONE

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

        // Check and request necessary permissions
        checkPermissions()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.doneButton.setOnClickListener {
            saveEditedImage()
        }

        binding.filterButton.setOnClickListener {
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
            val textEntryDialog = CustomTextEntryDialog(this) { text, color, backgroundResId ->
                selectedBackgroundResId = backgroundResId
                addTextWithBackground(text, color)
            }
            textEntryDialog.show()
        }


        individualViewModal.createStoryResult.observe(activity){result->
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
            videoClickListener = { pickCameraPhotos() },
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
            checkVideoDurationAndTrim(uri)
        }
    }
    private fun checkVideoDurationAndTrim(uri: Uri) {
        val projection = arrayOf(MediaStore.Video.Media.DURATION)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
//                val durationMillis = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
//                val durationSeconds = durationMillis / 1000

                val savedVideoUri = saveVideo(uri)

                // Launch the video trimmer with the saved video URI
                if (savedVideoUri != null) {
                    showVideoTrimmer(savedVideoUri)
                }
            }
        } ?: run {
            Log.e("CreatePostActivity", "Failed to retrieve video duration")
        }
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
    private fun showVideoTrimmer(uri: Uri) {
        val intent = Intent(this, VideoTrimmerActivity::class.java)
        intent.putExtra("video_uri", uri.toString())
        intent.putExtra("FROM", "CreateStory")
        startActivity(intent)
    }

    private fun pickCameraPhotos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            captureImageFromCamera()
        }
    }
    private fun captureImageFromCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
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

    // Add emoji to the image
    private fun addEmoji(emoji: String) {
        photoEditor.addEmoji(emoji)
    }
    private fun addTextWithBackground(text: String, color: Int) {
        val textWithBackgroundBitmap = createTextBitmap(text, color, selectedBackgroundResId)
        // Add the bitmap to the image using PhotoEditor
        photoEditor.addImage(textWithBackgroundBitmap)
//        Toast.makeText(this, "Text with background added", Toast.LENGTH_SHORT).show()
    }

    private fun createTextBitmap(text: String, color: Int, backgroundResId: Int): Bitmap {
        val view = layoutInflater.inflate(R.layout.text_with_background, null)
        val textView = view.findViewById<TextView>(R.id.text_view)

        // Set the text color and background
        textView.text = text
        textView.setTextColor(color)
        textView.setBackgroundResource(backgroundResId)

        // Set the max width programmatically to ensure text wrapping
        val maxWidth = (300 * resources.displayMetrics.density).toInt() // 300dp to pixels
        textView.maxWidth = maxWidth

        // Measure and layout the view
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Create bitmap and draw the view onto it
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun saveEditedImage() {
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
            val newDir = File(filesDir, "edited_images").apply {
                if (!exists()) mkdirs()
            }

            val file = File(newDir, "edited_image_${System.currentTimeMillis()}.jpg")

            try {
                photoEditor.saveAsFile(file.absolutePath, object : PhotoEditor.OnSaveListener {
                    override fun onSuccess(imagePath: String) {
                        val imageFile = File(imagePath)

                        // Get URI using FileProvider (Recommended for Android 7+)
                        val editedImageUri = FileProvider.getUriForFile(
                            this@CreateStoryActivity,
                            "${packageName}.provider",
                            imageFile
                        )

                        postStory(imageFile) // Upload or share the image

                        // Return URI as result
                        val resultIntent = Intent().apply {
                            putExtra("edited_image_uri", editedImageUri.toString())
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                    }

                    override fun onFailure(exception: Exception) {
                        Toast.makeText(this@CreateStoryActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
                        Log.e("EditImageActivity", "Error: ${exception.message}", exception)
                    }
                })
            } catch (e: SecurityException) {
                Log.e("EditImageActivity", "Permission issue: ${e.message}", e)
                Toast.makeText(this, "Storage permission is required to save the image", Toast.LENGTH_SHORT).show()
            }
        }, 300)
    }

    private fun postStory(imageFile: File) {
        individualViewModal.createStory(imageFile,null)
    }


    private fun saveImageToStorage(bitmap: Bitmap): Uri? {
        val folder = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(folder, "edited_image_${System.currentTimeMillis()}.jpg")
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            Uri.fromFile(file)
        } catch (e: IOException) {
            Log.e("SaveImage", "Error saving image: ${e.message}")
            null
        }
    }


}
