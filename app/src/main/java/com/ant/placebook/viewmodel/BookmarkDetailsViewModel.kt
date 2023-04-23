package com.ant.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.ant.placebook.model.Bookmark
import com.ant.placebook.repository.BookmarkRepo
import com.ant.placebook.utils.ImageUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val bookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    // Convert a Bookmark model to BookmarkDetailsView model
    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes
        )
    }

    // Transform a live Bookmark to a live BookmarkDetailsView
    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark)
        { repoBookmark -> bookmarkToBookmarkView(repoBookmark) }
    }

    // Update the bookmark view model class
    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }

        // Populate the bookmarkView's parameters
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
        }

        // Return the altered bookmark
        return bookmark
    }


    // Get a BookmarkDetailsView object
    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        // If bookmarkDetailsView is null, then create a bookmarkDetailsView
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }

        // Return the BookmarkDetailsView
        return bookmarkDetailsView
    }

    // Hold the info required by View
    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = ""
    ) {
        // Get the image
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }

        // Set the image
        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(
                    context,
                    image,
                    Bookmark.generateImageFilename(it)
                )
            }
        }
    }

    // Update a bookmark
    @OptIn(DelicateCoroutinesApi::class)
    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // Run the method in the background
        GlobalScope.launch {
            // Convert the BookmarkDetailsView to a Bookmark
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }
}