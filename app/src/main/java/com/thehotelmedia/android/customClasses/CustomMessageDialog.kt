package com.thehotelmedia.android.customClasses

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.CustomMessageDialogBinding

class CustomMessageDialog(
    context: Context,
    private val message: String,
    private val onDismiss: () -> Unit
) {
    private val dialog: Dialog = Dialog(context).apply {
        setCancelable(false) // Make it non-cancelable
        window?.setBackgroundDrawableResource(android.R.color.transparent) // Make background transparent
    }

    init {
        val binding = CustomMessageDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root) // Set the root view of the binding

        binding.dialogMessage.text = message // Set the message

        // Show the dialog with fade-in animation
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).start()
    }

    fun show() {
        dialog.show()

        // Dismiss the dialog after 3 seconds and call the onDismiss callback
        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 3000) // 3000 milliseconds = 3 seconds
    }

    private fun dismiss() {
        dialog.dismiss()
        onDismiss()
    }

    private fun setContentView(view: View) {
        dialog.setContentView(view)
    }
}
