package com.thehotelmedia.android.activity.userTypes.forms.createPost

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.feelingActivity.FeelingsAdapter
import com.thehotelmedia.android.databinding.ActivityFeelingBinding
import com.thehotelmedia.android.extensions.toggleEnable

class FeelingActivity : BaseActivity() {

    private lateinit var binding: ActivityFeelingBinding
    private var selectedFeeling: String = ""
    private lateinit var feelingsAdapter: FeelingsAdapter
    private var feelingsList: ArrayList<String> = arrayListOf()
    private var filteredList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeelingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        selectedFeeling = (intent.getSerializableExtra("selectedFeeling") as? String).toString()

        if (selectedFeeling.isNotEmpty()){
            binding.feelingLayout.visibility = View.VISIBLE
            binding.feelingTv.text = selectedFeeling
        } else {
            binding.feelingLayout.visibility = View.GONE
            binding.feelingTv.text = selectedFeeling
        }

        // Set up the feelings list
        setupFeelingsList()

        // Set up the adapter
        feelingsAdapter = FeelingsAdapter(this, filteredList) { feeling ->
            onFeelingClick(feeling)
        }
        binding.feelingsRv.adapter = feelingsAdapter

        // Search functionality
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list when text changes
                filterFeelings(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.doneBtn.toggleEnable(false)

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cancel.setOnClickListener {
            val resultIntent = Intent().apply {
                val bundle = Bundle().apply {
                    putSerializable("selectedFeeling", "")
                }
                putExtras(bundle)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun setupFeelingsList() {
        feelingsList = ArrayList(resources.getStringArray(R.array.feelings_list).toList())
        // Initially display all feelings
        filteredList.addAll(feelingsList)
    }

    private fun filterFeelings(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(feelingsList) // Show all feelings if query is empty
        } else {
            val filtered = feelingsList.filter { it.contains(query, ignoreCase = true) }
            filteredList.addAll(filtered)
        }
        feelingsAdapter.notifyDataSetChanged() // Notify adapter of data change
    }

    private fun onFeelingClick(feeling: String) {
        val resultIntent = Intent().apply {
            val bundle = Bundle().apply {
                putSerializable("selectedFeeling", feeling)
            }
            putExtras(bundle)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
