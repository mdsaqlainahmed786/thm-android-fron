package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.FragmentBlockUserBottomSheetBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class BlockUserBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBlockUserBottomSheetBinding
    private var isBlocked: Boolean? = null
    private var userId: String? = null
    private var callback: BottomSheetListener? = null

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    // Interface to send data back to the Activity
    interface BottomSheetListener {
        fun onBooleanDataReceived(isUserBlocked: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callback = context as BottomSheetListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement BottomSheetListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            isBlocked = it.getBoolean("IS_BLOCKED")
            userId = it.getString("USER_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_block_user_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {

        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        val blockText = getString(R.string.block_text)
        val unBlockText = getString(R.string.unblock_text)

        binding.titleTv.text = if (isBlocked == true) unBlockText else blockText

//        // Observer for blocking result
//        individualViewModal.blockUserResult.observe(viewLifecycleOwner) { result ->
//            if (result.status == true) {
//                // Handle the result and update isBlocked status
//                isBlocked = !(isBlocked ?: false)
//                callback?.onBooleanDataReceived(isBlocked!!)
//                dismiss() // Dismiss after updating data
//            } else {
//                val msg = result.message.toString()
//                CustomSnackBar.showSnackBar(binding.root, msg)
//            }
//        }
//
//        // Observer for showing toast messages
//        individualViewModal.toast.observe(viewLifecycleOwner) { message ->
//            CustomSnackBar.showSnackBar(binding.root, message)
//        }

        // Button listeners
        binding.yesBtn.setOnClickListener {
            progressBar.show()
            individualViewModal.blockUser(userId.toString())
            println("asfkasfhajsf   $userId")
            isBlocked = !(isBlocked ?: false)
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.hide()
                callback?.onBooleanDataReceived(isBlocked!!)
                dismiss() // Dismiss after updating data
            }, 500) // 1000 milliseconds = 1 second

        }

        binding.noBtn.setOnClickListener {
            dismiss() // Dismiss the dialog when 'No' is clicked
        }
    }


    companion object {
        fun newInstance(isBlocked: Boolean, userId: String): BlockUserBottomSheetFragment {
            val fragment = BlockUserBottomSheetFragment()
            val bundle = Bundle().apply {
                putBoolean("IS_BLOCKED", isBlocked)
                putString("USER_ID", userId)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
