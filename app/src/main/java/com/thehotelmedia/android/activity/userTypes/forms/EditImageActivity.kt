package com.thehotelmedia.android.activity.userTypes.forms

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
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.imageEditor.FilterAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.imageEditor.CustomEmojiEntryDialog
import com.thehotelmedia.android.customClasses.imageEditor.CustomTextEntryDialog
import com.thehotelmedia.android.databinding.ActivityEditImageBinding
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoFilter
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import java.io.File


class EditImageActivity : BaseActivity() {

    private lateinit var binding: ActivityEditImageBinding
    private lateinit var photoEditor: PhotoEditor
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri? = null
    private val activity = this@EditImageActivity
    private var selectedBackgroundResId: Int = android.R.color.transparent
    private lateinit var progressBar: CustomProgressBar

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 101
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = CustomProgressBar(this)

        // Initialize PhotoEditor
        photoEditor = PhotoEditor.Builder(this, binding.photoEditorView)
            .setPinchTextScalable(true)
            .build()

        // Get image URI from intent - try both Parcelable and String
        var tempUri: Uri? = intent.getParcelableExtra("image_uri")
        if (tempUri == null) {
            val uriString = intent.getStringExtra("image_uri_string")
            if (uriString != null) {
                tempUri = Uri.parse(uriString)
            }
        }
        
        // Store in class property
        imageUri = tempUri
        
        // Check if image is already cropped (coming from EditPostActivity)
        val alreadyCropped = intent.getBooleanExtra("already_cropped", false)
        
        // Use local immutable variable to allow smart cast
        val uriToUse = tempUri
        if (uriToUse != null) {
            if (alreadyCropped) {
                // Image is already cropped, load it directly
                croppedImageUri = uriToUse
                loadImage(uriToUse) // loadImage will enable buttons after image loads
            } else {
                // Start crop immediately after image selection
                startCropActivity(uriToUse)
                // Disable editing buttons until crop is completed
                enableEditingButtons(false)
            }
        } else {
            // No image URI provided, finish activity
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up click listeners for editing options
        setupEditButtons()


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
        val filterAdapter = FilterAdapter(activity, photoEditor, filterList,imageUri)
        binding.filterRecyclerView.adapter = filterAdapter


    }

    // Disable or enable editing buttons based on crop completion
    private fun enableEditingButtons(enable: Boolean) {
        binding.cropButton.isEnabled = enable
        binding.filterButton.isEnabled = enable
        binding.emojiButton.isEnabled = enable
        binding.addTextButton.isEnabled = enable
        binding.doneButton.isEnabled = enable
    }


private fun startCropActivity(uri: Uri) {
    val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))

    val options = UCrop.Options().apply {
        // Set toolbar and status bar colors (optional)
        setToolbarColor(ContextCompat.getColor(activity, R.color.black))
        setStatusBarColor(ContextCompat.getColor(activity, R.color.black))
        // Set toolbar title and widget colors
        setToolbarWidgetColor(ContextCompat.getColor(activity, R.color.white))
//        setActiveControlsWidgetColor(ContextCompat.getColor(activity, R.color.colorAccent))

        setToolbarTitle("Edit Image")

        // Add aspect ratio options for the user to choose from
        setAspectRatioOptions(0,
            AspectRatio("Original", 0f, 0f),
            AspectRatio("Square", 1f, 1f),  // 1:1 aspect ratio
            AspectRatio("Portrait", 3f, 4f),// 3:4 aspect ratio
            AspectRatio("Landscape", 4f, 3f)// 4:3 aspect ratio
        )
    }

    // Start UCrop with the destination URI and options
    UCrop.of(uri, destinationUri)
        .withOptions(options)
        .withMaxResultSize(1000, 1000)  // Set the max size if necessary
        .start(this)
}


    // Load the cropped image into PhotoEditor
    private fun loadImage(uri: Uri) {
        Log.d("EditImageActivity", "Loading image from URI: $uri")
        
        // Verify the URI is valid and file exists (for file:// URIs)
        if (uri.scheme == "file" || uri.scheme == null) {
            val path = uri.path ?: uri.toString().replace("file://", "")
            val file = File(path)
            if (!file.exists()) {
                Log.e("EditImageActivity", "File does not exist: ${file.absolutePath}")
                Toast.makeText(this, "Image file not found: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            Log.d("EditImageActivity", "File exists: ${file.absolutePath}, size: ${file.length()} bytes")
        }
        
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .apply(RequestOptions().error(R.drawable.ic_image_placeholder_image))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d("EditImageActivity", "Image loaded successfully, size: ${resource.width}x${resource.height}")
                    // Scale the image to fit the PhotoEditorView without stretching it
                    binding.photoEditorView.source.setImageBitmap(resource)
                    // Adjust the ImageView's scaleType if necessary
                    binding.photoEditorView.source.scaleType = ImageView.ScaleType.FIT_CENTER
                    // Enable editing buttons after image is loaded
                    enableEditingButtons(true)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    Log.d("EditImageActivity", "Image load cleared")
                }
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("EditImageActivity", "Failed to load image from URI: $uri")
                    Toast.makeText(this@EditImageActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }


    // Handle result from UCrop
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                croppedImageUri = it
                loadImage(it) // Load cropped image into the editor
                enableEditingButtons(true) // Enable other editing buttons after cropping
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

    // Set up click listeners for editing options
    private fun setupEditButtons() {
        binding.filterLayout.visibility = View.GONE
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.cropButton.setOnClickListener {
            imageUri?.let { startCropActivity(it) }
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
//            applyFilter(PhotoFilter.SEPIA)
        }
        binding.emojiButton.setOnClickListener {
            val emojiEntryDialog = CustomEmojiEntryDialog(this) { emoji ->
                addEmoji(emoji)
            }
            emojiEntryDialog.show()
//            addEmoji("😀")
        }
        binding.addTextButton.setOnClickListener {
//            addText("Sample Text", Color.WHITE)
            val textEntryDialog = CustomTextEntryDialog(this) { text, color, backgroundResId ->
                // Add text to the image using the PhotoEditor and selected color
                selectedBackgroundResId = backgroundResId
                addTextWithBackground(text, color)
            }
            textEntryDialog.show()
        }


        binding.doneButton.setOnClickListener {
            try {
                saveEditedImage()
            } catch (e: Exception) {
                Log.e("EditImageActivity", "Error saving image", e)
            }
        }
    }

    // Apply filter to the image
    private fun applyFilter(filter: PhotoFilter) {
        photoEditor.setFilterEffect(filter)
//        Toast.makeText(this, "Filter applied: $filter", Toast.LENGTH_SHORT).show()
    }

    // Add emoji to the image
    private fun addEmoji(emoji: String) {
        photoEditor.addEmoji(emoji)
//        Toast.makeText(this, "Emoji added: $emoji", Toast.LENGTH_SHORT).show()
    }

    // Add text to the image
    private fun addText(text: String, color: Int) {
        photoEditor.addText(text, color)
//        Toast.makeText(this, "Text added", Toast.LENGTH_SHORT).show()
    }

    private fun addTextWithBackground(text: String, color: Int) {
        val textWithBackgroundBitmap = createTextBitmap(text, color, selectedBackgroundResId)
        // Add the bitmap to the image using PhotoEditor
        photoEditor.addImage(textWithBackgroundBitmap)
//        Toast.makeText(this, "Text with background added", Toast.LENGTH_SHORT).show()
    }
//    private fun createTextBitmap(text: String, color: Int, backgroundResId: Int): Bitmap {
//        val view = layoutInflater.inflate(R.layout.text_with_background, null)
//        val textView = view.findViewById<TextView>(R.id.text_view)
//        textView.text = text
//        textView.setTextColor(color)
//        textView.setBackgroundResource(backgroundResId)
//
//        // Measure and layout the view
//        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
//        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
//
//        // Create bitmap and draw the view onto it
//        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
//
//        return bitmap
//    }

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
            if (!checkStoragePermission()) {
                requestStoragePermission()
                return
            }
        }

        progressBar.show()
        binding.photoEditorView.postDelayed({
            val newDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "edited_images").apply {
                if (!exists()) mkdirs()
            }

            val file = File(newDir, "edited_image_${System.currentTimeMillis()}.jpg")

            // Save the edited image
            photoEditor.saveAsFile(file.absolutePath, object : PhotoEditor.OnSaveListener {
                override fun onSuccess(imagePath: String) {
                    Log.d("EditImageActivity", "Image saved at $imagePath")

                    // Create a URI for the saved image
                    val editedImageUri = FileProvider.getUriForFile(
                        this@EditImageActivity,
                        "${packageName}.provider",
                        file
                    )

                    // Return the URI as a result
                    val resultIntent = Intent().apply {
                        putExtra("edited_image_uri", editedImageUri.toString())
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()  // Close activity
                }

                override fun onFailure(exception: Exception) {
                    Toast.makeText(this@EditImageActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
                    Log.e("EditImageActivity", "Error: ${exception.message}", exception)
                }
            })
        }, 300)
    }

    // Check if storage permission is granted
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request storage permission
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE_PERMISSION
        )
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveEditedImage() // Retry saving after permission is granted
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
