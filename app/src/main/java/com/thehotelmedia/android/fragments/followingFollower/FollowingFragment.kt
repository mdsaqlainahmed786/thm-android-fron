package com.thehotelmedia.android.fragments.followingFollower

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.search.ProfilesAdapter
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.FragmentFollowingBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class FollowingFragment : Fragment() {

    private lateinit var binding: FragmentFollowingBinding
    private var userId: String = ""
    private lateinit var progressBar : CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var profileAdapter: ProfilesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_following, container, false)

        initUi()

        return binding.root
    }

    private fun initUi() {

        println("askfhkasdhfkas   enter following $userId")
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        getFollowingData()

        individualViewModal.toast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }



    private fun getFollowingData() {
        profileAdapter = ProfilesAdapter(requireContext(), ::onMenuClicked, "FollowingScreen")
        binding.itemsRv.adapter = profileAdapter

        binding.itemsRv.adapter = profileAdapter
            .withLoadStateFooter(footer = LoaderAdapter())


        individualViewModal.getFollowingData(userId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                profileAdapter.submitData(it)
            }
        }
    }
    private fun onMenuClicked(position: Int,id: String,view: View) {
        showMenuDialog(view,id)
    }

    private fun isLoading() {
        profileAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && profileAdapter.itemCount == 0

            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Handle empty state (optional)
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
//                binding.hasDataLayout.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
//                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun showMenuDialog(view: View?, userId: String) {
        // Inflate the dropdown menu layout
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.single_post_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
//        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
//        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)



        reportBtn.setOnClickListener {
            reportPost(userId)
            popupWindow.dismiss()
        }


        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }


    private fun reportPost(userId: String) {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", userId)
                putString("TYPE", "user")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportUser(userId,selectedReason)
            }
        }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)

    }


}