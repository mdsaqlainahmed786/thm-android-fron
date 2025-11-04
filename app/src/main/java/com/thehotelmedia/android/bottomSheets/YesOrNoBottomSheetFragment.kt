package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FragmentYesOrNoBottomSheetBinding


class YesOrNoBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding : FragmentYesOrNoBottomSheetBinding

    private var title: String? = null

    // Callback functions
    var onYesClicked: (() -> Unit)? = null
    var onNoClicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation

        // Disable drag behavior by setting a custom BottomSheetBehavior
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isDraggable = false // Disable dragging
        }

        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_yes_or_no_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {

        // Set the title from arguments
        binding.titleTv.text = title
        binding.yesBtn.setOnClickListener {
            onYesClicked?.invoke()
            dismiss()
        }

        binding.noBtn.setOnClickListener {
            onNoClicked?.invoke()
            dismiss()
        }

    }

//    override fun onDismiss(dialog: DialogInterface) {
//        super.onDismiss(dialog)
//        // Trigger the onNoClicked callback when dismissed
//        onNoClicked?.invoke()
//    }


    companion object {
        private const val ARG_TITLE = "title"

        fun newInstance(title: String): YesOrNoBottomSheetFragment {
            val fragment = YesOrNoBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }


}