package com.ant.placebook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ant.placebook.R
import com.ant.placebook.databinding.BookmarkItemBinding
import com.ant.placebook.ui.MapsActivity
import com.ant.placebook.viewmodel.MapsViewModel.BookmarkView

class BookmarkListAdapter(
    private var bookmarkData: List<BookmarkView>?,
    private val mapsActivity: MapsActivity
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

    // Hold the view widgets
    class ViewHolder(val binding: BookmarkItemBinding, private val mapsActivity: MapsActivity) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up a click listener on the ViewHolder
            binding.root.setOnClickListener {
                // Zoom the map to the bookmark
                val bookmarkView = itemView.tag as BookmarkView
                mapsActivity.moveToBookmark(bookmarkView)
            }
        }
    }

    // Assign bookmarks to a new BookmarkView List and call notifyDataSetChanged()
    @SuppressLint("NotifyDataSetChanged")
    fun setBookmarkData(bookmarks: List<BookmarkView>) {
        this.bookmarkData = bookmarks
        notifyDataSetChanged()
    }

    // Override onCreateViewHolder to create a ViewHolder and return it
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BookmarkItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding, mapsActivity)
    }

    // Override onBindViewHolder to populate a ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bookmarkData?.let { list ->
            // Set bookmarkView data to the current item
            val bookmarkViewData = list[position]
            holder.binding.root.tag = bookmarkViewData
            holder.binding.bookmarkData = bookmarkViewData
            holder.binding.bookmarkIcon.setImageResource(R.drawable.ic_other)
        }
    }

    // Return the number of items in the bookmarkData List
    override fun getItemCount() = bookmarkData?.size ?: 0
}