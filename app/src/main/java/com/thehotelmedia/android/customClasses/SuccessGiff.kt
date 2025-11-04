package com.thehotelmedia.android.customClasses

import android.animation.Animator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.thehotelmedia.android.R
import eightbitlab.com.blurview.BlurView

class SuccessGiff(private val context: Context) {
    private var dialog: Dialog? = null
    private var onAnimationComplete: (() -> Unit)? = null

    fun show(message: String, onAnimationComplete: () -> Unit) {
        this.onAnimationComplete = onAnimationComplete
        if (dialog == null) {
            dialog = Dialog(context)
            dialog?.let { dlg ->
                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(R.layout.success_giff__layout)
                dlg.setCancelable(false)
                dlg.setCanceledOnTouchOutside(false)
                val layoutParams = dlg.window?.attributes
                layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams?.gravity = Gravity.CENTER
                dlg.window?.attributes = layoutParams

                val blurView = dlg.findViewById<BlurView>(R.id.blurView)
                val lottieView = dlg.findViewById<LottieAnimationView>(R.id.lottieView)
                val messageTv = dlg.findViewById<TextView>(R.id.messageTv)

                // Set the message text
                messageTv.text = message

                // Set and play Lottie animation
                lottieView.setAnimation(R.raw.success_giff)
                lottieView.repeatCount = 0 // Play animation only once
                lottieView.playAnimation()

                // Add animation listener to detect when animation is complete
                lottieView.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        // Call the lambda function when the animation ends
                        hide() // Hide the dialog
                        onAnimationComplete.invoke() // Notify activity or fragment
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }
        }
        dialog?.show()
    }

    fun hide() {
        dialog?.dismiss()
        dialog = null // Cleanup dialog reference
    }
}
