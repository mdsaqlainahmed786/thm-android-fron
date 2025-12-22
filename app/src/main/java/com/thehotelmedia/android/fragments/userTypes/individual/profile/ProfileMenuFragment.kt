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
            // Handle menu item click - get the media object (it's a single object, not an array)
            val mediaItem = menuItem.media
            if (mediaItem == null) return@ProfileMenuAdapter

            val mimeType = mediaItem.mimeType?.lowercase().orEmpty()
            val mediaType = mediaItem.mediaType?.lowercase().orEmpty()
            val sourceUrl = mediaItem.sourceUrl

            when {
                mimeType.contains("pdf") || mediaType.contains("pdf") -> {
                    // Open PDF using Intent
                    if (!sourceUrl.isNullOrEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(sourceUrl)
                            setDataAndType(Uri.parse(sourceUrl), "application/pdf")
                            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Handle case where no PDF viewer is available
                            android.widget.Toast.makeText(requireContext(), "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                mimeType.startsWith("image") || mediaType == "im" -> {
                    // Open image in viewer
                    if (!sourceUrl.isNullOrEmpty()) {
                        val intent = Intent(requireContext(), VideoImageViewer::class.java).apply {
                            putExtra("MEDIA_URL", sourceUrl)
                            putExtra("MEDIA_TYPE", "image")
                            putExtra("FROM", "MENU")
                        }
                        startActivity(intent)
                    }
                }
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

