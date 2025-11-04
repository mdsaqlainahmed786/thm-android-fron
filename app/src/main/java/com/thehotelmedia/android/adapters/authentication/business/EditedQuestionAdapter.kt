package com.thehotelmedia.android.adapters.authentication.business

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.dropDown.RelatedQuestionDropDownAdapter
import com.thehotelmedia.android.databinding.BusinessRelatedQuestionItemBinding
import com.thehotelmedia.android.modals.authentication.business.questionAnswer.Data
import com.thehotelmedia.android.modals.profileData.profile.BusinessAnswerRef


class EditedQuestionAdapter(
    private val context: Context,
    private val questionAnswerList: ArrayList<Data>,
    private var answerAmenitiesRefList: ArrayList<BusinessAnswerRef>?, // Pass this as a parameter
    private val onAnswerSelected: (String, String) -> Unit
) : RecyclerView.Adapter<EditedQuestionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: BusinessRelatedQuestionItemBinding) : RecyclerView.ViewHolder(binding.root)

    var answersList: ArrayList<String> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BusinessRelatedQuestionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return questionAnswerList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val questionAnswer = questionAnswerList[position]

        // Set the question text
        binding.QuestionsTv.text = questionAnswer.question

        // Convert the answers to ArrayList
        answersList = questionAnswer.answer as ArrayList<String>

        // Check if there is a preselected answer for this questionID
        val preSelectedAnswer = answerAmenitiesRefList?.find { it.questionID == questionAnswer.id }?.answer

        // Call method to set the dropdown adapter with preselected answer
        questionAnswer.id?.let { setDropDownAdapter(binding, answersList, it, preSelectedAnswer) }
    }

    private fun setDropDownAdapter(
        binding: BusinessRelatedQuestionItemBinding,
        answerList: ArrayList<String>,
        question: String,
        preSelectedAnswer: String?
    ) {
        binding.answerTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.blured_background))
        binding.answerTv.dropDownVerticalOffset = binding.answerTv.height + 30

        // Set up the adapter for the dropdown
        val businessTypeAdapter = RelatedQuestionDropDownAdapter(context, answerList)
        binding.answerTv.setAdapter(businessTypeAdapter)

        // Preselect the answer if it's available
        preSelectedAnswer?.let {
            val index = answerList.indexOf(it)
            if (index != -1) {
                binding.answerTv.setText(answerList[index], false) // Set the preselected answer
                binding.answerTv.setTextColor(ContextCompat.getColor(context, R.color.text_color))
                // Trigger onAnswerSelected for the preselected answer
                onAnswerSelected(question, preSelectedAnswer)
            }
        }

        // Set the listener for item selection
        binding.answerTv.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as String
            binding.answerTv.setTextColor(ContextCompat.getColor(context, R.color.text_color))
            onAnswerSelected(question, selectedItem)
        }
    }

    // Optional: Update function to refresh the answer list
    fun updateAnswerAmenitiesRefList(newAnswerAmenitiesRefList: ArrayList<BusinessAnswerRef>?) {
        this.answerAmenitiesRefList = newAnswerAmenitiesRefList
        notifyDataSetChanged()
    }
}
