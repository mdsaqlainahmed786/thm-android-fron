package com.thehotelmedia.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.LoadingItemsBinding


class LoaderAdapter : LoadStateAdapter<LoaderAdapter.LoaderViewHolder>() {

    inner class LoaderViewHolder(val binding: LoadingItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState) {

            binding.progress.isVisible = loadState is LoadState.Loading

        }

    }

    override fun onBindViewHolder(holder: LoaderViewHolder, loadState: LoadState) {

        holder.bind(loadState)

    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoaderViewHolder {
        val binding = LoadingItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoaderViewHolder(binding)
    }

}
