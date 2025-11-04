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

class RelatedQuestionAdapter(
    private val context: Context,
    private val questionAnswerList: ArrayList<Data>,
    private val onAnswerSelected: (String, String) -> Unit
) : RecyclerView.Adapter<RelatedQuestionAdapter.ViewHolder>() {

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
        binding.QuestionsTv.text = questionAnswer.question
        answersList = questionAnswer.answer as ArrayList<String>

        questionAnswer.id?.let { setDropDownAdapter(binding, answersList, it) }
    }

    private fun setDropDownAdapter(
        binding: BusinessRelatedQuestionItemBinding,
        answerList: ArrayList<String>,
        question: String
    ) {
        binding.answerTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.blured_background))
        binding.answerTv.dropDownVerticalOffset = binding.answerTv.height + 30


        val businessTypeAdapter = RelatedQuestionDropDownAdapter(context, answerList)
        binding.answerTv.setAdapter(businessTypeAdapter)
        binding.answerTv.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as String
            binding.answerTv.setTextColor(ContextCompat.getColor(context, R.color.text_color))
            onAnswerSelected(question, selectedItem)
        }
    }
}
