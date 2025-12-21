package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FragmentPostMenuBottomSheetBinding

class PostMenuBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPostMenuBottomSheetBinding
    
    private var isOwner: Boolean = false
    private var postId: String = ""
    
    // Callback functions
    var onEditClicked: (() -> Unit)? = null
    var onDeleteClicked: (() -> Unit)? = null
    var onReportClicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isOwner = it.getBoolean(ARG_IS_OWNER, false)
            postId = it.getString(ARG_POST_ID, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        android.util.Log.e("PostMenuBottomSheet", "onCreateDialog called")
        try {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
            bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
            android.util.Log.e("PostMenuBottomSheet", "BottomSheetDialog created successfully")
            return bottomSheetDialog
        } catch (e: Exception) {
            android.util.Log.e("PostMenuBottomSheet", "ERROR in onCreateDialog: ${e.message}", e)
            throw e
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.e("PostMenuBottomSheet", "onCreateView called")
        try {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_post_menu_bottom_sheet, container, false)
            android.util.Log.e("PostMenuBottomSheet", "Binding inflated successfully")
            initUI()
            android.util.Log.e("PostMenuBottomSheet", "initUI completed")
            return binding.root
        } catch (e: Exception) {
            android.util.Log.e("PostMenuBottomSheet", "ERROR in onCreateView: ${e.message}", e)
            throw e
        }
    }

    private fun initUI() {
        android.util.Log.e("PostMenuBottomSheet", "initUI called - isOwner: $isOwner, postId: $postId")
        try {
            // Find TextViews and set click listeners
            val editBtn: TextView? = binding.root.findViewById(R.id.editBtn)
            val deleteBtn: TextView? = binding.root.findViewById(R.id.deleteBtn)
            val reportBtn: TextView? = binding.root.findViewById(R.id.reportBtn)
            val addToStoryBtn: TextView? = binding.root.findViewById(R.id.addToStoryBtn)

            android.util.Log.e("PostMenuBottomSheet", "Found buttons - editBtn: $editBtn, deleteBtn: $deleteBtn, reportBtn: $reportBtn")

            // Show/hide buttons based on ownership
            if (isOwner) {
                // Owner sees Edit and Delete options
                android.util.Log.e("PostMenuBottomSheet", "Showing Edit and Delete for owner")
                editBtn?.visibility = View.VISIBLE
                deleteBtn?.visibility = View.VISIBLE
                reportBtn?.visibility = View.GONE
                addToStoryBtn?.visibility = View.GONE
            } else {
                // Others see only Report option
                android.util.Log.e("PostMenuBottomSheet", "Showing Report for non-owner")
                editBtn?.visibility = View.GONE
                deleteBtn?.visibility = View.GONE
                reportBtn?.visibility = View.VISIBLE
                addToStoryBtn?.visibility = View.GONE
            }

            // Edit button click listener
            editBtn?.setOnClickListener {
                android.util.Log.e("PostMenuBottomSheet", "Edit button clicked")
                onEditClicked?.invoke()
                dismiss()
            }

            // Delete button click listener
            deleteBtn?.setOnClickListener {
                android.util.Log.e("PostMenuBottomSheet", "Delete button clicked")
                onDeleteClicked?.invoke()
                dismiss()
            }

            // Report button click listener
            reportBtn?.setOnClickListener {
                android.util.Log.e("PostMenuBottomSheet", "Report button clicked")
                onReportClicked?.invoke()
                dismiss()
            }
            
            android.util.Log.e("PostMenuBottomSheet", "initUI completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("PostMenuBottomSheet", "ERROR in initUI: ${e.message}", e)
            e.printStackTrace()
        }
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_IS_OWNER = "is_owner"

        fun newInstance(postId: String, isOwner: Boolean): PostMenuBottomSheetFragment {
            val fragment = PostMenuBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_POST_ID, postId)
            args.putBoolean(ARG_IS_OWNER, isOwner)
            fragment.arguments = args
            return fragment
        }
    }
}

