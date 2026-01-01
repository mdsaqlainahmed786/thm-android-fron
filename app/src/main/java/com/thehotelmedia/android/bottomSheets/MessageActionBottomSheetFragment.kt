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
import com.thehotelmedia.android.databinding.FragmentMessageActionBottomSheetBinding

class MessageActionBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentMessageActionBottomSheetBinding

    var onEditClick: (() -> Unit)? = null
    var onDeleteClick: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation

        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isDraggable = true
        }
        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_message_action_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        binding.editBtn.setOnClickListener {
            onEditClick?.invoke()
            dismiss()
        }

        binding.deleteBtn.setOnClickListener {
            onDeleteClick?.invoke()
            dismiss()
        }
    }

    companion object {
        fun newInstance(): MessageActionBottomSheetFragment {
            return MessageActionBottomSheetFragment()
        }
    }
}


