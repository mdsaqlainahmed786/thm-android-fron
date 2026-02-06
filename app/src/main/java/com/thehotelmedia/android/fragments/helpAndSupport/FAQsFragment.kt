package com.thehotelmedia.android.fragments.helpAndSupport

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.helpAndSupports.FAQRecyclerAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.search.OptionsAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentFAQsBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class FAQsFragment : Fragment() {

    private lateinit var binding: FragmentFAQsBinding

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private lateinit var faqRecyclerAdapter: FAQRecyclerAdapter
    private var  type = ""
    private var  query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_f_a_qs, container, false)

        initUi()

        return binding.root
    }

    private fun initUi() {
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do something after the text is changed, if necessary
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do something before the text is changed, if necessary
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Print the text in real-time as the user types
                query = s.toString().trim()
                getFaqData()
            }
        })

        binding.searchEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText is focused
                binding.searchLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            } else {
                // When EditText loses focus
                binding.searchLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)
            }
        }


        val tags = listOf("General", "Account", "Privacy", "Content")
        faqRecyclerAdapter = FAQRecyclerAdapter(requireContext())
        val optionAdapter = OptionsAdapter(requireContext(),tags,::onOptionSelected)
        binding.optionsRv.adapter = optionAdapter
        // Manually select the first item after the adapter is set
        onOptionSelected(tags[0]) // Call the selection function
        optionAdapter.setSelectedTag(tags[0]) // Update the adapter to mark the first item as selected

    }

    private fun onOptionSelected(tags: String?) {
        when (tags) {
            "General" -> {
                type = "general"
                getFaqData()
            }
            "Account" -> {
                type = "content"
                getFaqData()
            }
            "Privacy" -> {
                type = "account"
                getFaqData()
            }
            "Content" -> {
                type = "privacy"
                getFaqData()
            }
        }
    }

    private fun getFaqData() {
        binding.itemsRv.adapter = faqRecyclerAdapter.withLoadStateFooter(
            footer = LoaderAdapter { faqRecyclerAdapter.retry() }
        )
        individualViewModal.getFaq(query,type).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                faqRecyclerAdapter.hideAllDescriptions()
                faqRecyclerAdapter.submitData(it)
            }
        }
    }
    private fun isLoading() {
        faqRecyclerAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    faqRecyclerAdapter.itemCount == 0


            if (query.isEmpty()) {
                if (isLoading) {
                    progressBar.show()
                } else {
                    progressBar.hide()
                }
            }

//            if (isEmpty) {
//                binding.noDataFoundLayout.visibility = View.VISIBLE
//            } else {
//                binding.noDataFoundLayout.visibility = View.GONE
//            }

        }
    }

}