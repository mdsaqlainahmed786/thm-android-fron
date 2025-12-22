package com.thehotelmedia.android.fragments.userTypes.individual.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.MenuViewerActivity
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.adapters.userTypes.individual.profile.ProfileMenuAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentProfileMenuBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import androidx.fragment.app.Fragment

class ProfileMenuFragment : Fragment() {

    private lateinit var binding: FragmentProfileMenuBinding
    private lateinit var individualViewModal: IndividualViewModal
    private var userId: String = ""
    private var businessProfileId: String = ""
    private lateinit var menuAdapter: ProfileMenuAdapter
    private lateinit var progressBar: CustomProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: ""
            businessProfileId = it.getString("BUSINESS_PROFILE_ID") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_menu, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        if (userId.isNotEmpty() && businessProfileId.isNotEmpty()) {
            getMenuData()
        } else {
            binding.privateAccLayout.root.visibility = View.VISIBLE
            binding.hasDataLayout.visibility = View.GONE
            binding.noDataFoundLayout.visibility = View.GONE
        }
    }

    private fun getMenuData() {
        menuAdapter = ProfileMenuAdapter(requireContext()) { menuItem ->
            // Delegate to MenuViewerActivity, opening at the clicked index
            val position = menuAdapter.currentList.indexOf(menuItem)
            if (businessProfileId.isNotEmpty() && position >= 0) {
                val intent = Intent(requireContext(), MenuViewerActivity::class.java).apply {
                    putExtra("BUSINESS_PROFILE_ID", businessProfileId)
                    putExtra("INITIAL_INDEX", position)
                }
                startActivity(intent)
            }
        }

        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.menuRv.layoutManager = layoutManager
        binding.menuRv.adapter = menuAdapter

        // Fetch menu data
        individualViewModal.getMenuResult.observe(viewLifecycleOwner) { result ->
            if (result?.status == true) {
                val menuItems = result.data ?: emptyList()
                if (menuItems.isNotEmpty()) {
                    menuAdapter.submitList(menuItems)
                    binding.hasDataLayout.visibility = View.VISIBLE
                    binding.noDataFoundLayout.visibility = View.GONE
                } else {
                    binding.hasDataLayout.visibility = View.GONE
                    binding.noDataFoundLayout.visibility = View.VISIBLE
                }
            } else {
                binding.hasDataLayout.visibility = View.GONE
                binding.noDataFoundLayout.visibility = View.VISIBLE
            }
        }

        // Load menu
        if (businessProfileId.isNotEmpty()) {
            individualViewModal.getMenu(businessProfileId)
        }
    }
}

