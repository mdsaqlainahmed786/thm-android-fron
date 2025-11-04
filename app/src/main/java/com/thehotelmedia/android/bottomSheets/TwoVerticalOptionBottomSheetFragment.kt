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
import com.thehotelmedia.android.databinding.FragmentTwoOptionVerticalBottomSheetBinding


class TwoVerticalOptionBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTwoOptionVerticalBottomSheetBinding

    var onBlockClick: (() -> Unit)? = null
    var onViewProfileClick: (() -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation

        // Disable drag behavior by setting a custom BottomSheetBehavior
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isDraggable = true // Disable dragging
        }
        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_two_option_vertical_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {


        // Retrieve arguments
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val blockButtonText = arguments?.getString(ARG_BLOCK_BTN_TEXT) ?: ""
        val viewProfileButtonText = arguments?.getString(ARG_VIEW_PROFILE_BTN_TEXT) ?: ""
        if (title.isEmpty()){
            binding.titleTv.visibility = View.GONE
            binding.titleView.visibility = View.GONE
        }else{
            binding.titleTv.visibility = View.VISIBLE
            binding.titleView.visibility = View.VISIBLE
        }

        // Set text to UI elements
        binding.titleTv.text = title
        binding.blockBtn.text = blockButtonText
        binding.viewProfileBtn.text = viewProfileButtonText


        binding.blockBtn.setOnClickListener {
            onBlockClick?.invoke()
        }
        binding.viewProfileBtn.setOnClickListener {
            onViewProfileClick?.invoke()
        }



    }


    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }


    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_BLOCK_BTN_TEXT = "block_btn_text"
        private const val ARG_VIEW_PROFILE_BTN_TEXT = "view_profile_btn_text"

        fun newInstance(
            title: String,
            blockButtonText: String,
            viewProfileButtonText: String
        ): TwoVerticalOptionBottomSheetFragment {
            val fragment = TwoVerticalOptionBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_BLOCK_BTN_TEXT, blockButtonText)
            args.putString(ARG_VIEW_PROFILE_BTN_TEXT, viewProfileButtonText)
            fragment.arguments = args
            return fragment
        }
    }

}