package com.thehotelmedia.android.customClasses

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.WindowManager
import com.airbnb.lottie.LottieAnimationView
import com.thehotelmedia.android.R
import eightbitlab.com.blurview.BlurView



class GiffProgressBar(private val context: Context) {
    private var dialog: Dialog? = null

    fun show() {
        // Prevent showing dialog if context is not a valid Activity or is finishing/destroyed
        if (context is android.app.Activity) {
            if (context.isFinishing || context.isDestroyed) {
                return
            }
        }


        if (dialog == null) {
            dialog = Dialog(context)
            dialog?.let { dlg ->
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(R.layout.giff_progress_bar_layout) // Create a layout for your progress bar or use system default
                dlg.setCancelable(false)
                dlg.setCanceledOnTouchOutside(false)
                val layoutParams = dlg.window?.attributes
                layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.gravity = Gravity.CENTER
                dlg.window?.attributes = layoutParams

                val blurView = dlg.findViewById<BlurView>(R.id.blurView)
                val lottieView = dlg.findViewById<LottieAnimationView>(R.id.lottieView)
                lottieView.setAnimation(R.raw.giff_loading)
                lottieView.playAnimation()
//                context.blurTheView(blurView)
            }
        }
        dialog?.show()
    }

    fun hide() {
        dialog?.dismiss()
    }

//    val progressBar = CustomProgressBar(requireContext()) // 'this' refers to the context
//    progressBar.show() // To show the progress bar
//    progressBar.hide() // To hide the progress bar
}