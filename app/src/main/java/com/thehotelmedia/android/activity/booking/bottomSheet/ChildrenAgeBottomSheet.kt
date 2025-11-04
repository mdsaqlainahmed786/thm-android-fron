package com.thehotelmedia.android.activity.booking.bottomSheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.booking.PlanDetailsActivity
import com.thehotelmedia.android.adapters.booking.ChildrenAdapter
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.FragmentChildrenAgeBottomSheetBinding

class ChildrenAgeBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentChildrenAgeBottomSheetBinding
    private var childrenCount: Int = 0
    private lateinit var childrenAdapter: ChildrenAdapter
    private val childrenAges: MutableList<Int> = mutableListOf()

    companion object {
        fun newInstance(
            businessProfileId: String?,
            userLargeProfilePic: String?,
            userFullName: String?,
            businessName: String?,
            businessIcon: String?,
            fullAddress: String?,
            rating: Double?,
            checkInDate: String?,
            checkOutDate: String?,
            guestCount: Int?,
            childrenCount: Int?,
            hasPet: Boolean?,
            guestMessage: String?
        ): ChildrenAgeBottomSheet {
            return ChildrenAgeBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("KEY_BUSINESS_PROFILE_ID", businessProfileId)
                    putString("KEY_USER_LARGE_PROFILE_PIC", userLargeProfilePic)
                    putString("KEY_USER_FULL_NAME", userFullName)
                    putString("KEY_BUSINESS_NAME", businessName)
                    putString("KEY_BUSINESS_ICON", businessIcon)
                    putString("KEY_FULL_ADDRESS", fullAddress)
                    putDouble("KEY_RATING", rating ?: 0.0)
                    putString("KEY_CHECK_IN_DATE", checkInDate)
                    putString("KEY_CHECK_OUT_DATE", checkOutDate)
                    putInt("KEY_GUEST_COUNT", guestCount ?: 0)
                    putInt("KEY_CHILDREN_COUNT", childrenCount ?: 0)
                    putBoolean("KEY_HAS_PET", hasPet ?: false)
                    putString("KEY_GUEST_MESSAGE", guestMessage)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme).apply {
            window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_children_age_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        val args = requireArguments()
        childrenCount = args.getInt("KEY_CHILDREN_COUNT", 0)

        childrenAdapter = ChildrenAdapter(requireContext(),childrenCount) { selectedAge ->
            childrenAges.clear()
            childrenAges.addAll(selectedAge) // Update the list with new values
            Log.d("ChildrenAgeBottomSheet", "Updated childrenAges: $childrenAges")
        }

        binding.childrenRv.adapter = childrenAdapter

        binding.proceedBtn.setOnClickListener {
            if (childrenAges.size != childrenCount) {
                CustomSnackBar.showSnackBar(binding.root,getString(R.string.select_all_children_ages))
            } else {
                proceedToPlanDetails(args)
            }
        }
    }

    private fun proceedToPlanDetails(args: Bundle) {
        val intent = Intent(requireContext(), PlanDetailsActivity::class.java).apply {
            putExtras(args)
            putIntegerArrayListExtra("KEY_CHILDREN_AGES", ArrayList(childrenAges))
        }
        startActivity(intent)
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet).apply {
                peekHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                state = BottomSheetBehavior.STATE_EXPANDED
            }
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }
}
