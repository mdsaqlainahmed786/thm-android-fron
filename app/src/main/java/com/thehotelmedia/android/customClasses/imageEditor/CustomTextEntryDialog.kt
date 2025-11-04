package com.thehotelmedia.android.customClasses.imageEditor

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.widget.AppCompatImageView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.DialogTextEntryBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.thehotelmedia.android.customClasses.MessageStore

class CustomTextEntryDialog(private val context: Context, private val onTextConfirmed: (String, Int, Int) -> Unit) {
    private var dialog: Dialog? = null
    private var currentBackgroundIndex = 0 // Initialize to 0 instead of -1
    private val backgrounds = arrayOf(
        R.drawable.text_background_transparent,
        R.drawable.text_background_white,
        R.drawable.text_background_black
    )

    private var selectedColor: Int = Color.WHITE

//    private var currentTextAlignment = View.TEXT_ALIGNMENT_CENTER // Default alignment is left


    fun show() {
        if (dialog == null) {
            dialog = Dialog(context)
            dialog?.let { dlg ->
                val binding = DialogTextEntryBinding.inflate(dlg.layoutInflater)

                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(binding.root)
                dlg.setCancelable(true)
                dlg.setCanceledOnTouchOutside(true)

                binding.doneBtn.setOnClickListener {
                    val text = binding.editText.text.toString().trim()
                    if (text.isNotEmpty()){
                        val backgroundResId = getCurrentBackground()
                        onTextConfirmed(text, selectedColor, backgroundResId)
                        dlg.dismiss()
                    }else{
                        dlg.dismiss()
                    }

                }

                binding.changeBackgroundButton.setOnClickListener {
                    applyNextBackground(binding.editText)
                }

                // Alignment button listener
//                binding.alignmentButton.setOnClickListener {
//                    toggleTextAlignment(binding.editText, binding.alignmentButton)
//                }

                binding.colorButton.setOnClickListener {
                    ColorPickerDialog
                        .Builder(context)
                        .setTitle(MessageStore.pickColor(context))
                        .setColorShape(ColorShape.SQAURE) // Corrected spelling
                        .setDefaultColor(R.color.white)
                        .setColorListener { color, _ ->
                            selectedColor = color
                            binding.editText.setTextColor(selectedColor)
                        }
                        .show()
                }

                binding.editText.requestFocus()

                dlg.window?.apply {
                    setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                    attributes?.gravity = Gravity.CENTER
                }
            }
        }
        dialog?.show()
    }

    fun hide() {
        dialog?.dismiss()
    }

//    private fun applyNextBackground(editText: EditText) {
//        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgrounds.size
//        val backgroundResId = backgrounds[currentBackgroundIndex]
//        editText.setBackgroundResource(backgroundResId)
//    }

private fun applyNextBackground(editText: EditText) {
    currentBackgroundIndex = (currentBackgroundIndex + 1) % backgrounds.size
    val backgroundResId = backgrounds[currentBackgroundIndex]
    editText.setBackgroundResource(backgroundResId)

    // Change text color based on the applied background
    when (backgroundResId) {
        R.drawable.text_background_white -> {
            editText.setTextColor(Color.BLACK) // Set text color to black when background is white
            selectedColor = Color.BLACK
        }
        R.drawable.text_background_black -> {
            editText.setTextColor(Color.WHITE) // Set text color to white when background is black
            selectedColor = Color.WHITE
        }
        else -> {
            editText.setTextColor(selectedColor) // Use the selected color for other backgrounds
            selectedColor = selectedColor
        }
    }
}

//    private fun toggleTextAlignment(editText: EditText, alignmentButton: AppCompatImageView) {
//        when (currentTextAlignment) {
//            View.TEXT_ALIGNMENT_VIEW_START -> {
//                currentTextAlignment = View.TEXT_ALIGNMENT_CENTER
//                editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
//                alignmentButton.setImageResource(R.drawable.ic_text_align_center) // Update icon
//            }
//            View.TEXT_ALIGNMENT_CENTER -> {
//                currentTextAlignment = View.TEXT_ALIGNMENT_VIEW_END
//                editText.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
//                alignmentButton.setImageResource(R.drawable.ic_text_align_right) // Update icon
//            }
//            View.TEXT_ALIGNMENT_VIEW_END -> {
//                currentTextAlignment = View.TEXT_ALIGNMENT_VIEW_START
//                editText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
//                alignmentButton.setImageResource(R.drawable.ic_text_align_left) // Update icon
//            }
//        }
//    }


    private fun getCurrentBackground(): Int {
        return backgrounds[currentBackgroundIndex]
    }
}
