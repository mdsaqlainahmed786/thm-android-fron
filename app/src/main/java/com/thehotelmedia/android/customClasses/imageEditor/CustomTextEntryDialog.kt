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

class CustomTextEntryDialog(
    private val context: Context,
    private val onTextConfirmed: (String, Int, Int) -> Unit
) {
    private var dialog: Dialog? = null
    private var binding: DialogTextEntryBinding? = null
    private var currentBackgroundIndex = 0
    private val backgrounds = arrayOf(
        R.drawable.text_background_transparent,
        R.drawable.text_background_white,
        R.drawable.text_background_black
    )

    private var selectedColor: Int = Color.WHITE

//    private var currentTextAlignment = View.TEXT_ALIGNMENT_CENTER // Default alignment is left


    fun show(
        initialText: String? = null,
        initialColor: Int? = null,
        initialBackgroundResId: Int? = null
    ) {
        if (dialog == null) {
            dialog = Dialog(context)
            dialog?.let { dlg ->
                val inflatedBinding = DialogTextEntryBinding.inflate(dlg.layoutInflater)
                binding = inflatedBinding

                dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dlg.setContentView(inflatedBinding.root)
                dlg.setCancelable(true)
                dlg.setCanceledOnTouchOutside(true)

                inflatedBinding.doneBtn.setOnClickListener {
                    val text = inflatedBinding.editText.text.toString().trim()
                    if (text.isNotEmpty()) {
                        val backgroundResId = getCurrentBackground()
                        onTextConfirmed(text, selectedColor, backgroundResId)
                        dlg.dismiss()
                    } else {
                        dlg.dismiss()
                    }
                }

                inflatedBinding.changeBackgroundButton.setOnClickListener {
                    applyNextBackground(inflatedBinding.editText)
                }

                // Alignment button listener
//                binding.alignmentButton.setOnClickListener {
//                    toggleTextAlignment(binding.editText, binding.alignmentButton)
//                }

                inflatedBinding.colorButton.setOnClickListener {
                    ColorPickerDialog
                        .Builder(context)
                        .setTitle(MessageStore.pickColor(context))
                        .setColorShape(ColorShape.SQAURE) // Corrected spelling
                        .setDefaultColor(R.color.white)
                        .setColorListener { color, _ ->
                            selectedColor = color
                            inflatedBinding.editText.setTextColor(selectedColor)
                        }
                        .show()
                }

                inflatedBinding.editText.requestFocus()

                dlg.window?.apply {
                    setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                    attributes?.gravity = Gravity.CENTER
                }
                dlg.setOnDismissListener {
                    binding = null
                    dialog = null
                }
            }
        }
        dialog?.let { dlg ->
            val dialogBinding = binding ?: DialogTextEntryBinding.inflate(dlg.layoutInflater).also {
                dlg.setContentView(it.root)
                binding = it
            }
            val editText = dialogBinding.editText

            val resolvedBackgroundIndex = initialBackgroundResId?.let { resId ->
                backgrounds.indexOf(resId).takeIf { it >= 0 }
            } ?: currentBackgroundIndex
            currentBackgroundIndex = resolvedBackgroundIndex

            selectedColor = initialColor ?: Color.WHITE

            editText.setText(initialText.orEmpty())
            editText.setSelection(editText.text?.length ?: 0)

            editText.setTextColor(selectedColor)
            applyBackgroundAtIndex(editText, resolvedBackgroundIndex, initialColor)

            dlg.show()
        }
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
        val nextIndex = (currentBackgroundIndex + 1) % backgrounds.size
        applyBackgroundAtIndex(editText, nextIndex, null)
    }

    private fun applyBackgroundAtIndex(editText: EditText, index: Int, overrideColor: Int?) {
        currentBackgroundIndex = index
        val backgroundResId = backgrounds[currentBackgroundIndex]
        editText.setBackgroundResource(backgroundResId)

        when {
            overrideColor != null -> {
                selectedColor = overrideColor
                editText.setTextColor(overrideColor)
            }
            backgroundResId == R.drawable.text_background_white -> {
                selectedColor = Color.BLACK
                editText.setTextColor(Color.BLACK)
            }
            backgroundResId == R.drawable.text_background_black -> {
                selectedColor = Color.WHITE
                editText.setTextColor(Color.WHITE)
            }
            else -> {
                editText.setTextColor(selectedColor)
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
