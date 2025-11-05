package com.thehotelmedia.android.activity.userTypes.forms.createPost

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.R
import com.thehotelmedia.android.modals.forms.taggedPeople.TaggedData
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.forms.tagPeople.SelectedTagPeopleAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.forms.tagPeople.TagPeopleListAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityTagPeopleBinding
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch
import java.io.Serializable

data class TagPeople(
    val id: String,
    val name: String,
    val profilePic: String,
    val accountType: String, // New property for account type
    val businessTypeName: String, // New property for business type name
    val businessTypeIcon: String // New property for business type icon
) : Serializable
class TagPeopleActivity : BaseActivity() {


    private lateinit var binding: ActivityTagPeopleBinding
    private lateinit var tagPeopleListAdapter: TagPeopleListAdapter
    private val tagPeopleList = ArrayList<TaggedData>()
    private var finalSelectedPeopleList = ArrayList<TagPeople>()

    private lateinit var individualViewModal: IndividualViewModal
    private var selectedTagPeopleList: ArrayList<TagPeople>? = null
    private lateinit var progressBar : CustomProgressBar
    private var isCollaboration: Boolean = false
//    private lateinit var profilePhotosAdapter : ProfilePhotosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        selectedTagPeopleList = intent.getSerializableExtra("selectedTagPeopleList") as? ArrayList<TagPeople>
        isCollaboration = intent.getBooleanExtra("isCollaboration", false)
        
        // Update title based on whether it's for collaboration or tagging
        if (isCollaboration) {
            binding.titleTv.text = getString(R.string.collaborate_cap)
        } else {
            binding.titleTv.text = getString(R.string.tag_people_cap)
        }


        println("asfjdksakfhaskdf    $selectedTagPeopleList")

        binding.doneBtn.toggleEnable(false)


        getTaggedList("")


        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Call getTaggedList when the text changes
                val searchText = s.toString()
                getTaggedList(searchText)
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.doneBtn.setOnClickListener {
            Log.d("TagPeopleActivity", "Done clicked. Returning ${finalSelectedPeopleList.size} selected people: ${finalSelectedPeopleList.map { it.name }}")
            println("asfjkskfa    $finalSelectedPeopleList")


            val resultIntent = Intent().apply {
                val bundle = Bundle().apply {
                    putSerializable("selectedPeopleList", finalSelectedPeopleList)
                }
                putExtras(bundle)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()



//            val resultIntent = Intent().apply {
//                val bundle = Bundle().apply {
//                    putSerializable("selectedPeopleList", finalSelectedPeopleList)
//                }
//                putExtras(bundle)
//            }
//            setResult(Activity.RESULT_OK, resultIntent)
//            finish()

//            val resultIntent = Intent().apply {
//                val bundle = Bundle().apply {
//                    putParcelableArrayList("selectedPeopleList", finalSelectedPeopleList)
//                }
//                putExtras(bundle)
//            }
//            setResult(Activity.RESULT_OK, resultIntent)
//            finish()
        }
    }

    private fun getTaggedList(search: String) {
        // Hide the progress bar when starting a new search
        progressBar.hide()

        // Pass isCollaboration flag to adapter to limit selection to 1 for collaboration
        tagPeopleListAdapter = TagPeopleListAdapter(this,::onPeopleSelected, isCollaboration)

        binding.tagPeopleRv.adapter = tagPeopleListAdapter
            .withLoadStateFooter(footer = LoaderAdapter())


        individualViewModal.getTagged(search).observe(this) {
            this.lifecycleScope.launch {
                isLoading()
                tagPeopleListAdapter.submitData(it)
            }
        }
        
        // Restore selected items - use finalSelectedPeopleList if available, otherwise use selectedTagPeopleList from intent
        val itemsToRestore = if (finalSelectedPeopleList.isNotEmpty()) {
            finalSelectedPeopleList
        } else if (selectedTagPeopleList != null && selectedTagPeopleList!!.isNotEmpty()) {
            selectedTagPeopleList!!
        } else {
            emptyList()
        }
        
        // For collaboration, only allow one selection - take the first item if multiple exist
        val itemsToRestoreFiltered = if (isCollaboration && itemsToRestore.size > 1) {
            arrayListOf(itemsToRestore.first())
        } else {
            itemsToRestore
        }
        
        if (itemsToRestoreFiltered.isNotEmpty()) {
            setRecentChatProfileAdapter(itemsToRestoreFiltered as ArrayList<TagPeople>)
            tagPeopleListAdapter.setSelectedItems(itemsToRestoreFiltered as ArrayList<TagPeople>)
        }

    }
//    private fun isLoading() {
//        tagPeopleListAdapter.addLoadStateListener {
//
//            val isLoading = it.refresh is LoadState.Loading
//
//
//            val isEmpty = it.refresh is LoadState.NotLoading &&
//                    tagPeopleListAdapter.itemCount == 0
//            if (isLoading) {
//                progressBar.show()
//            } else {
//                progressBar.hide()
//            }
//
////            if (isEmpty) {
////                binding.noDataFoundLayout.visibility = View.VISIBLE
////                binding.hasDataLayout.visibility = View.GONE
////            } else {
////                binding.noDataFoundLayout.visibility = View.GONE
////                binding.hasDataLayout.visibility = View.VISIBLE
////            }
//
//        }
//    }

    private fun isLoading() {
        tagPeopleListAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && tagPeopleListAdapter.itemCount == 0

            // Hide progress bar if there's input in the EditText
            val hasSearchText = binding.searchEt.text?.isNotEmpty()

            if (isLoading && !hasSearchText!!) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Handle empty state (optional)
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.tagPeopleRv.visibility = View.GONE
                binding.selectedPeopleRv.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.tagPeopleRv.visibility = View.VISIBLE
                binding.selectedPeopleRv.visibility = View.VISIBLE
            }
        }
    }


    private fun onPeopleSelected(tagPeople: java.util.ArrayList<TagPeople>) {
        toggleDoneButton(tagPeople)
        setRecentChatProfileAdapter(tagPeople)
    }

    private fun setRecentChatProfileAdapter(tagPeople: java.util.ArrayList<TagPeople>) {
        val recentTagAdapter = SelectedTagPeopleAdapter(this,tagPeople,::onPeopleUnselected,::onListUpdated)
        binding.selectedPeopleRv.adapter = recentTagAdapter
    }

    private fun onListUpdated(tagPeopleList: ArrayList<TagPeople>) {
        finalSelectedPeopleList = tagPeopleList
        toggleDoneButton(tagPeopleList)
    }

    private fun onPeopleUnselected(tagPeople: TagPeople) {
        tagPeopleListAdapter.unselectItem(tagPeople)
    }
    private fun toggleDoneButton(tagPeople: ArrayList<TagPeople>) {
        if (tagPeople.isEmpty()){
            binding.doneBtn.toggleEnable(false)
        }else{
            binding.doneBtn.toggleEnable(true)
        }
    }


}