package com.thehotelmedia.android.customClasses

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.extensions.blurTheView
import eightbitlab.com.blurview.BlurView

class CustomProgressBar(private val context: Context) {
    private var dialog: Dialog? = null

//    private val handler = Handler(Looper.getMainLooper())
//
//    private val serverDownRunnable = Runnable {
//        // Show a toast message after 1 minute
//        if (dialog?.isShowing == true) {
//            Toast.makeText(context, "Server is down, please try again later", Toast.LENGTH_LONG).show()
//        }
//    }

    fun show() {

        if (dialog == null ) {

            dialog = Dialog(context)
            dialog?.let { dlg ->
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(R.layout.progress_bar_layout) // Create a layout for your progress bar or use system default
                dlg.setCancelable(false)
                dlg.setCanceledOnTouchOutside(false)
                val layoutParams = dlg.window?.attributes
                layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.gravity = Gravity.CENTER
                dlg.window?.attributes = layoutParams

                val lottieAnimationView = dlg.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
                lottieAnimationView.playAnimation() // Start animation
                lottieAnimationView.loop(true) // Loop animation
            }
        }
        dialog?.show()

//        // Start a delayed task to show the toast after 20 sec (20,000 milliseconds)
//        handler.postDelayed(serverDownRunnable, 20000)
    }


    fun hide() {
        // Ensure that the dialog is currently being shown before attempting to dismiss
        dialog?.let {
            if (it.isShowing) {
                try {
                    it.dismiss()
                    dialog = null // Set dialog to null after dismissal to avoid memory leaks
                } catch (e: IllegalArgumentException) {
                    // Catch any IllegalArgumentException and log it if needed
                    e.printStackTrace()
                }
            }
        }
    }

}
