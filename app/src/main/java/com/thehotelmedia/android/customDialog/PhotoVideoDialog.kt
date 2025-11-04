package com.thehotelmedia.android.customDialog

import android.app.Activity
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thehotelmedia.android.databinding.DialogPhotoVideoBinding

class PhotoVideoDialog(
    private val activity: Activity,
    private val title: String,
    private val photoText: String,
    private val videoText: String,
    private val photoIconResId: Int,
    private val videoIconResId: Int,
    private val photoClickListener: () -> Unit,
    private val videoClickListener: () -> Unit,
    private val onDismissListener: (() -> Unit)? = null,
    private val autoCancel: Boolean,
) {

    fun show() {
        // Inflate the custom layout using View Binding
        val binding = DialogPhotoVideoBinding.inflate(LayoutInflater.from(activity))
        // Create AlertDialog using MaterialAlertDialogBuilder
        val alertDialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(autoCancel)
            .setOnCancelListener {

            }
            .create()

        // Set custom title text
        binding.title.text = title

        // Set dynamic text and icons for photo and video buttons
        binding.photoTv.text = photoText
        binding.videoTv.text = videoText

        binding.photoIv.setImageResource(photoIconResId)
        binding.videoIv.setImageResource(videoIconResId)

        // Set button click listeners using binding
        binding.photoBtn.setOnClickListener {
            alertDialog.dismiss()
            photoClickListener.invoke()  // Call the photo click listener
        }

        binding.videoBtn.setOnClickListener {
            alertDialog.dismiss()
            videoClickListener.invoke()  // Call the video click listener
        }
        // Set a dismiss listener for the dialog
        alertDialog.setOnDismissListener {
            onDismissListener?.invoke()  // Invoke the dismiss listener, if provided
        }

        // Show the dialog
        alertDialog.show()
    }
}
