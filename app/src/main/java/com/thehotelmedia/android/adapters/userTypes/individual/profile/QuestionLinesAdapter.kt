package com.thehotelmedia.android.adapters.userTypes.individual.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.ProfileQuestionItemsBinding

class QuestionLinesAdapter(
    private val context: Context,
    private val bio: String) : RecyclerView.Adapter<QuestionLinesAdapter.ViewHolder>(){
    inner class ViewHolder(val binding: ProfileQuestionItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileQuestionItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.bioTv.text = bio
    }

}