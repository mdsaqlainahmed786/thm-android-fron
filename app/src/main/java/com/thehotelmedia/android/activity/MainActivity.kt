package com.thehotelmedia.android.activity

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.modals.feeds.feed.MediaRef


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MultiTypeAdapter
    private var activePosition = 0
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)





        val staticDataList = mutableListOf(
            RecyclerItem.Review(reviewText = "This is a great product!"),
            RecyclerItem.Post(postTitle = "Breaking News", postContent = "Something exciting just happened!"),
            RecyclerItem.Post(postTitle = "Breaking News", postContent = "Something exciting just happened!"),
            RecyclerItem.Post(postTitle = "Breaking News", postContent = "Something exciting just happened!"),
            RecyclerItem.Event(eventName = "Tech Conference 2024", eventDate = "April 15, 2024"),
            RecyclerItem.Review(reviewText = "Could be better in some aspects."),
            RecyclerItem.Post(postTitle = "Tech Update", postContent = "New tech trends to watch out for."),
            RecyclerItem.Event(eventName = "Music Festival", eventDate = "June 20, 2024")
        )

        adapter = MultiTypeAdapter(staticDataList, activePosition, this)
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != activePosition) {
                    updateActivePosition(firstVisibleItem)
                }
            }
        })
    }

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition != activePosition) {
            val previousActivePosition = activePosition
            activePosition = newPosition
            adapter.setActivePosition(activePosition)
//            adapter.notifyDataSetChanged()
            adapter.notifyItemChanged(previousActivePosition)
            adapter.notifyItemChanged(activePosition)
        }
    }
}

sealed class RecyclerItem {
    data class Review(val reviewText: String, var isActive: Boolean = false) : RecyclerItem()
    data class Post(val postTitle: String, val postContent: String, var isActive: Boolean = false) : RecyclerItem()
    data class Event(val eventName: String, val eventDate: String, var isActive: Boolean = false) : RecyclerItem()
}

class MultiTypeAdapter(
    private val data: List<RecyclerItem>,
    private var activePosition: Int,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_REVIEW = 0
        const val TYPE_POST = 1
        const val TYPE_EVENT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is RecyclerItem.Review -> TYPE_REVIEW
            is RecyclerItem.Post -> TYPE_POST
            is RecyclerItem.Event -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_REVIEW -> ReviewViewHolder(inflater.inflate(R.layout.item_review, parent, false))
            TYPE_POST -> PostViewHolder(inflater.inflate(R.layout.item_post, parent, false))
            TYPE_EVENT -> EventViewHolder(inflater.inflate(R.layout.item_event, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isActive = position == activePosition
        when (holder) {
            is ReviewViewHolder -> holder.bind(data[position] as RecyclerItem.Review, isActive)
            is PostViewHolder -> holder.bind(data[position] as RecyclerItem.Post, isActive)
            is EventViewHolder -> holder.bind(data[position] as RecyclerItem.Event, isActive)
        }
    }

    override fun getItemCount(): Int = data.size

    fun setActivePosition(position: Int) {
        activePosition = position
    }

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.reviewTextView)
        fun bind(item: RecyclerItem.Review, isActive: Boolean) {
            textView.text = item.reviewText
            itemView.setBackgroundColor(if (isActive) 0xFFFF0000.toInt() else 0xFF808080.toInt())
        }
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postTitleTextView: TextView = itemView.findViewById(R.id.postTitleTextView)
        private val postContentTextView: TextView = itemView.findViewById(R.id.postContentTextView)
        private val viewPager: ViewPager2 = itemView.findViewById(R.id.viewPager2)

        fun bind(item: RecyclerItem.Post, isActive: Boolean) {
            postTitleTextView.text = item.postTitle
            postContentTextView.text = item.postContent

            val mediaList = arrayListOf<MediaRef>(
                MediaRef(
                    Id = "1",
                    mediaType = "video",
                    mimeType = "video/mp4",
                    sourceUrl = "https://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8"
                ),
                MediaRef(
                    Id = "2",
                    mediaType = "image",
                    mimeType = "image/png",
                    sourceUrl = "https://farm2.staticflickr.com/1533/26541536141_41abe98db3_z_d.jpg"
                ),
                MediaRef(
                    Id = "3",
                    mediaType = "video",
                    mimeType = "video/mp4",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
                ),
            )

            val mediaPagerAdapter = MediaPagerAdapter(
                itemView.context,
                mediaList,
                isActive,
                "postId",
                false,
                1,
                1,
                null, // individualViewModal
                { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
//                updateLikeBtn(updatedIsLikedByMe, binding.likeIv)
//                binding.likeTv.text = updatedLikeCount.toString()
//                binding.commentTv.text = updatedCommentCount.toString()
                // You can also update UI elements in the activity here
                }
            )
            viewPager.adapter = mediaPagerAdapter


            itemView.setBackgroundColor(if (isActive) 0xFFFF0000.toInt() else 0xFF808080.toInt())
        }
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.eventNameTextView)
        private val dateView: TextView = itemView.findViewById(R.id.eventDateTextView)
        fun bind(item: RecyclerItem.Event, isActive: Boolean) {
            nameView.text = item.eventName
            dateView.text = item.eventDate
            itemView.setBackgroundColor(if (isActive) 0xFFFF0000.toInt() else 0xFF808080.toInt())
        }
    }
}

