package com.thehotelmedia.android.bottomSheets

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.userTypes.forms.createPost.FeelingActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.AttachedMediaAdapter
import com.thehotelmedia.android.databinding.FragmentEditPostBottomSheetBinding
import com.thehotelmedia.android.modals.feeds.feed.MediaRef

class EditPostBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditPostBottomSheetBinding

    var initialContent: String? = null
    var initialFeeling: String? = null
    var initialMedia: List<MediaRef> = emptyList()

    private val mediaList = mutableListOf<String>()
    private lateinit var attachedMediaAdapter: AttachedMediaAdapter

    // Callback when Save is clicked - now returns content, feeling, and media
    var onSaveClicked: ((String, String, List<String>) -> Unit)? = null

    private val feelingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            val selectedFeeling = bundle?.getSerializable("selectedFeeling") as? String ?: ""
            updateFeeling(selectedFeeling)
        }
    }

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val mimeType = requireContext().contentResolver.getType(it)
            if (mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true) {
                mediaList.add(it.toString())
                updateMediaAdapter()
            }
        }
    }
    
    private val pickMultipleMediaLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        uris.forEach { uri ->
            val mimeType = requireContext().contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true) {
                mediaList.add(uri.toString())
            }
        }
        updateMediaAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialContent = arguments?.getString(ARG_CONTENT)
        initialFeeling = arguments?.getString(ARG_FEELING)
        initialMedia = arguments?.getParcelableArrayList<MediaRef>(ARG_MEDIA) ?: emptyList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { BottomSheetBehavior.from(it).isDraggable = false }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_post_bottom_sheet, container, false)
        initUi()
        return binding.root
    }

    private fun initUi() {
        // Set content
        binding.contentEt.setText(initialContent ?: "")

        // Set feeling
        updateFeeling(initialFeeling ?: "")

        // Set media - convert MediaRef URLs to local format for display
        // Note: AttachedMediaAdapter expects local file paths, but we have URLs
        // For now, we'll store URLs and adapter will try to load them
        mediaList.clear()
        initialMedia.forEach { mediaRef ->
            // Use sourceUrl or thumbnailUrl from MediaRef
            val mediaUrl = mediaRef.sourceUrl ?: mediaRef.thumbnailUrl
            mediaUrl?.let { 
                // Store URL - adapter should handle both URLs and local paths
                mediaList.add(it)
            }
        }
        updateMediaAdapter()

        // Setup click listeners
        binding.cancelBtn.setOnClickListener { dismiss() }

        binding.feelingLayout.setOnClickListener {
            val intent = Intent(requireContext(), FeelingActivity::class.java)
            intent.putExtra("selectedFeeling", binding.feelingTv.text.toString())
            feelingLauncher.launch(intent)
        }

        binding.feelingRemoveBtn.setOnClickListener {
            updateFeeling("")
        }

        binding.addMediaBtn.setOnClickListener {
            pickMultipleMediaLauncher.launch("*/*")
        }

        binding.removeAllMediaBtn.setOnClickListener {
            mediaList.clear()
            updateMediaAdapter()
        }

        binding.saveBtn.setOnClickListener {
            val content = binding.contentEt.text?.toString()?.trim().orEmpty()
            val feeling = if (binding.feelingLayout.visibility == View.VISIBLE && 
                binding.feelingTv.text.toString() != "Select feeling") {
                binding.feelingTv.text.toString()
            } else {
                ""
            }
            onSaveClicked?.invoke(content, feeling, mediaList)
            dismiss()
        }
    }

    private fun updateFeeling(feeling: String) {
        if (feeling.isNotEmpty()) {
            binding.feelingLayout.visibility = View.VISIBLE
            binding.feelingTv.text = feeling
            binding.feelingTv.setTextColor(requireContext().getColor(R.color.text_color))
            binding.feelingRemoveBtn.visibility = View.VISIBLE
        } else {
            binding.feelingLayout.visibility = View.GONE
            binding.feelingRemoveBtn.visibility = View.GONE
        }
    }

    private fun updateMediaAdapter() {
        if (mediaList.isEmpty()) {
            binding.mediaRv.visibility = View.GONE
            binding.removeAllMediaBtn.visibility = View.GONE
        } else {
            binding.mediaRv.visibility = View.VISIBLE
            binding.removeAllMediaBtn.visibility = View.VISIBLE
            
            attachedMediaAdapter = AttachedMediaAdapter(requireContext(), mediaList, { updatedList ->
                mediaList.clear()
                mediaList.addAll(updatedList)
                updateMediaAdapter()
            }, null)
            binding.mediaRv.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.mediaRv.adapter = attachedMediaAdapter
        }
    }

    companion object {
        private const val ARG_CONTENT = "content"
        private const val ARG_FEELING = "feeling"
        private const val ARG_MEDIA = "media"

        fun newInstance(content: String, feeling: String? = null, media: List<MediaRef> = emptyList()): EditPostBottomSheetFragment {
            val f = EditPostBottomSheetFragment()
            val b = Bundle()
            b.putString(ARG_CONTENT, content)
            b.putString(ARG_FEELING, feeling)
            b.putParcelableArrayList(ARG_MEDIA, ArrayList(media))
            f.arguments = b
            return f
        }
    }
}
