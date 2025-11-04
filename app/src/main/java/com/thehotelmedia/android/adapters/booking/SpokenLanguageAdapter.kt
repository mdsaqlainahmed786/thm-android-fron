package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SpokenLanguageItemBinding
import com.thehotelmedia.android.modals.booking.roomDetails.LanguageSpoken

class SpokenLanguageAdapter(
    private val context: Context,
    private var languages: ArrayList<LanguageSpoken> // List of languages
) : RecyclerView.Adapter<SpokenLanguageAdapter.SpokenLanguageViewHolder>() {

    inner class SpokenLanguageViewHolder(val binding: SpokenLanguageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpokenLanguageViewHolder {
        val binding = SpokenLanguageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpokenLanguageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    override fun onBindViewHolder(holder: SpokenLanguageViewHolder, position: Int) {
        val binding = holder.binding
        val name = languages[position].name ?: ""
        val flag = languages[position].flag ?: ""
        println("Aadsjgkajkl   $flag")
        binding.countyNameTv.text = name
        Glide.with(context).load(flag).placeholder(R.drawable.india_flag).into(binding.countyFlagIv)

    }
}
