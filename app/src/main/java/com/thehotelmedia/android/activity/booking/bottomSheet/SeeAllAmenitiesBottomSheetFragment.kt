package com.thehotelmedia.android.activity.booking.bottomSheet

import android.app.Dialog
import android.content.res.Resources
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
import com.thehotelmedia.android.adapters.booking.BusinessAmenitiesParentAdapter
import com.thehotelmedia.android.databinding.FragmentSeeAllAmenitiesBottomSheetBinding
import com.thehotelmedia.android.modals.booking.roomDetails.AmenitiesRef


class SeeAllAmenitiesBottomSheetFragment : BottomSheetDialogFragment() {


    private lateinit var binding: FragmentSeeAllAmenitiesBottomSheetBinding
    private var amenitiesList: ArrayList<AmenitiesRef> = arrayListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_see_all_amenities_bottom_sheet, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        val jsonAmenities = arguments?.getString("amenities_json")
        jsonAmenities?.let {
            val gson = Gson()
            val type = object : TypeToken<ArrayList<AmenitiesRef>>() {}.type
            amenitiesList = gson.fromJson(it, type) // Convert JSON back to object
        }

        val groupedItems = groupItemsByCategory(amenitiesList)


        val businessAmenitiesParentAdapter = BusinessAmenitiesParentAdapter(requireContext(),groupedItems)
        binding.amenitiesParentRv.adapter = businessAmenitiesParentAdapter







    }


    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet).apply {
                peekHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                state = BottomSheetBehavior.STATE_EXPANDED

                // Get the screen height and set the max height to 75% of it
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.75).toInt()

                // Set max height to 75% of the screen height
                this.maxHeight = maxHeight
            }

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }



    private fun groupItemsByCategory(items: ArrayList<AmenitiesRef>): List<Map<String, Any>> {
        val categoryMap = mutableMapOf<String, MutableSet<String>>()

        // Group items by category, ensuring non-null category
        for (item in items) {
            val category = item.category ?: "Unknown"  // Default value for null categories
            val name = item.name ?: "Unknown"  // Default value for null categories
            categoryMap.getOrPut(category) { mutableSetOf() }.add(name)
        }

        // Convert the map into a nested list with sorted names
        return categoryMap.map { (category, names) ->
            mapOf("category" to category, "names" to names.sorted())
        }
    }

}