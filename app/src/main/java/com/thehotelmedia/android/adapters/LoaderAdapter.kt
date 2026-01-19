package com.thehotelmedia.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.LoadingItemsBinding


class LoaderAdapter(
    private val onRetry: (() -> Unit)? = null
) : LoadStateAdapter<LoaderAdapter.LoaderViewHolder>() {

    inner class LoaderViewHolder(val binding: LoadingItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState) {

            val isLoading = loadState is LoadState.Loading
            val isError = loadState is LoadState.Error

            binding.progress.isVisible = isLoading

            binding.errorText.isVisible = isError
            binding.retryButton.isVisible = isError && onRetry != null

            if (isError) {
                binding.errorText.text = (loadState as LoadState.Error).error.localizedMessage
                    ?: "Something went wrong. Please try again."
            }

            binding.retryButton.setOnClickListener {
                onRetry?.invoke()
            }

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
