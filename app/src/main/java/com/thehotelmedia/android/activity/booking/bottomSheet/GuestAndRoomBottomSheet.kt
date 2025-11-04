package com.thehotelmedia.android.activity.booking.bottomSheet

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
import com.thehotelmedia.android.databinding.FragmentGuestAndRoomBottomSheetBinding
import com.thehotelmedia.android.extensions.toggleEnable

class GuestAndRoomBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentGuestAndRoomBottomSheetBinding
    private var guestCount = 1
    private var childrenCount = 0
    private var hasPet = false

    private var onApplyClick: ((Int, Int, Boolean) -> Unit)? = null

    companion object {
        fun newInstance(
            guestCount: Int,
            childrenCount: Int,
            hasPet: Boolean,
            onApplyClick: (Int, Int, Boolean) -> Unit
        ): GuestAndRoomBottomSheet {
            return GuestAndRoomBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt("guest_count", guestCount)
                    putInt("children_count", childrenCount)
                    putBoolean("has_pet", hasPet)
                }
                this.onApplyClick = onApplyClick // This line won't work; fix it in `onCreate`
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            guestCount = it.getInt("guest_count", 1)
            childrenCount = it.getInt("children_count", 0)
            hasPet = it.getBoolean("has_pet", false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_guest_and_room_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        binding.guestNumberTv.text = guestCount.toString()
        binding.guestDecBtn.toggleEnable(guestCount > 1)

        binding.childrenNumberTv.text = childrenCount.toString()
        binding.childrenDecBtn.toggleEnable(childrenCount > 0)

        updateCheckBox(hasPet)

        binding.guestAddBtn.setOnClickListener {
            guestCount++
            binding.guestNumberTv.text = guestCount.toString()
            binding.guestDecBtn.toggleEnable(true)
        }

        binding.guestDecBtn.setOnClickListener {
            if (guestCount > 1) {
                guestCount--
                binding.guestNumberTv.text = guestCount.toString()
                binding.guestDecBtn.toggleEnable(guestCount > 1)

            }
        }

        binding.childrenAddBtn.setOnClickListener {
            childrenCount++
            binding.childrenNumberTv.text = childrenCount.toString()
            binding.childrenDecBtn.toggleEnable(true)
            binding.applyBtn.text = getString(R.string.next)
        }

        binding.childrenDecBtn.setOnClickListener {
            if (childrenCount > 0) {
                childrenCount--
                binding.childrenNumberTv.text = childrenCount.toString()
                binding.childrenDecBtn.toggleEnable(childrenCount > 0)
                if (childrenCount ==0){
                    binding.applyBtn.text = getString(R.string.proceed)
                }
            }
        }

        binding.petCheckBox.setOnClickListener {
            hasPet = !hasPet
            updateCheckBox(hasPet)
        }

        binding.applyBtn.setOnClickListener {
            onApplyClick?.invoke(guestCount, childrenCount, hasPet)
            dismiss()
        }
    }

    private fun updateCheckBox(hasPetValue: Boolean) {
        binding.petCheckBox.setImageResource(
            if (hasPetValue) R.drawable.ic_round_selected_checkbox
            else R.drawable.ic_round_unselected_checkbox
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
    }
}
