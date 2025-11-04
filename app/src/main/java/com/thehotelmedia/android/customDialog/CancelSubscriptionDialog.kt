package com.thehotelmedia.android.customDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.thehotelmedia.android.databinding.DialogCancelSubscriptionBinding

class CancelSubscriptionDialog(private val onYesClicked: () -> Unit) : DialogFragment() {

    private lateinit var binding: DialogCancelSubscriptionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the dialog layout using ViewBinding
        binding = DialogCancelSubscriptionBinding.inflate(inflater, container, false)

        // Set up the "Yes" button click listener
        binding.yesBtn.setOnClickListener {
            // Call the lambda function passed from the activity
            onYesClicked()

            // Dismiss the dialog
            dismiss()
        }

        // Set up the "Cancel" button click listener
        binding.cancelBtn.setOnClickListener {
            // Dismiss the dialog without doing anything
            dismiss()
        }
        // Set up the "Cancel" button click listener
        binding.noBtn.setOnClickListener {
            // Dismiss the dialog without doing anything
            dismiss()
        }

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params
    }
}
