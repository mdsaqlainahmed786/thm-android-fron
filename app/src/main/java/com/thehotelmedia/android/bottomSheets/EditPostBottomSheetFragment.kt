package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FragmentEditPostBottomSheetBinding

class EditPostBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditPostBottomSheetBinding

    var initialContent: String? = null

    // Callback when Save is clicked
    var onSaveClicked: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialContent = arguments?.getString(ARG_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { BottomSheetBehavior.from(it).isDraggable = false }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_post_bottom_sheet, container, false)
        initUi()
        return binding.root
    }

    private fun initUi() {
        binding.contentEt.setText(initialContent ?: "")
        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.saveBtn.setOnClickListener {
            val updated = binding.contentEt.text?.toString()?.trim().orEmpty()
            if (updated.isNotEmpty()) {
                onSaveClicked?.invoke(updated)
                dismiss()
            }
        }
    }

    companion object {
        private const val ARG_CONTENT = "content"

        fun newInstance(content: String): EditPostBottomSheetFragment {
            val f = EditPostBottomSheetFragment()
            val b = Bundle()
            b.putString(ARG_CONTENT, content)
            f.arguments = b
            return f
        }
    }
}


