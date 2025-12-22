package com.thehotelmedia.android.activity

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityMenuViewerBinding
import com.thehotelmedia.android.databinding.ItemMenuPageImageBinding
import com.thehotelmedia.android.databinding.ItemMenuPagePdfBinding
import com.thehotelmedia.android.modals.menu.MenuItem
import com.thehotelmedia.android.modals.menu.MenuMedia
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class MenuViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuViewerBinding
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private var businessProfileId: String = ""
    private var initialIndex: Int = 0
    private var menuItems: List<MenuItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        businessProfileId = intent.getStringExtra("BUSINESS_PROFILE_ID") ?: ""
        initialIndex = intent.getIntExtra("INITIAL_INDEX", 0)

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(
            this,
            ViewModelFactory(null, individualRepo, null)
        )[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        setupUi()
        observeMenu()

        if (businessProfileId.isNotEmpty()) {
            individualViewModal.getMenu(businessProfileId)
        } else {
            finish()
        }
    }

    private fun setupUi() {
        binding.backBtn.setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun observeMenu() {
        individualViewModal.getMenuResult.observe(this) { result ->
            if (result?.status == true) {
                val items = result.data ?: emptyList()
                if (items.isNotEmpty()) {
                    menuItems = items
                    setupViewPager()
                } else {
                    // No menu items, just close
                    finish()
                }
            } else {
                finish()
            }
        }

        individualViewModal.loading.observe(this) { loading ->
            if (loading == true) progressBar.show() else progressBar.hide()
        }
    }

    private fun setupViewPager() {
        val pages = menuItems.mapNotNull { it.media?.let { media -> MenuPage.fromMedia(media) } }
        if (pages.isEmpty()) {
            finish()
            return
        }

        val adapter = MenuPagerAdapter(pages)
        binding.menuViewPager.adapter = adapter

        val startIndex = initialIndex.coerceIn(0, pages.lastIndex)
        binding.menuViewPager.setCurrentItem(startIndex, false)
        updatePageIndicator(startIndex, pages.size)

        binding.menuViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position, pages.size)
            }
        })
    }

    private fun updatePageIndicator(position: Int, total: Int) {
        binding.pageIndicatorTv.text = "${position + 1} / $total"
    }

    private data class MenuPage(
        val type: PageType,
        val sourceUrl: String,
        val thumbnailUrl: String?,
        val mimeType: String?
    ) {
        enum class PageType { IMAGE, PDF }

        companion object {
            fun fromMedia(media: MenuMedia): MenuPage? {
                val mime = media.mimeType?.lowercase().orEmpty()
                val mediaType = media.mediaType?.lowercase().orEmpty()
                val source = media.sourceUrl ?: return null

                return when {
                    mime.contains("pdf") || mediaType.contains("pdf") -> MenuPage(
                        PageType.PDF,
                        source,
                        media.thumbnailUrl,
                        mime
                    )
                    mime.startsWith("image") || mediaType == "im" || mediaType.startsWith("image") -> MenuPage(
                        PageType.IMAGE,
                        source,
                        media.thumbnailUrl,
                        mime
                    )
                    else -> null
                }
            }
        }
    }

    private inner class MenuPagerAdapter(
        private val pages: List<MenuPage>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            return when (pages[position].type) {
                MenuPage.PageType.IMAGE -> 0
                MenuPage.PageType.PDF -> 1
            }
        }

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val binding = ItemMenuPageImageBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                ImageViewHolder(binding)
            } else {
                val binding = ItemMenuPagePdfBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                PdfViewHolder(binding)
            }
        }

        override fun getItemCount(): Int = pages.size

        override fun onBindViewHolder(
            holder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
            position: Int
        ) {
            val page = pages[position]
            when (holder) {
                is ImageViewHolder -> holder.bind(page)
                is PdfViewHolder -> holder.bind(page)
            }
        }

        inner class ImageViewHolder(
            private val binding: ItemMenuPageImageBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(page: MenuPage) {
                binding.menuImageView.visibility = View.VISIBLE
                Glide.with(this@MenuViewerActivity)
                    .load(page.sourceUrl)
                    .placeholder(R.drawable.ic_post_placeholder)
                    .error(R.drawable.ic_post_placeholder)
                    .into(binding.menuImageView)
            }
        }

        inner class PdfViewHolder(
            private val binding: ItemMenuPagePdfBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(page: MenuPage) {
                val webView: WebView = binding.pdfWebView
                webView.settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    allowFileAccess = true
                    allowContentAccess = true
                }
                
                webView.webViewClient = WebViewClient()
                
                // Load PDF directly - modern Android WebView supports PDF rendering
                // For better compatibility, try Google Docs viewer first, then fallback to direct URL
                val pdfUrl = if (page.sourceUrl.startsWith("http://") || page.sourceUrl.startsWith("https://")) {
                    // Try Google Docs viewer for better rendering
                    "https://docs.google.com/viewer?url=${android.net.Uri.encode(page.sourceUrl)}&embedded=true"
                } else {
                    page.sourceUrl
                }
                
                webView.loadUrl(pdfUrl)
            }
        }
    }
}


