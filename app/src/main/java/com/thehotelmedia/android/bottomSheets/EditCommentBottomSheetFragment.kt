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
import com.thehotelmedia.android.databinding.FragmentEditMessageBottomSheetBinding

class EditCommentBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditMessageBottomSheetBinding
    private var messageText: String = ""

    var onSaveClick: ((String) -> Unit)? = null
    var onCancelClick: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageText = arguments?.getString(ARG_MESSAGE) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation

        // Disable drag behavior by setting a custom BottomSheetBehavior
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Open expanded
            behavior.skipCollapsed = true
        }

        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_message_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        binding.messageEt.setText(messageText)
        binding.messageEt.setSelection(messageText.length)
        binding.titleTv.text = getString(R.string.edit_comment)

        binding.saveBtn.setOnClickListener {
            val newMessage = binding.messageEt.text.toString().trim()
            if (newMessage.isNotEmpty()) {
                onSaveClick?.invoke(newMessage)
                dismiss()
            }
        }

        binding.cancelBtn.setOnClickListener {
            onCancelClick?.invoke()
            dismiss()
        }
    }

    companion object {
        private const val ARG_MESSAGE = "ARG_MESSAGE"

        fun newInstance(currentMessage: String): EditCommentBottomSheetFragment {
            return EditCommentBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, currentMessage)
                }
            }
        }
    }
}



