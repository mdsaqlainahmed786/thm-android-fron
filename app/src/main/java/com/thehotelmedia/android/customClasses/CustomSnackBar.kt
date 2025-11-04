package com.thehotelmedia.android.customClasses

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.thehotelmedia.android.databinding.CustomSnackBarBinding

class CustomSnackBar {
    companion object {
        fun showSnackBar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {

                val inflater = LayoutInflater.from(view.context)
                val binding = CustomSnackBarBinding.inflate(inflater)

                // Set message
                binding.toastText.text = message

                // Set layout params with margin
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                binding.root.layoutParams = params

                // Create and show toast
                val toast = Toast(view.context)
                toast.duration = Toast.LENGTH_SHORT
                toast.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 100)
                toast.view = binding.root
                toast.show()

        }

    }
}