package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.ReportReasonsAdapter
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.FragmentReportBottomSheetBinding

class ReportBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentReportBottomSheetBinding

    private var id = ""
    private var type = ""

    private lateinit var reportReasonsAdapter: ReportReasonsAdapter

    // Lambda callback to send the selected reason back
    var onReasonSelected: ((String) -> Unit)? = null


    // Get the reasons from resources
    private val userReasons: List<String> by lazy {
        resources.getStringArray(R.array.user_reasons).toList()
    }

    private val postReasons: List<String> by lazy {
        resources.getStringArray(R.array.post_reasons).toList()
    }

    private val commentReasons: List<String> by lazy {
        resources.getStringArray(R.array.comment_reasons).toList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        // You can also set a custom enter animation here if needed
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onStart() {
        super.onStart()
        // Ensure the bottom sheet opens to 75% of the screen height
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {

                val behavior = BottomSheetBehavior.from(it)
                // Get the screen height
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.75).toInt() // 75% of the screen height

                // Set the bottom sheet height to 75% and restrict it from expanding more
                bottomSheet.layoutParams.height = maxHeight
                behavior.peekHeight = maxHeight

                // Optional: Set a fixed state to stop expanding more than the max height
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // Disable expanding beyond 75%
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Optional: you can control the sliding here
                    }
                })

            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_bottom_sheet, container, false)

        initUI()
        return binding.root
    }

    private fun initUI() {

        id = arguments?.getString("ID") ?: ""
        type = arguments?.getString("TYPE") ?: ""

        if (type == "user"){
            reportUser()
        }else{
            reportPost()
        }


        if (type == "user"){
            binding.reportTitle.text = MessageStore.whyReportingUser(requireContext())
            reportReasonsAdapter = ReportReasonsAdapter(requireContext(),userReasons,::userReasons)
        }else if (type == "comment"){
            binding.reportTitle.text = MessageStore.whyReportingComment(requireContext())
            reportReasonsAdapter = ReportReasonsAdapter(requireContext(), commentReasons,::userReasons)
        }else if (type == "post"){
            binding.reportTitle.text = MessageStore.whyReportingPost(requireContext())
            reportReasonsAdapter = ReportReasonsAdapter(requireContext(), postReasons,::userReasons)
        }
        binding.reportRv.adapter = reportReasonsAdapter

    }


    private fun userReasons(reason: String) {
        onReasonSelected?.invoke(reason)  // Invoke the lambda callback with the selected reason
        dismiss()  // Close the bottom sheet after selecting the reason
    }

    private fun reportUser() {

    }

    private fun reportPost() {

    }

}