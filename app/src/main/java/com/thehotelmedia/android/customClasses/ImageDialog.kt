package com.thehotelmedia.android.customClasses

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.thehotelmedia.android.databinding.DialogImageViewBinding
import kotlin.math.max
import kotlin.math.min

class ImageDialog(private val context: Context) {

    fun showImage(imageUrl: String) {
        // Initialize the dialog and binding
        val dialog = Dialog(context)
        val binding = DialogImageViewBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)


        // Set dialog window to match parent
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        // Set the dialog window background to transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Load the image using Glide into the ImageView
        Glide.with(context)
            .load(imageUrl)
            .circleCrop()
            .into(binding.dialogCircularImageView)

//      // Set the root view to dismiss the dialog on click
//        binding.dialogCircularImageView.setOnClickListener {
//            dialog.dismiss()
//        }


        // Show the dialog
        dialog.show()
    }
}
