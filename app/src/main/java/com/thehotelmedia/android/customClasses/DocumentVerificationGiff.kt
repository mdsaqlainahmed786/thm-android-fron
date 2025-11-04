package com.thehotelmedia.android.customClasses

import android.animation.Animator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import eightbitlab.com.blurview.BlurView

class DocumentVerificationGiff(private val context: Context) {
    private var dialog: Dialog? = null
    private var onAnimationComplete: (() -> Unit)? = null

    fun show(message: String, onAnimationComplete: () -> Unit) {
        this.onAnimationComplete = onAnimationComplete
        if (dialog == null) {
            dialog = Dialog(context)
            dialog?.let { dlg ->
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(R.layout.document_verificcation_giff__layout)
                dlg.setCancelable(false)
                dlg.setCanceledOnTouchOutside(false)
                val layoutParams = dlg.window?.attributes
                layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.gravity = Gravity.CENTER
                dlg.window?.attributes = layoutParams

                val blurView = dlg.findViewById<BlurView>(R.id.blurView)
//                val lottieView = dlg.findViewById<LottieAnimationView>(R.id.lottieView)
                val giffImageView = dlg.findViewById<ImageView>(R.id.giffImageView)
                val messageTv = dlg.findViewById<TextView>(R.id.messageTv)

                // Set the message text
                messageTv.text = message

                Glide.with(context)
                    .asGif()
                    .load(R.raw.document) // Replace with your GIF URL or file
                    .into(giffImageView)
//                // Set and play Lottie animation
//                lottieView.setAnimation(R.raw.document_verification_giff)
//                lottieView.repeatCount = 0 // Play animation only once
//                lottieView.playAnimation()
//
//                // Add animation listener to detect when animation is complete
//                lottieView.addAnimatorListener(object : Animator.AnimatorListener {
//                    override fun onAnimationStart(animation: Animator) {}
//                    override fun onAnimationEnd(animation: Animator) {
//                        // Call the lambda function when the animation ends
//                        hide() // Hide the dialog
//                        onAnimationComplete.invoke() // Notify activity or fragment
//                    }
//                    override fun onAnimationCancel(animation: Animator) {}
//                    override fun onAnimationRepeat(animation: Animator) {}
//                })
            }
        }
        dialog?.show()

        // Schedule dialog dismissal after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            hide()
            onAnimationComplete.invoke()
        }, 4000)
    }

    fun hide() {
        dialog?.dismiss()
        dialog = null // Cleanup dialog reference
    }
}
