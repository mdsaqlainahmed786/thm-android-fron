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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.CollaboratorsAdapter
import com.thehotelmedia.android.databinding.FragmentTagPeopleBottomSheetBinding
import com.thehotelmedia.android.modals.feeds.feed.Collaborator

class CollaboratorsBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTagPeopleBottomSheetBinding
    private var collaboratorsJson: String? = null
    private var collaborators: List<Collaborator>? = null

    companion object {
        private const val ARG_COLLABORATORS_JSON = "ARG_COLLABORATORS_JSON"

        fun newInstance(collaboratorsJson: String): CollaboratorsBottomSheetFragment {
            val fragment = CollaboratorsBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_COLLABORATORS_JSON, collaboratorsJson)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            collaboratorsJson = it.getString(ARG_COLLABORATORS_JSON)
            collaborators = Gson().fromJson(collaboratorsJson, object : TypeToken<List<Collaborator>>() {}.type)
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure the bottom sheet opens to 75% of the screen height
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.75).toInt()

                bottomSheet.layoutParams.height = maxHeight
                behavior.peekHeight = maxHeight
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tag_people_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        collaborators?.let { collaboratorList ->
            val collaboratorsAdapter = CollaboratorsAdapter(requireContext(), collaboratorList)
            binding.itemsRv.adapter = collaboratorsAdapter
        }
    }
}




