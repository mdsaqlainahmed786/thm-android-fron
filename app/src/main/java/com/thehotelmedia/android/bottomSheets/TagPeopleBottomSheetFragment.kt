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
import com.thehotelmedia.android.adapters.TaggedPeopleHomeAdapter
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentTagPeopleBottomSheetBinding
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef


class TagPeopleBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding : FragmentTagPeopleBottomSheetBinding
    private var taggedPeopleJson: String? = null
    private var taggedPeople: List<TaggedRef>? = null

    private lateinit var preferenceManager: PreferenceManager

    companion object {
        private const val ARG_TAGGED_PEOPLE_JSON = "ARG_TAGGED_PEOPLE_JSON"

        fun newInstance(taggedPeopleJson: String): TagPeopleBottomSheetFragment {
            val fragment = TagPeopleBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_TAGGED_PEOPLE_JSON, taggedPeopleJson)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        // You can also set a custom enter animation here if needed
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            taggedPeopleJson = it.getString(ARG_TAGGED_PEOPLE_JSON)
            taggedPeople = Gson().fromJson(taggedPeopleJson, object : TypeToken<List<TaggedRef>>() {}.type)
        }
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

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tag_people_bottom_sheet, container, false)

        initUI()
        return binding.root

//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_tag_people_bottom_sheet, container, false)
    }

    private fun initUI() {

        println("skajfhj   $taggedPeople")
        preferenceManager = PreferenceManager.getInstance(requireContext())
        val ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()



        val taggedPeopleAdapter = TaggedPeopleHomeAdapter(requireContext(),taggedPeople!!,ownerUserId)
        binding.itemsRv.adapter = taggedPeopleAdapter


    }

}